package aQute.remote.agent;

import java.io.PrintStream;

public class RedirectOutput extends PrintStream {

	private final AgentServer agent;
	private final PrintStream out;
	private boolean err;

	public RedirectOutput(AgentServer agent, PrintStream out, boolean err) {
		super(out);
		this.agent = agent;
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

		try {
			out.write(b, off, len);
			String s = new String(b, off, len); // default encoding!
			if (err)
				this.agent.getSupervisor().stderr(s);
			else
				this.agent.getSupervisor().stdout(s);
		} catch (Exception e) {
			e.printStackTrace();
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
