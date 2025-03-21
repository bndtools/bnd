package aQute.bnd.build;

import static java.util.Objects.requireNonNull;
import static org.osgi.framework.Constants.FRAMEWORK_BEGINNING_STARTLEVEL;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.help.instructions.BuilderInstructions;
import aQute.bnd.help.instructions.LauncherInstructions;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.Strategy;
import aQute.lib.io.IO;
import aQute.lib.startlevel.StartLevelRuntimeHandler;
import aQute.lib.strings.Strings;
import aQute.lib.watcher.FileWatcher;
import aQute.lib.watcher.FileWatcher.Builder;
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
	public static final String			EMBEDDED_ACTIVATOR	= "Embedded-Activator";

	final static Logger					logger				= LoggerFactory.getLogger(ProjectLauncher.class);
	private final Project				project;
	private final List<Runnable>		onUpdate			= new ArrayList<>();
	private long						timeout				= 0L;
	private final List<String>			classpath			= new ArrayList<>();
	private List<String>				runbundles			= Create.list();

	private final List<String>			runvm				= new ArrayList<>();
	private final List<String>			runprogramargs		= new ArrayList<>();
	private boolean						runframeworkrestart;
	private Map<String, String>			runproperties;
	private Command						java;
	private Parameters					runsystempackages;
	private Parameters					runsystemcapabilities;
	private final List<String>			activators			= Create.list();
	private File						storageDir;
	protected BuilderInstructions		builderInstrs;
	protected LauncherInstructions		launcherInstrs;

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
	public final static int				WARNING				= 126 - 1;
	public final static int				ERROR				= 126 - 2;
	public final static int				TIMEDOUT			= 126 - 3;
	public final static int				UPDATE_NEEDED		= 126 - 4;
	public final static int				CANCELED			= 126 - 5;
	public final static int				DUPLICATE_BUNDLE	= 126 - 6;
	public final static int				RESOLVE_ERROR		= 126 - 7;
	public final static int				ACTIVATOR_ERROR		= 126 - 8;
	public final static int				STOPPED				= 126 - 9;

	public ProjectLauncher(Project project) throws Exception {
		this.project = project;
		this.setBase(project.getBase());
		builderInstrs = project.getInstructions(BuilderInstructions.class);
		launcherInstrs = project.getInstructions(LauncherInstructions.class);

		validate();
	}

	/**
	 * Validate some settings
	 */
	protected void validate() {
		Collection<String> runvm = getRunVM();
		if (runvm.size() == 1)
			try {
				for (String r : runvm) {
					if (Verifier.isSpaceSeparated(r)) {
						SetLocation location = project.warning(
							"%s is a comma (,) separated instruction, it looks like you separate its values with spaces? If you need spaces, please quote them: %s",
							Constants.RUNVM, runvm);
						project.getHeader(Constants.RUNVM)
							.set(location);
					}
				}
			} catch (Exception e) {
				// ignore
			}
	}

	/**
	 * Collect all the aspect from the project and set the local fields from
	 * them. Should be called after constructor has been called.
	 *
	 * @throws Exception
	 */
	protected void updateFromProject() throws Exception {
		setCwd(getProject().getBase());

		// pkr: could not use this because this is killing the runtests.
		// getProject().refresh();
		runbundles.clear();
		classpath.clear();

		Collection<Container> run = getProject().getRunbundles();

		for (Container container : run) {
			File file = container.getFile();
			if (file != null && (file.isFile() || file.isDirectory())) {
				addRunBundle(IO.absolutePath(file));
			} else {
				getProject().error("Bundle file \"%s\" does not exist, given error is %s", file, container.getError());
			}
		}

		if (getProject().getRunBuilds()) {
			File[] builds = getProject().getBuildFiles(true);
			if (builds != null)
				for (File file : builds)
					addRunBundle(IO.absolutePath(file));
		}

		runsystempackages = new Parameters(getProject().mergeProperties(Constants.RUNSYSTEMPACKAGES), getProject());
		runsystemcapabilities = new Parameters(getProject().mergeProperties(Constants.RUNSYSTEMCAPABILITIES),
			getProject());
		setRunFramework(getRunframework(getProject().getProperty(Constants.RUNFRAMEWORK)));

		setTimeout(Processor.getDuration(getProject().getProperty(Constants.RUNTIMEOUT), 0L), TimeUnit.MILLISECONDS);
		setTrace(getProject().isRunTrace());

		Collection<Container> runpath = getProject().getRunpath();
		runpath.addAll(getProject().getRunFw());
		for (Container c : runpath) {
			addClasspath(c);
		}

		getProject().getRunVM()
			.forEach(this::addRunVM);
		getProject().getRunProgramArgs()
			.forEach(this::addRunProgramArgs);
		runproperties = null; // set in getRunProperties
		runframeworkrestart = getProject().is(Constants.RUNFRAMEWORKRESTART);
		storageDir = getProject().getRunStorage();

		setKeep(getProject().getRunKeep());
	}

	private int getRunframework(String property) {
		if (Constants.RUNFRAMEWORK_NONE.equalsIgnoreCase(property))
			return NONE;
		if (Constants.RUNFRAMEWORK_SERVICES.equalsIgnoreCase(property))
			return SERVICES;
		return SERVICES;
	}

	public void addClasspath(Container container) throws Exception {
		addClasspath(container, classpath);
	}

	protected void addClasspath(Container container, List<String> pathlist) throws Exception {
		if (container.getError() != null) {
			getProject().error("Cannot launch because %s has reported %s", container.getProject(),
				container.getError());
		} else {
			Collection<Container> members = container.getMembers();
			for (Container m : members) {
				String path = IO.absolutePath(m.getFile());
				if (!pathlist.contains(path)) {
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

						Parameters exports = getProject().parseHeader(manifest.getMainAttributes()
							.getValue(Constants.EXPORT_PACKAGE));
						for (Entry<String, Attrs> e : exports.entrySet()) {
							if (!runsystempackages.containsKey(e.getKey()))
								runsystempackages.put(e.getKey(), e.getValue());
						}

						// Allow activators on the runpath. They are called
						// after the framework is completely initialized
						// with the system context.
						String activator = manifest.getMainAttributes()
							.getValue(EMBEDDED_ACTIVATOR);
						if (activator != null)
							activators.add(activator);
					}
					pathlist.add(path);
				}
			}
		}
	}

	protected void addClasspath(Collection<Container> path) throws Exception {
		for (Container c : Container.flatten(path)) {
			addClasspath(c);
		}
	}

	public void addRunBundle(String path) {
		path = IO.normalizePath(path);
		if (!runbundles.contains(path)) {
			runbundles.add(path);
		}
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

	public Collection<String> getRunProgramArgs() {
		return runprogramargs;
	}

	public Map<String, String> getRunProperties() {
		Map<String, String> properties = runproperties;
		if (properties != null) {
			return properties;
		}
		properties = getProject().getRunProperties();
		try {
			calculatedProperties(properties);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
		return runproperties = properties;
	}

	public File getStorageDir() {
		return storageDir;
	}

	public abstract String getMainTypeName();

	public void update() throws Exception {
		getProject().refresh();
		updateFromProject();
		for (Runnable update : onUpdate) {
			update.run();
		}
	}

	public void onUpdate(Runnable update) {
		onUpdate.add(update);
	}

	@Override
	public String getJavaExecutable(String java) {
		return getProject().getJavaExecutable(java);
	}

	public int launch() throws Exception {
		Command command = getCommand();

		logger.debug("cmd line {}", command);
		try {
			int result = command.execute(in, out, err);
			if (result == Integer.MIN_VALUE)
				return TIMEDOUT;
			reportResult(result);
			return result;
		} finally {
			cleanup();
			listeners.clear();
		}
	}

	public Command getCommand() throws Exception {
		if (java != null) {
			return java;
		}
		prepare();
		java = new Command();

		//
		// Handle the environment
		//

		getRunEnv().forEach(java::var);

		java.add(getJavaExecutable("java"));
		if (getProject().is(Constants.JAVAAGENT)) {
			for (String agent : agents) {
				java.add("-javaagent:" + agent);
			}
		}

		String jdb = getRunJdb();
		if (jdb != null) {
			Matcher matcher = RUNJDB_P.matcher(jdb);
			String address;
			String suspend;
			if (matcher.matches()) {
				address = matcher.group("address");
				suspend = "-".equals(matcher.group("sign")) ? "n" : "y";
			} else {
				address = "1044";
				suspend = "y";
			}
			java.add("-agentlib:jdwp=transport=dt_socket,server=y,address=" + address + ",suspend=" + suspend);
		}

		java.addAll(split(System.getenv("JAVA_OPTS"), "\\s+"));

		java.add("-cp");
		java.add(join(getClasspath(), File.pathSeparator));
		java.addAll(getRunVM());
		java.add(getMainTypeName());
		java.addAll(getRunProgramArgs());
		if (getTimeout() != 0) {
			java.setTimeout(getTimeout() + 1000, TimeUnit.MILLISECONDS);
		}

		File cwd = getCwd();
		if (cwd != null)
			java.setCwd(cwd);
		return java;
	}

	/**
	 * launch a framework internally. I.e. do not start a separate process.
	 */
	final static Pattern IGNORE = Pattern.compile("org[./]osgi[./]resource.*");

	public int start(ClassLoader parent) throws Exception {
		// FIXME This seems kinda broken. I think ProjectLauncherImpl will need
		// to implement this since only it will know the main class name of the
		// non-pre jar

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
		for (String path : getRunpath()) {
			cp.add(new File(path).toURI()
				.toURL());
		}
		@SuppressWarnings("resource")
		URLClassLoader cl = new URLClassLoader(cp.toArray(new URL[0]), fcl) {
			@Override
			public void addURL(URL url) {
				super.addURL(url);
			}
		};

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
				getProject().error("Launch timedout: %s", java);
				break;

			case ERROR :
				getProject().error("Launch errored: %s", java);
				break;

			case WARNING :
				getProject().warning("Launch had a warning %s", java);
				break;
			default :
				getProject().error("Exit code remote process %d: %s", result, java);
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
		getCommand().cancel();
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
	public void prepare() throws Exception {
		// noop
	}

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
		Collection<Container> deflts = getProject().getBundles(Strategy.HIGHEST, defaultSpec, null);
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

	private static final Pattern RUNJDB_P = Pattern
		.compile("^\\s*(?<sign>[-+])?(?<address>(?:\\S+:)?\\d{1,5})\\s*$");

	public String getRunJdb() {
		return getProject().getProperty(Constants.RUNJDB);
	}

	public Map<String, String> getRunEnv() {
		String runenv = getProject().getProperty(Constants.RUNENV);
		if (runenv != null) {
			return OSGiHeader.parseProperties(runenv);
		}
		return Collections.emptyMap();
	}

	public interface NotificationListener {
		void notify(NotificationType type, String notification);
	}

	public enum NotificationType {
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

	public void calculatedProperties(Map<String, String> properties) throws Exception {
		setupStartlevels(properties);
	}

	/**
	 * Calculate the start level properties. This code is matched to the
	 * aQute.lib class {@link StartLevelRuntimeHandler} that handles the runtime
	 * details.
	 * <p>
	 * The -runbundles instruction can carry a `startlevel` attribute. If any
	 * bundle has this start level attribute we control the startlevel process.
	 * If no bundle has this attribute, then the start level handling is not
	 * doing anything. The remaining section assumes that there is at least 1
	 * bundle with a set startlevel attribute.
	 * <p>
	 * The {@link StartLevelRuntimeHandler#LAUNCH_STARTLEVEL_DEFAULT} is then
	 * set to the maximum startlevel + 1. This signals that the
	 * {@link StartLevelRuntimeHandler} class handles the runtime aspects.
	 * <p>
	 * The {@link Constants#RUNSTARTLEVEL_BEGIN} controls the beginning start
	 * level of the framework after the framework itself is started. The user
	 * can set this or else it is set to the maxmum startlevel+2.
	 * <p>
	 * During runtime, the handler must be created with
	 * {@link StartLevelRuntimeHandler#create(aQute.lib.startlevel.Trace, Map)}
	 * before the framework is created since it may change the properties. I.e.
	 * the properties given to the {@link FrameworkFactory} must be the same
	 * object as given to the create method. One thing is that it will set the
	 * {@link Constants#RUNSTARTLEVEL_BEGIN} to ensure that all bundles are
	 * installed at level 1.
	 * <p>
	 * After the framework is created, the runtime handler
	 * {@link StartLevelRuntimeHandler#beforeStart(org.osgi.framework.launch.Framework)}
	 * must be called. This will prepare that bundles will get their proper
	 * start level when installed.
	 * <p>
	 * After the set of bundles is installed, the
	 * {@link StartLevelRuntimeHandler#afterStart()} is called to raise the
	 * start level to the desired level. Either the set
	 * {@link Constants#RUNSTARTLEVEL_BEGIN} or the maximum level + 2.
	 */
	private void setupStartlevels(Map<String, String> properties) throws Exception {
		Parameters runbundles = new Parameters();
		int maxLevel = -1;

		for (Container c : project.getRunbundles()) {
			Map<String, String> attrs = c.getAttributes();
			if (attrs == null)
				continue;

			if (c.getBundleSymbolicName() == null)
				continue;

			if (c.getError() != null)
				continue;

			if (c.getFile() == null || !c.getFile()
				.isFile())
				continue;

			Attrs runtimeAttrs;
			if (attrs instanceof Attrs a) {
				runtimeAttrs = new Attrs(a);
			} else {
				runtimeAttrs = new Attrs(attrs);
			}

			String startLevelString = attrs.get(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE);
			if (startLevelString == null)
				continue;

			int startlevel = -1;

			if (!Verifier.isNumber(startLevelString)) {
				error("Invalid start level on -runbundles. bsn=%s, version=%s, startlevel=%s, not a number",
					c.getBundleSymbolicName(), c.getVersion(), startLevelString);
				continue;
			} else {
				startlevel = Integer.parseInt(startLevelString);
				if (startlevel > 0) {
					if (startlevel > maxLevel)
						maxLevel = startlevel;

				}

				Domain domain = Domain.domain(c.getFile());
				String bsn = domain.getBundleSymbolicName()
					.getKey();
				String bundleVersion = Strings.trim(domain.getBundleVersion());

				if (!Verifier.isVersion(bundleVersion)) {
					error("Invalid version on -runbundles. bsn=%s, version=%s", c.getBundleSymbolicName(),
						c.getVersion(), startLevelString);
					continue;
				} else {
					runtimeAttrs.put("version", bundleVersion);
				}
				runbundles.put(bsn, runtimeAttrs);
			}
		}

		boolean areStartlevelsEnabled = maxLevel > 0;

		String beginningLevelString = properties.get(FRAMEWORK_BEGINNING_STARTLEVEL);

		if (!runbundles.isEmpty()) {
			properties.put(Constants.LAUNCH_RUNBUNDLES_ATTRS, runbundles.toString());

			if (areStartlevelsEnabled) {
				int defaultLevel = maxLevel + 1;
				int beginningLevel = maxLevel + 2;

				if (!properties.containsKey(LAUNCH_STARTLEVEL_DEFAULT)) {
					switch (project.instructions.launcher()
						.manage()) {
						default :
						case all :
							properties.put(LAUNCH_STARTLEVEL_DEFAULT, Integer.toString(defaultLevel));
							break;
						case narrow :
							properties.put(LAUNCH_STARTLEVEL_DEFAULT, Integer.toString(-defaultLevel));
							break;
						case none :
							properties.put(LAUNCH_STARTLEVEL_DEFAULT, "0");
							break;
					}
				}

				if (beginningLevelString == null) {
					properties.put(FRAMEWORK_BEGINNING_STARTLEVEL, Integer.toString(beginningLevel));
				}
			}

		}

		if (beginningLevelString != null) {
			if (!Verifier.isNumber(beginningLevelString)) {
				error("%s set to %s, not a valid startlevel (is not a number)", beginningLevelString);
			} else {
				int beginningStartLevel = Integer.parseInt(beginningLevelString);
				if (beginningStartLevel < 1) {
					error("%s set to %s, must be > 0", beginningLevelString);
				}
			}
		}
	}

	public LiveCoding liveCoding(Executor executor, ScheduledExecutorService scheduledExecutor) throws Exception {
		return new LiveCoding(executor, scheduledExecutor);
	}

	public class LiveCoding implements Closeable {
		private final Semaphore					semaphore			= new Semaphore(1);
		private final AtomicBoolean				propertiesChanged	= new AtomicBoolean(false);
		private final Executor					executor;
		private final ScheduledExecutorService	scheduledExecutor;
		private volatile FileWatcher			fw;

		LiveCoding(Executor executor, ScheduledExecutorService scheduledExecutor) throws Exception {
			this.executor = requireNonNull(executor);
			this.scheduledExecutor = requireNonNull(scheduledExecutor);
			watch();
		}

		@Override
		public void close() {
			FileWatcher old = fw;
			if (old != null) {
				old.close();
			}
		}

		private void watch() throws IOException {
			Builder builder = new FileWatcher.Builder().executor(executor)
				.changed(this::changed)
				.file(getProject().getPropertiesFile())
				.files(getProject().getIncluded());
			for (String runpath : getRunpath()) {
				builder.file(new File(runpath));
			}
			for (String runbundle : getRunBundles()) {
				builder.file(new File(runbundle));
			}
			FileWatcher old = fw;
			fw = builder.build();
			if (old != null) {
				old.close();
			}
			logger.debug("[LiveCoding] Watching for changes...");
		}

		private void changed(File file, String kind) {
			logger.info("[LiveCoding] Detected change to {}.", file);
			propertiesChanged.compareAndSet(false, getProject().getPropertiesFile()
				.equals(file)
				|| getProject().getIncluded()
					.contains(file));
			if (semaphore.tryAcquire()) {
				scheduledExecutor.schedule(() -> {
					try {
						logger.info("[LiveCoding] Updating ProjectLauncher.");
						update();
					} catch (Exception e) {
						logger.error("[LiveCoding] Error on ProjectLauncher update", e);
					} finally {
						semaphore.release();
						if (propertiesChanged.compareAndSet(true, false)) {
							logger.info("[LiveCoding] Detected changes to bnd properties file. Replacing watcher.");
							try {
								watch();
							} catch (IOException e) {
								logger.error("[LiveCoding] Error replacing watcher {}", e);
							}
						}
					}
				}, 600, TimeUnit.MILLISECONDS);
			}
		}
	}

	public boolean isRunFrameworkRestart() {
		return runframeworkrestart;
	}

	static final String[] STRINGARRAY = new String[0];
	public static String renderArguments(Collection<String> arguments) {
		return renderArguments(arguments.toArray(STRINGARRAY));
	}

	public static String renderArguments(Collection<String> arguments, boolean isWin32) {
		return renderArguments(arguments.toArray(STRINGARRAY), isWin32);
	}

	public static String renderArguments(String[] arguments) {
		return renderArguments(arguments, IO.isWindows());
	}

	public static String renderArguments(String[] arguments, boolean isWin32) {
		StringBuilder buf = new StringBuilder();
		int count = arguments.length;
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				buf.append(' ');
			}

			boolean containsSpace = false;
			char[] characters = arguments[i].toCharArray();
			for (char ch : characters) {
				if (ch == ' ' || ch == '\t') {
					containsSpace = true;
					buf.append('"');
					break;
				}
			}

			int backslashes = 0;
			for (int j = 0; j < characters.length; j++) {
				char ch = characters[j];
				if (ch == '"') {
					if (isWin32) {
						if (j == 0 && characters.length == 2 && characters[1] == '"') {
							// empty string on windows platform, see bug 130767.
							// Bug in constructor of JDK's
							// java.lang.ProcessImpl.
							buf.append("\"\""); //$NON-NLS-1$
							break;
						}
						if (backslashes > 0) {
							// Feature in Windows: need to double-escape
							// backslashes in front of double quote.
							for (; backslashes > 0; backslashes--) {
								buf.append('\\');
							}
						}
					}
					buf.append('\\');
				} else if (ch == '\\') {
					if (isWin32) {
						backslashes++;
					} else {
						buf.append('\\');
					}
				} else if (isWin32) {
					backslashes = 0; // FIX for Eclipse code
				}
				buf.append(ch);
			}
			if (containsSpace) {
				buf.append('"');
			} else if (characters.length == 0) {
				buf.append("\"\""); //$NON-NLS-1$
			}
		}
		return buf.toString();
	}
}
