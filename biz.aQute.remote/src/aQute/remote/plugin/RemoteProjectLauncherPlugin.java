package aQute.remote.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Manifest;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.RunSession;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.strings.Strings;
import aQute.remote.embedded.activator.EmbeddedActivator;
import aQute.remote.util.JMXBundleDeployer;

/**
 * This is the plugin. It is found by bnd on the -runpath when it needs to
 * launch. The plugin is reponsible for launching the specification. In this
 * case, we will inspect -runremote. The -runremote can contain a number of
 * remote framework specifications.
 */
public class RemoteProjectLauncherPlugin extends ProjectLauncher {
	private static Converter converter = new Converter();

	static {
		converter.setFatalIsException(false);
	}

	private Parameters				runremote;
	private List<RunSessionImpl>	sessions	= new ArrayList<>();
	private boolean					prepared;

	/**
	 * The well defined launcher
	 *
	 * @param project the project or Run
	 */
	public RemoteProjectLauncherPlugin(Project project) throws Exception {
		super(project);
		runremote = new Parameters(getProject().getProperty(Constants.RUNREMOTE), getProject());
	}

	/**
	 * We do not have a main for a remote framework
	 */
	@Override
	public String getMainTypeName() {
		return "";
	}

	/**
	 * Called when a change in the IDE is detected. We will then upate from the
	 * project and then update the remote framework.
	 */
	@Override
	public void update() throws Exception {
		super.update();
		updateFromProject();

		Parameters runremote = new Parameters(getProject().getProperty(Constants.RUNREMOTE), getProject());

		for (RunSessionImpl session : sessions)
			try {
				Attrs attrs = runremote.get(session.getName());
				RunRemoteDTO dto = Converter.cnv(RunRemoteDTO.class, attrs);
				session.update(dto);
			} catch (Exception e) {
				getProject().exception(e, "Failed to update session %s", session.getName());
			}
	}

	/**
	 * We parse the -runremote and create sessions for each one of them
	 */

	@Override
	public void prepare() throws Exception {
		if (prepared)
			return;

		prepared = true;
		super.prepare();

		updateFromProject();

		Map<String, String> properties = new HashMap<>(getRunProperties());

		calculatedProperties(properties);

		Collection<String> embeddedActivators = getActivators();
		if (embeddedActivators != null && !embeddedActivators.isEmpty()) {
			properties.put("biz.aQute.remote.embedded", Strings.join(embeddedActivators));
		}

		for (Entry<String, Attrs> entry : runremote.entrySet()) {
			RunRemoteDTO dto = converter.convert(RunRemoteDTO.class, entry.getValue());
			dto.name = entry.getKey();

			Map<String, Object> sessionProperties = new HashMap<>(properties);
			sessionProperties.putAll(entry.getValue());
			sessionProperties.put("session.name", dto.name);

			if (dto.jmx != null) {
				tryJMXDeploy(dto.jmx, "biz.aQute.remote.agent");
			}

			RunSessionImpl session = new RunSessionImpl(this, dto, properties);
			sessions.add(session);
		}
	}

	/**
	 * provide backward compatibility with the older API when IDE did not have
	 * multiple sessions. This should be straightforward to do since this method
	 * should not return until the process has exited. So we should be able to
	 * just launch all the sessions in their own threads and then sync.
	 */
	@Override
	public int launch() throws Exception {
		prepare();

		final int[] results = new int[sessions.size()];
		final Thread[] sessionThreads = new Thread[sessions.size()];

		for (int i = 0; i < sessions.size(); i++) {
			final int j = i;
			final RunSessionImpl session = sessions.get(j);

			sessionThreads[j] = new Thread("session launch " + j) {
				@Override
				public void run() {
					try {
						results[j] = session.launch();
					} catch (Exception e) {
						//
					}
				}
			};

			sessionThreads[j].start();
		}

		for (Thread sessionThread : sessionThreads) {
			sessionThread.join();
		}

		for (int result : results) {
			if (result > 0) {
				return result;
			}
		}

		return 0;
	}

	/**
	 * Make sure all sessions are closed
	 */
	@Override
	public void close() {
		for (RunSessionImpl session : sessions)
			try {
				session.close();
			} catch (Exception e) {
				// ignore
			}
	}

	/**
	 * Kill!
	 */
	@Override
	public void cancel() throws Exception {
		for (RunSessionImpl session : sessions)
			try {
				session.cancel();
			} catch (Exception e) {
				// ignore
			}
	}

	/**
	 * Send any given text to the remote framework and treat it as input
	 */
	@Override
	public void write(String text) throws Exception {
		throw new UnsupportedOperationException("This launcher only understands run sessions");
	}

	/**
	 * Get the sessions
	 */
	@Override
	public List<? extends RunSession> getRunSessions() throws Exception {
		prepare();
		return sessions;
	}

	@SuppressWarnings("deprecation")
	private void tryJMXDeploy(String jmx, String bsn) {
		JMXBundleDeployer jmxBundleDeployer = null;
		int port = -1;

		try {
			port = Integer.parseInt(jmx);
		} catch (Exception e) {
			// not an integer
		}

		try {
			if (port > -1) {
				jmxBundleDeployer = new JMXBundleDeployer(port);
			} else {
				jmxBundleDeployer = new JMXBundleDeployer();
			}
		} catch (Exception e) {
			// ignore if we can't create bundle deployer (no remote osgi.core
			// jmx avail)
		}

		if (jmxBundleDeployer != null) {
			for (String path : this.getRunpath()) {
				File file = new File(path);
				try (Jar jar = new Jar(file)) {
					if (bsn.equals(jar.getBsn())) {
						long bundleId = jmxBundleDeployer.deploy(bsn, file);

						trace("agent installed with bundleId=%s", bundleId);
						break;
					}
				} catch (Exception e) {
					//
				}
			}
		}
	}

	/**
	 * Created a JAR that is a bundle and that contains its dependencies
	 */

	@Override
	public Jar executable() throws Exception {
		Collection<String> bsns = getProject().getBsns();
		if (bsns.size() != 1)
			throw new IllegalArgumentException("Can only handle a single bsn for a run configuration " + bsns);
		String bsn = bsns.iterator()
			.next();

		Jar jar = new Jar(bsn);
		String path = "aQute/remote/embedded/activator/EmbeddedActivator.class";
		Resource resource = Resource.fromURL(getClass().getClassLoader()
			.getResource(path));
		jar.putResource("aQute/remote/embedded/activator/EmbeddedActivator.class", resource);

		Collection<Container> rb = getProject().getRunbundles();
		rb = Container.flatten(rb);
		Attrs attrs = new Attrs();

		for (Container c : rb) {
			if (c.getError() != null) {
				getProject().error("invalid runbundle %s", c);
			} else {
				File f = c.getFile();
				String tbsn = c.getBundleSymbolicName();
				String version = c.getVersion();
				if (version == null || !Version.isVersion(version))
					getProject().warning("The version of embedded bundle %s does not have a proper version", c);

				jar.putResource("jar/" + c.getBundleSymbolicName() + ".jar", new FileResource(f));

				attrs.put(tbsn, version);
			}
		}

		Analyzer a = new Analyzer(getProject());
		a.setJar(jar);

		a.setBundleActivator(EmbeddedActivator.class.getName());
		a.setProperty("Bnd-Embedded", attrs.toString()
			.replace(';', ','));
		Manifest manifest = a.calcManifest();
		jar.setManifest(manifest);
		getProject().getInfo(a);
		return jar;
	}
}
