package aQute.remote.plugin;

import java.io.InputStream;
import java.io.InputStreamReader;

import aQute.bnd.util.dto.DTO;
import aQute.remote.api.Agent;
import aQute.remote.api.Event;
import aQute.remote.api.Supervisor;
import aQute.remote.util.AgentSupervisor;

/**
 * This is the supervisor on the bnd launcher side. It provides the SHA
 * repository for the agent and handles the redirection. It also handles the
 * events.
 */
public class LauncherSupervisor extends AgentSupervisor<Supervisor, Agent> implements Supervisor {
	private Appendable	stdout;
	private Appendable	stderr;
	private Thread		stdin;
	private int			shell	= -100;	// always invalid so we update it

	static class Info extends DTO {
		public String	sha;
		public long		lastModified;
	}

	@Override
	public void event(Event e) throws Exception {
		switch (e.type) {
			case exit :
				exit(e.code);
				break;
			default :
				break;
		}

	}

	@Override
	public boolean stdout(String out) throws Exception {
		if (stdout != null) {
			stdout.append(out);
			return true;
		}
		return false;
	}

	@Override
	public boolean stderr(String out) throws Exception {
		if (stderr != null) {
			stderr.append(out);
			return true;
		}
		return false;
	}

	public void setStdout(Appendable out) throws Exception {
		this.stdout = out;
	}

	public void setStderr(Appendable err) throws Exception {
		this.stderr = err;
	}

	public void setStdin(final InputStream in) throws Exception {
		final InputStreamReader isr = new InputStreamReader(in);
		this.stdin = new Thread("stdin") {
			@Override
			public void run() {
				StringBuilder sb = new StringBuilder();

				while (!isInterrupted())
					try {
						if (isr.ready()) {
							int read = isr.read();
							if (read >= 0) {
								sb.append((char) read);
							} else
								return;

						} else {
							if (sb.length() == 0)
								sleep(100);
							else {
								getAgent().stdin(sb.toString());
								sb.setLength(0);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
			}
		};
		this.stdin.start();
	}

	public void setStreams(Appendable out, Appendable err) throws Exception {
		setStdout(out);
		setStderr(err);
		getAgent().redirect(shell);
	}

	public void connect(String host, int port) throws Exception {
		super.connect(Agent.class, this, host, port);
	}

	/**
	 * The shell port to use.
	 * <ul>
	 * <li>&lt;0 – Attach to a local Gogo CommandSession
	 * <li>0 – Use the standard console
	 * <li>else – Open a stream to that port
	 * </ul>
	 *
	 * @param shellPort
	 */
	public void setShell(int shellPort) {
		this.shell = shellPort;
	}

	public int getExitCode() {
		return exitCode;
	}

	public void abort() throws Exception {
		if (isOpen()) {
			getAgent().abort();
		}
	}

	public void redirect(int shell) throws Exception {
		if (this.shell != shell && isOpen()) {
			getAgent().redirect(shell);
			this.shell = shell;
		}
	}
}
