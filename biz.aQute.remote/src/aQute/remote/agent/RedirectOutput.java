package aQute.remote.agent;

import java.io.PrintStream;
import java.util.List;

public class RedirectOutput extends PrintStream {

	private final List<AgentServer> agents;
	private final PrintStream out;
	private boolean err;
	private static ThreadLocal<Boolean> onStack = new ThreadLocal<Boolean>();

	public RedirectOutput(List<AgentServer> agents, PrintStream out, boolean err) {
		super(out);
		this.agents = agents;
		this.out = out;
		this.err = err;
	}

	public void write(int b) {
		this.write(new byte[] { (byte) b }, 0, 1);
	}

	public void write(byte b[]) {
		write(b, 0, b.length);
	}

	public void write(byte b[], int off, int len) {
		if ((off | len | (b.length - (len + off)) | (off + len)) < 0)
			throw new IndexOutOfBoundsException();

		out.write(b, off, len);
		if (onStack.get()==null) {
			onStack.set(true);
			try {
				String s = new String(b, off, len); // default encoding!
				for (AgentServer agent : agents) {
					if (err)
						agent.getSupervisor().stderr(s);
					else
						agent.getSupervisor().stdout(s);
				}
			} catch (Exception e) {
				//e.printStackTrace();
			} finally {
				onStack.remove();
			}
		} else {
			out.println("oops");
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
