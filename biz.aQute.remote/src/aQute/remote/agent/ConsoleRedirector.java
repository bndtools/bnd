package aQute.remote.agent;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConsoleRedirector implements Redirector {
	private static RedirectOutput stdout;
	private static RedirectOutput stderr;
	private static RedirectInput stdin;
	private static CopyOnWriteArrayList<AgentServer> agents = new CopyOnWriteArrayList<AgentServer>();
	volatile boolean quit = false;
	
	public ConsoleRedirector(AgentServer agent) throws IOException {
		synchronized (agents) {
			if (!agents.contains(this)) {
				agents.add(agent);
				if (agents.size() == 1) {
					System.setOut(stdout = new RedirectOutput(agents,
							System.out, false));
					System.setErr(stderr = new RedirectOutput(agents,
							System.err, true));
					System.setIn(stdin = new RedirectInput(System.in));
				}
			}
		}
	}

	@Override
	public void close() throws IOException {
		quit = true;
		synchronized (agents) {
			if (agents.remove(this)) {
				if (agents.size() == 0) {
					System.setOut(stdout.getOut());
					System.setErr(stderr.getOut());
					System.setIn(stdin.getOrg());
				}
			}
		}
	}

	@Override
	public int getPort() {
		return Redirector.CONSOLE;
	}

	@Override
	public void stdin(String s) throws IOException {
		stdin.add(s);
	}

	@Override
	public PrintStream getOut() {
		return stdout;
	}

}
