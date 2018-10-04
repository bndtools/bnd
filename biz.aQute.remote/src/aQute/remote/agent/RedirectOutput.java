package aQute.remote.agent;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Handles the redirection of the output. Any text written to this PrintStream
 * is send to the supervisor. We are a bit careful here that we're breaking
 * recursive calls that can happen when there is shit happening deep down below.
 */
public class RedirectOutput extends PrintStream {
	static Timer						timer		= new Timer();
	private final List<AgentServer>		agents;
	private final PrintStream			out;
	private StringBuilder				sb			= new StringBuilder();
	private boolean						err;
	private static ThreadLocal<Boolean>	onStack		= new ThreadLocal<>();
	private TimerTask					active;
	private String						lastOutput	= "";

	/**
	 * If we do not have an original, we create a null stream because the
	 * PrintStream requires this.
	 */
	static class NullOutputStream extends OutputStream {
		@Override
		public void write(int arg0) throws IOException {}
	}

	public RedirectOutput(List<AgentServer> agents, PrintStream out, boolean err) {
		super(out == null ? out = newNullOutputStream() : out);
		this.agents = agents;
		this.out = out;
		this.err = err;
	}

	// TODO: java11 - use OutputStream.nullOutputStream()
	private static PrintStream newNullOutputStream() {
		return new PrintStream(new NullOutputStream());
	}

	@Override
	public void write(int b) {
		this.write(new byte[] {
			(byte) b
		}, 0, 1);
	}

	@Override
	public void write(byte b[]) {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte b[], int off, int len) {
		if ((off | len | (b.length - (len + off)) | (off + len)) < 0)
			throw new IndexOutOfBoundsException();

		out.write(b, off, len);
		if (onStack.get() == null) {
			onStack.set(true);
			try {
				sb.append(new String(b, off, len)); // default encoding!
				flushConditional();
			} catch (Exception e) {
				// e.printStackTrace();
			} finally {
				onStack.remove();
			}
		} else {
			out.println("[recursive output] " + new String(b, off, len));
		}
	}

	private void flushConditional() {
		synchronized (this) {
			if (active != null)
				return;

			active = new TimerTask() {

				@Override
				public void run() {
					synchronized (RedirectOutput.this) {
						active = null;
					}
					flush();
				}

			};
			timer.schedule(active, 300);
		}
	}

	@Override
	public void flush() {
		final String output;
		synchronized (this) {
			if (sb.length() == 0)
				return;

			output = sb.toString();
			sb = new StringBuilder();
		}

		setLastOutput(output);

		for (AgentServer agent : agents) {
			if (agent.quit)
				continue;

			try {
				if (err)
					agent.getSupervisor()
						.stderr(output);
				else
					agent.getSupervisor()
						.stdout(output);
			} catch (InterruptedException ie) {
				return;
			} catch (Exception ie) {
				try {
					agent.close();
				} catch (IOException e) {
					// e.printStackTrace();
				}
			}
		}

		super.flush();
	}

	@Override
	public void close() {
		super.close();
	}

	public PrintStream getOut() {
		return out;
	}

	public String getLastOutput() {
		return lastOutput;
	}

	private void setLastOutput(String out) {
		if (!"".equals(out) && out != null) {
			out = out.replaceAll("^>.*$", "");

			if (!"".equals(out) && !out.startsWith("true")) {
				lastOutput = out;
			}
		}
	}
}
