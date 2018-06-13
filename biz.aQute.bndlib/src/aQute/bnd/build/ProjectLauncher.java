package aQute.bnd.build;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Strategy;
import aQute.lib.io.IO;
import aQute.libg.command.Command;
import aQute.libg.generics.Create;

/**
 * A Project Launcher is a base class to be extended by launchers. Launchers are
 * JARs that launch a framework and install a number of bundles and then run the
 * framework. A launcher jar must specify a Launcher-Class manifest header. This
 * class is instantiated and cast to a LauncherPlugin. This plug in is then
 * asked to provide a ProjectLauncher. This project launcher is then used by the
 * project to run the code. Launchers must extend this class.
 */
public abstract class ProjectLauncher extends Processor {
	private final static Logger			logger				= LoggerFactory.getLogger(ProjectLauncher.class);
	private final Project				project;
	private long						timeout				= 0;
	private final List<String>			classpath			= new ArrayList<>();
	private List<String>				runbundles			= Create.list();
	private final List<String>			runvm				= new ArrayList<>();
	private final List<String>			runprogramargs		= new ArrayList<>();
	private Map<String, String>			runproperties;
	private Command						java;
	private Parameters					runsystempackages;
	private Parameters					runsystemcapabilities;
	private final List<String>			activators			= Create.list();
	private File						storageDir;

	private boolean						trace;
	private boolean						keep;
	private int							framework;
	private File						cwd;
	private Collection<String>			agents				= new ArrayList<>();
	private Set<NotificationListener>	listeners			= Collections.newSetFromMap(new IdentityHashMap<>());

	protected Appendable				out					= System.out;
	protected Appendable				err					= System.err;
	protected InputStream				in					= System.in;

	public final static int				SERVICES			= 10111;
	public final static int				NONE				= 20123;

	// MUST BE ALIGNED WITH LAUNCHER
	public final static int				OK					= 0;
	public final static int				WARNING				= -1;
	public final static int				ERROR				= -2;
	public final static int				TIMEDOUT			= -3;
	public final static int				UPDATE_NEEDED		= -4;
	public final static int				CANCELED			= -5;
	public final static int				DUPLICATE_BUNDLE	= -6;
	public final static int				RESOLVE_ERROR		= -7;
	public final static int				ACTIVATOR_ERROR		= -8;
	public final static int				CUSTOM_LAUNCHER		= -128;

	public final static String			EMBEDDED_ACTIVATOR	= "Embedded-Activator";

	public ProjectLauncher(Project project) throws Exception {
		this.project = project;

		updateFromProject();
	}

	/**
	 * Collect all the aspect from the project and set the local fields from
	 * them. Should be called
	 * 
	 * @throws Exception
	 */
	protected void updateFromProject() throws Exception {
		setCwd(project.getBase());

		// pkr: could not use this because this is killing the runtests.
		// project.refresh();
		runbundles.clear();
		Collection<Container> run = project.getRunbundles();

		for (Container container : run) {
			File file = container.getFile();
			if (file != null && (file.isFile() || file.isDirectory())) {
				runbundles.add(IO.absolutePath(file));
			} else {
				project.error("Bundle file \"%s\" does not exist, given error is %s", file, container.getError());
			}
		}

		if (project.getRunBuilds()) {
			File[] builds = project.getBuildFiles(true);
			if (builds != null)
				for (File file : builds)
					runbundles.add(IO.absolutePath(file));
		}

		Collection<Container> runpath = project.getRunpath();
		runsystempackages = new Parameters(project.mergeProperties(Constants.RUNSYSTEMPACKAGES), project);
		runsystemcapabilities = new Parameters(project.mergeProperties(Constants.RUNSYSTEMCAPABILITIES), project);
		framework = getRunframework(project.getProperty(Constants.RUNFRAMEWORK));

		timeout = Processor.getDuration(project.getProperty(Constants.RUNTIMEOUT), 0);
		trace = Processor.isTrue(project.getProperty(Constants.RUNTRACE));

		runpath.addAll(project.getRunFw());

		for (Container c : runpath) {
			addClasspath(c);
		}

		runvm.addAll(project.getRunVM());
		runprogramargs.addAll(project.getRunProgramArgs());
		runproperties = project.getRunProperties();

		storageDir = project.getRunStorage();

		setKeep(project.getRunKeep());
	}

	private int getRunframework(String property) {
		if (Constants.RUNFRAMEWORK_NONE.equalsIgnoreCase(property))
			return NONE;
		else if (Constants.RUNFRAMEWORK_SERVICES.equalsIgnoreCase(property))
			return SERVICES;

		return SERVICES;
	}

	public void addClasspath(Container container) throws Exception {
		if (container.getError() != null) {
			project.error("Cannot launch because %s has reported %s", container.getProject(), container.getError());
		} else {
			Collection<Container> members = container.getMembers();
			for (Container m : members) {
				String path = IO.absolutePath(m.getFile());
				if (!classpath.contains(path)) {

					Manifest manifest = m.getManifest();

					if (manifest != null) {

						// We are looking for any agents, used if
						// -javaagent=true is set
						String agentClassName = manifest.getMainAttributes()
							.getValue("Premain-Class");
						if (agentClassName != null) {
							String agent = path;
							if (container.getAttributes()
								.get("agent") != null) {
								agent += "=" + container.getAttributes()
									.get("agent");
							}
							agents.add(agent);
						}

						Parameters exports = project.parseHeader(manifest.getMainAttributes()
							.getValue(Constants.EXPORT_PACKAGE));
						for (Entry<String, Attrs> e : exports.entrySet()) {
							if (!runsystempackages.containsKey(e.getKey()))
								runsystempackages.put(e.getKey(), e.getValue());
						}

						// Allow activators on the runpath. They are called
						// after
						// the framework is completely initialized wit the
						// system
						// context.
						String activator = manifest.getMainAttributes()
							.getValue(EMBEDDED_ACTIVATOR);
						if (activator != null)
							activators.add(activator);
					}
					classpath.add(path);
				}
			}
		}
	}

	protected void addClasspath(Collection<Container> path) throws Exception {
		for (Container c : Container.flatten(path)) {
			addClasspath(c);
		}
	}

	public void addRunBundle(String f) {
		runbundles.add(f);
	}

	public Collection<String> getRunBundles() {
		return runbundles;
	}

	public void addRunVM(String arg) {
		runvm.add(arg);
	}

	public void addRunProgramArgs(String arg) {
		runprogramargs.add(arg);
	}

	public List<String> getRunpath() {
		return classpath;
	}

	public Collection<String> getClasspath() {
		return classpath;
	}

	public Collection<String> getRunVM() {
		return runvm;
	}

	@Deprecated
	public Collection<String> getArguments() {
		return getRunProgramArgs();
	}

	public Collection<String> getRunProgramArgs() {
		return runprogramargs;
	}

	public Map<String, String> getRunProperties() {
		return runproperties;
	}

	public File getStorageDir() {
		return storageDir;
	}

	public abstract String getMainTypeName();

	public abstract void update() throws Exception;

	public int launch() throws Exception {
		prepare();
		java = new Command();

		//
		// Handle the environment
		//

		Map<String, String> env = getRunEnv();
		for (Map.Entry<String, String> e : env.entrySet()) {
			java.var(e.getKey(), e.getValue());
		}

		java.add(project.getProperty("java", getJavaExecutable()));
		String javaagent = project.getProperty(Constants.JAVAAGENT);
		if (Processor.isTrue(javaagent)) {
			for (String agent : agents) {
				java.add("-javaagent:" + agent);
			}
		}

		String jdb = getRunJdb();
		if (jdb != null) {
			int port = 1044;
			try {
				port = Integer.parseInt(jdb);
			} catch (Exception e) {
				// ok, value can also be ok, or on, or true
			}
			String suspend = port > 0 ? "y" : "n";

			java.add("-Xrunjdwp:server=y,transport=dt_socket,address=" + Math.abs(port) + ",suspend=" + suspend);
		}

		java.addAll(split(System.getenv("JAVA_OPTS"), "\\s+"));

		java.add("-cp");
		java.add(Processor.join(getClasspath(), File.pathSeparator));
		java.addAll(getRunVM());
		java.add(getMainTypeName());
		java.addAll(getRunProgramArgs());
		if (timeout != 0)
			java.setTimeout(timeout + 1000, TimeUnit.MILLISECONDS);

		File cwd = getCwd();
		if (cwd != null)
			java.setCwd(cwd);

		logger.debug("cmd line {}", java);
		try {
			int result = java.execute(in, out, err);
			if (result == Integer.MIN_VALUE)
				return TIMEDOUT;
			reportResult(result);
			return result;
		} finally {
			cleanup();
			listeners.clear();
		}
	}

	private String getJavaExecutable() {
		String javaHome = System.getProperty("java.home");
		if (javaHome == null) {
			return "java";
		}
		File java = new File(javaHome, "bin/java");
		return IO.absolutePath(java);
	}

	/**
	 * launch a framework internally. I.e. do not start a separate process.
	 */
	static Pattern IGNORE = Pattern.compile("org(/|\\.)osgi(/|\\.).resource.*");

	public int start(ClassLoader parent) throws Exception {

		prepare();

		//
		// Intermediate class loader to not load osgi framework packages
		// from bnd's loader. Unfortunately, bnd uses some osgi classes
		// itself that would unnecessarily constrain the framework.
		//

		ClassLoader fcl = new ClassLoader(parent) {
			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				if (IGNORE.matcher(name)
					.matches())
					throw new ClassNotFoundException();

				return super.loadClass(name, resolve);
			}
		};

		//
		// Load the class that would have gone to the class path
		// i.e. the framework etc.
		//

		List<URL> cp = new ArrayList<>();
		for (String path : getClasspath()) {
			cp.add(new File(path).toURI()
				.toURL());
		}
		@SuppressWarnings("resource")
		URLClassLoader cl = new URLClassLoader(cp.toArray(new URL[0]), fcl);

		String[] args = getRunProgramArgs().toArray(new String[0]);

		Class<?> main = cl.loadClass(getMainTypeName());
		return invoke(main, args);
	}

	protected int invoke(Class<?> main, String args[]) throws Exception {
		throw new UnsupportedOperationException();
	}

	/**
	 * Is called after the process exists. Can you be used to cleanup the
	 * properties file.
	 */

	public void cleanup() {
		// do nothing by default
	}

	protected void reportResult(int result) {
		switch (result) {
			case OK :
				logger.debug("Command terminated normal {}", java);
				break;
			case TIMEDOUT :
				project.error("Launch timedout: %s", java);
				break;

			case ERROR :
				project.error("Launch errored: %s", java);
				break;

			case WARNING :
				project.warning("Launch had a warning %s", java);
				break;
			default :
				project.error("Exit code remote process %d: %s", result, java);
				break;
		}
	}

	public void setTimeout(long timeout, TimeUnit unit) {
		this.timeout = unit.convert(timeout, TimeUnit.MILLISECONDS);
	}

	public long getTimeout() {
		return this.timeout;
	}

	public void cancel() throws Exception {
		java.cancel();
	}

	public Map<String, ? extends Map<String, String>> getSystemPackages() {
		return runsystempackages.asMapMap();
	}

	public String getSystemCapabilities() {
		return runsystemcapabilities.isEmpty() ? null : runsystemcapabilities.toString();
	}

	public Parameters getSystemCapabilitiesParameters() {
		return runsystemcapabilities;
	}

	public void setKeep(boolean keep) {
		this.keep = keep;
	}

	public boolean isKeep() {
		return keep;
	}

	@Override
	public void setTrace(boolean level) {
		this.trace = level;
	}

	public boolean getTrace() {
		return this.trace;
	}

	/**
	 * Should be called when all the changes to the launchers are set. Will
	 * calculate whatever is necessary for the launcher.
	 * 
	 * @throws Exception
	 */
	public abstract void prepare() throws Exception;

	public Project getProject() {
		return project;
	}

	public boolean addActivator(String e) {
		return activators.add(e);
	}

	public Collection<String> getActivators() {
		return Collections.unmodifiableCollection(activators);
	}

	/**
	 * Either NONE or SERVICES to indicate how the remote end launches. NONE
	 * means it should not use the classpath to run a framework. This likely
	 * requires some dummy framework support. SERVICES means it should load the
	 * framework from the claspath.
	 */
	public int getRunFramework() {
		return framework;
	}

	public void setRunFramework(int n) {
		assert n == NONE || n == SERVICES;
		this.framework = n;
	}

	/**
	 * Add the specification for a set of bundles the runpath if it does not
	 * already is included. This can be used by subclasses to ensure the proper
	 * jars are on the classpath.
	 * 
	 * @param defaultSpec The default spec for default jars
	 */
	public void addDefault(String defaultSpec) throws Exception {
		Collection<Container> deflts = project.getBundles(Strategy.HIGHEST, defaultSpec, null);
		for (Container c : deflts)
			addClasspath(c);
	}

	/**
	 * Create a self executable.
	 */

	public Jar executable() throws Exception {
		throw new UnsupportedOperationException();
	}

	public File getCwd() {
		return cwd;
	}

	public void setCwd(File cwd) {
		this.cwd = cwd;
	}

	public String getRunJdb() {
		return project.getProperty(Constants.RUNJDB);
	}

	public Map<String, String> getRunEnv() {
		String runenv = project.getProperty(Constants.RUNENV);
		if (runenv != null) {
			return OSGiHeader.parseProperties(runenv);
		}
		return Collections.emptyMap();
	}

	public static interface NotificationListener {
		void notify(NotificationType type, String notification);
	}

	public static enum NotificationType {
		ERROR,
		WARNING,
		INFO;
	}

	public void registerForNotifications(NotificationListener listener) {
		listeners.add(listener);
	}

	public Set<NotificationListener> getNotificationListeners() {
		return Collections.unmodifiableSet(listeners);
	}

	/**
	 * Set the stderr and stdout streams for the output process. The debugged
	 * process must append its output (i.e. write operation in the process under
	 * debug) to the given appendables.
	 * 
	 * @param out std out
	 * @param err std err
	 */
	public void setStreams(Appendable out, Appendable err) {
		this.out = out;
		this.err = err;
	}

	/**
	 * Write text to the debugged process as if it came from stdin.
	 * 
	 * @param text the text to write
	 * @throws Exception
	 */
	public void write(String text) throws Exception {

	}

	/**
	 * Get the run sessions. If this return null, then launch on this object
	 * should be used, otherwise each returned object provides a remote session.
	 * 
	 * @throws Exception
	 */

	public List<? extends RunSession> getRunSessions() throws Exception {
		return null;
	}

	/**
	 * Utility to calculate the final framework properties from settings
	 */
	/**
	 * This method should go to the ProjectLauncher
	 * 
	 * @throws Exception
	 */

	public void calculatedProperties(Map<String, Object> properties) throws Exception {

		if (!keep)
			properties.put(org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN,
				org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);

		if (!runsystemcapabilities.isEmpty())
			properties.put(org.osgi.framework.Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA,
				runsystemcapabilities.toString());

		if (!runsystempackages.isEmpty())
			properties.put(org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, runsystempackages.toString());

	}

}
