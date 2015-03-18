package aQute.remote.plugin;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.osgi.dto.DTO;

import aQute.remote.api.Agent;
import aQute.remote.api.Event;
import aQute.remote.api.Supervisor;
import aQute.remote.util.AgentSupervisor;

public class LauncherSupervisor extends AgentSupervisor<Supervisor, Agent>
		implements Supervisor {
	private Appendable stdout;
	private Appendable stderr;
	private Thread stdin;
	private boolean redirected=false;

	static class Info extends DTO {
		public String sha;
		public long lastModified;
	}

	@Override
	public void event(Event e) throws Exception {
		System.out.println(e);
		switch (e.type) {
		case exit:
			exit(e.code);
			break;
		default:
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
		if ( !redirected)
			getAgent().redirect(redirected=true);
		this.stdout = out;
	}

	public void setStderr(Appendable err) throws Exception {
		if ( !redirected)
			getAgent().redirect(redirected=true);
		this.stderr = err;
	}

	public void setStdin(final InputStream in) throws Exception {
		if ( !redirected)
			getAgent().redirect(redirected=true);
		
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
		getAgent().redirect(true);
	}

	public void connect(String host, int port) throws Exception {
		super.connect(Agent.class, this, host, port);
	}

	public int getExitCode() {
		return exitCode;
	}
}
