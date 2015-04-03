package aQute.remote.agent;

import java.io.*;
import java.util.*;

/**
 * Handles the redirection of the output. Any text written to this PrintStream
 * is send to the supervisor. We are a bit careful here that we're breaking
 * recursive calls that can happen when there is shit happening deep down below.
 */
public class RedirectOutput extends PrintStream {

	private final List<AgentServer>		agents;
	private final PrintStream			out;
	private boolean						err;
	private static ThreadLocal<Boolean>	onStack	= new ThreadLocal<Boolean>();

	/**
	 * If we do not have an original, we create a null stream because the
	 * PrintStream requires this.
	 */
	static class NullOutputStream extends OutputStream {
		@Override
		public void write(int arg0) throws IOException {}
	}

	public RedirectOutput(List<AgentServer> agents, PrintStream out, boolean err) {
		super(out == null ? out = nullOutputStream() : out);
		this.agents = agents;
		this.out = out;
		this.err = err;
	}

	private static PrintStream nullOutputStream() {
		return new PrintStream(new NullOutputStream());
	}

	public void write(int b) {
		this.write(new byte[] {
			(byte) b
		}, 0, 1);
	}

	public void write(byte b[]) {
		write(b, 0, b.length);
	}

	public void write(byte b[], int off, int len) {
		if ((off | len | (b.length - (len + off)) | (off + len)) < 0)
			throw new IndexOutOfBoundsException();

		out.write(b, off, len);
		if (onStack.get() == null) {
			onStack.set(true);
			try {
				String s = new String(b, off, len); // default encoding!
				for (AgentServer agent : agents) {
					if (agent.quit)
						continue;

					try {
						if (err)
							agent.getSupervisor().stderr(s);
						else
							agent.getSupervisor().stdout(s);
					}
					catch (InterruptedException ie) {
						return;
					}
					catch (Exception ie) {
						agent.close();
					}
				}
			}
			catch (Exception e) {
				// e.printStackTrace();
			}
			finally {
				onStack.remove();
			}
		} else {
			out.println("[recursive output] " + new String(b, off, len));
		}
	}

	public void flush() {
		super.flush();
	}

	public void close() {
		super.close();
	}

	public PrintStream getOut() {
		return out;
	}

}
