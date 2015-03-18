package aQute.remote.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import aQute.bnd.build.RunSession;
import aQute.bnd.osgi.Constants;
import aQute.remote.api.Agent;

public class RunSessionImpl implements RunSession {
	private LauncherSupervisor supervisor;
	private RemoteProjectLauncherPlugin launcher;
	private RunRemoteDTO dto;
	private Map<String, Object> properties;
	public CountDownLatch started = new CountDownLatch(1);
	private Appendable stderr;
	private Appendable stdout;
	public static int jdb = 16043;
	public RunSessionImpl(RemoteProjectLauncherPlugin launcher,
			RunRemoteDTO dto, Map<String, Object> properties) throws Exception {
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
		if ( supervisor!=null)
			supervisor.setStderr(app);
	}

	@Override
	public void stdout(Appendable app) throws Exception {
		stdout = app;
		if ( supervisor != null)
			supervisor.setStdout(app);
	}

	@Override
	public void stdin(String input) throws Exception {
		if ( supervisor != null)
			supervisor.getAgent().stdin(input);
	}

	@Override
	public int launch() throws Exception {
		try {
			supervisor = new LauncherSupervisor();
			supervisor.connect(dto.host, dto.agent);

			Agent agent = supervisor.getAgent();

			if (agent.isEnvoy()) {
				int secondaryPort = installFramework(agent, dto, properties);
				supervisor.close();
				System.out.println("Installed framework, changing port "
						+ secondaryPort);
				supervisor = new LauncherSupervisor();
				supervisor.connect(dto.host, secondaryPort);
			}

			if ( stdout != null)
				supervisor.setStdout(stdout);

			if ( stderr != null)
				supervisor.setStderr(stderr);

			started.countDown();

			update();
			int exitCode = supervisor.join();
			System.out.println("Exiting " + dto.name + " " + exitCode);
			return exitCode;
		} catch (Exception e) {
			started.countDown();
			throw e;
		}
	}

	@Override
	public void cancel() throws IOException {
		supervisor.getAgent().abort();
	}

	@Override
	public Map<String, Object> getProperties() {
		return properties;
	}

	private int installFramework(Agent agent, RunRemoteDTO dto,
			Map<String, Object> properties) throws Exception {
		List<String> onpath = new ArrayList<String>(launcher.getRunpath());

		Map<String, String> runpath = getBundles(onpath, Constants.RUNPATH);

		return agent.createFramework(dto.name, runpath.values(), properties);
	}

	void update(Map<String, String> newer) throws Exception {
		supervisor.getAgent().update(newer);
	}

	int join() throws InterruptedException {
		return supervisor.join();
	}

	public void close() throws IOException {
		supervisor.close();
	}

	Map<String, String> getBundles(Collection<String> collection, String header)
			throws Exception {
		Map<String, String> newer = new HashMap<String, String>();

		for (String c : collection) {
			File f = new File(c);
			String sha = supervisor.addFile(f);
			newer.put(c, sha);
		}
		return newer;
	}

	void update() throws Exception {
		Map<String, String> newer = getBundles(launcher.getRunBundles(),
				Constants.RUNBUNDLES);

		supervisor.getAgent().update(newer);
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

}
