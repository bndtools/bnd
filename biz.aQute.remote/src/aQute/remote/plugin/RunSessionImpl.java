package aQute.remote.plugin;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import aQute.bnd.build.RunSession;
import aQute.bnd.osgi.Constants;
import aQute.remote.api.Agent;

/**
 * A session is a connection to a remote framework. It is possible to have many
 * sessions at the same time. This object maintains the state of the connection
 * and provides means to cancel the connection.
 */
public class RunSessionImpl implements RunSession {
	private LauncherSupervisor			supervisor;
	private RemoteProjectLauncherPlugin	launcher;
	private RunRemoteDTO				dto;
	private Map<String, String>			properties;
	public CountDownLatch				started	= new CountDownLatch(1);
	private Appendable					stderr;
	private Appendable					stdout;
	private int							shell	= -4711;
	public static int					jdb		= 16043;

	public RunSessionImpl(RemoteProjectLauncherPlugin launcher, RunRemoteDTO dto, Map<String, String> properties)
		throws Exception {
		this.launcher = launcher;
		this.properties = properties;
		this.dto = dto;

		if (dto.agent <= 0)
			dto.agent = Agent.DEFAULT_PORT;

		if (dto.host == null)
			dto.host = "localhost";

		if (dto.jdb <= 0)
			dto.jdb = jdb++;

		if (dto.timeout <= 0)
			dto.timeout = 5000;
	}

	@Override
	public String getName() {
		return dto.name;
	}

	@Override
	public String getLabel() {
		return dto.name + " – " + dto.host + ":" + dto.agent;
	}

	@Override
	public int getJdb() {
		return dto.jdb;
	}

	@Override
	public void stderr(Appendable app) throws Exception {
		stderr = app;
		if (supervisor != null)
			supervisor.setStderr(app);
	}

	@Override
	public void stdout(Appendable app) throws Exception {
		stdout = app;
		if (supervisor != null)
			supervisor.setStdout(app);
	}

	@Override
	public void stdin(String input) throws Exception {
		if (supervisor != null)
			supervisor.getAgent()
				.stdin(input);
	}

	@Override
	public int launch() throws Exception {
		try {
			supervisor = new LauncherSupervisor();
			supervisor.connect(dto.host, dto.agent);

			Agent agent = supervisor.getAgent();

			if (agent.isEnvoy())
				installFramework(agent, dto, properties);

			if (stdout != null)
				supervisor.setStdout(stdout);

			if (stderr != null)
				supervisor.setStderr(stderr);

			started.countDown();

			update(dto);
			int exitCode = supervisor.join();
			return exitCode;
		} catch (Exception e) {
			started.countDown();
			throw e;
		}
	}

	@Override
	public void cancel() throws Exception {
		supervisor.abort();
	}

	@Override
	public Map<String, ?> getProperties() {
		return properties;
	}

	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	private boolean installFramework(Agent agent, RunRemoteDTO dto, Map<String, String> properties2) throws Exception {
		List<String> onpath = new ArrayList<>(launcher.getRunpath());

		Map<String, String> runpath = getBundles(onpath, Constants.RUNPATH);

		return agent.createFramework(dto.name, runpath.values(), (Map) properties2);
	}

	void update(Map<String, String> newer) throws Exception {
		supervisor.getAgent()
			.update(newer);
	}

	int join() throws InterruptedException {
		return supervisor.join();
	}

	public void close() throws IOException {
		supervisor.close();
	}

	Map<String, String> getBundles(Collection<String> collection, String header) throws Exception {
		Map<String, String> newer = new LinkedHashMap<>();

		for (String c : collection) {
			File f = new File(c);
			String sha = supervisor.addFile(f);
			newer.put(c, sha);
		}
		return newer;
	}

	void update(RunRemoteDTO dto) throws Exception {
		Map<String, String> newer = getBundles(launcher.getRunBundles(), Constants.RUNBUNDLES);
		if (shell != dto.shell) {
			supervisor.getAgent()
				.redirect(dto.shell);
			shell = dto.shell;
		}
		supervisor.getAgent()
			.update(newer);
	}

	@Override
	public int getExitCode() {
		return supervisor.getExitCode();
	}

	@Override
	public void waitTillStarted(long ms) throws InterruptedException {
		started.await(ms, TimeUnit.MILLISECONDS);
	}

	@Override
	public String getHost() {
		return dto.host;
	}

	@Override
	public long getTimeout() {
		return dto.timeout;
	}

	@Override
	public int getAgent() {
		return dto.agent;
	}

	@Override
	public boolean validate(Callable<Boolean> isCancelled) throws Exception {
		boolean result = true;

		if (getAgent() <= 0 || getAgent() > 65535) {
			launcher.error("Agent port %s not in a valid IP range (0-65535)", getAgent());
			result = false;
		}

		InetAddress in;
		try {
			in = InetAddress.getByName(getHost());
		} catch (Exception e) {
			launcher.exception(e, "Cannot find host %s", getHost());
			return false; // no use to continue from here
		}

		if (isCancelled.call())
			return false;

		if (dto.reachable && !in.isReachable(5000)) {
			launcher.error("Host not reachable %s", getHost());
			return false;
		}

		while (!isCancelled.call())
			try {
				Socket s = new Socket(getHost(), getAgent());
				s.close();
				break;
			} catch (Exception e) {
				// Ignore
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {}
			}
		return result;
	}

}
