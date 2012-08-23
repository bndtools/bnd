package aQute.bnd.build;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.jar.*;

import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.libg.command.*;
import aQute.libg.generics.*;

/**
 * A Project Launcher is a base class to be extended by launchers. Launchers are
 * JARs that launch a framework and install a number of bundles and then run the
 * framework. A launcher jar must specify a Launcher-Class manifest header. This
 * class is instantiated and cast to a LauncherPlugin. This plug in is then
 * asked to provide a ProjectLauncher. This project launcher is then used by the
 * project to run the code. Launchers must extend this class.
 */
public abstract class ProjectLauncher {
	private final Project		project;
	private long				timeout				= 0;
	private final List<String>	classpath			= new ArrayList<String>();
	private List<String>		runbundles			= Create.list();
	private final List<String>	runvm				= new ArrayList<String>();
	private Map<String,String>	runproperties;
	private Command				java;
	private Parameters			runsystempackages;
	private final List<String>	activators			= Create.list();
	private File				storageDir;
	private final List<String>	warnings			= Create.list();
	private final List<String>	errors				= Create.list();

	private boolean				trace;
	private boolean				keep;
	private int					framework;

	public final static int		SERVICES			= 10111;
	public final static int		NONE				= 20123;

	// MUST BE ALIGNED WITH LAUNCHER
	public final static int		OK					= 0;
	public final static int		WARNING				= -1;
	public final static int		ERROR				= -2;
	public final static int		TIMEDOUT			= -3;
	public final static int		UPDATE_NEEDED		= -4;
	public final static int		CANCELED			= -5;
	public final static int		DUPLICATE_BUNDLE	= -6;
	public final static int		RESOLVE_ERROR		= -7;
	public final static int		ACTIVATOR_ERROR		= -8;
	public final static int		CUSTOM_LAUNCHER		= -128;

	public final static String	EMBEDDED_ACTIVATOR	= "Embedded-Activator";

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
		// pkr: could not use this because this is killing the runtests.
		// project.refresh();
		runbundles.clear();
		Collection<Container> run = project.getRunbundles();

		for (Container container : run) {
			File file = container.getFile();
			if (file != null && (file.isFile() || file.isDirectory())) {
				runbundles.add(file.getAbsolutePath());
			} else {
				warning("Bundle file \"%s\" does not exist", file);
			}
		}

		if (project.getRunBuilds()) {
			File[] builds = project.build();
			if (builds != null)
				for (File file : builds)
					runbundles.add(file.getAbsolutePath());
		}

		Collection<Container> runpath = project.getRunpath();
		runsystempackages = project.getParameters(Constants.RUNSYSTEMPACKAGES);
		framework = getRunframework(project.getProperty(Constants.RUNFRAMEWORK));
		trace = Processor.isTrue(project.getProperty(Constants.RUNTRACE));

		timeout = Processor.getDuration(project.getProperty(Constants.RUNTIMEOUT), 0);
		trace = Processor.isTrue(project.getProperty(Constants.RUNTRACE));

		List<Container> fws = project.getBundles(Strategy.HIGHEST, project.getProperty(Constants.RUNFW), Constants.RUNFW);
		runpath.addAll(fws);

		for (Container c : runpath) {
			addClasspath(c);
		}

		runvm.addAll(project.getRunVM());
		runproperties = project.getRunProperties();

		storageDir = project.getRunStorage();
		if (storageDir == null) {
			storageDir = new File(project.getTarget(), "fw");
		}
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
				String path = m.getFile().getAbsolutePath();
				if (!classpath.contains(path)) {
					classpath.add(path);

					Manifest manifest = m.getManifest();

					if (manifest != null) {
						Parameters exports = project.parseHeader(manifest.getMainAttributes().getValue(
								Constants.EXPORT_PACKAGE));
						for (Entry<String,Attrs> e : exports.entrySet()) {
							if (!runsystempackages.containsKey(e.getKey()))
								runsystempackages.put(e.getKey(), e.getValue());
						}

						// Allow activators on the runpath. They are called
						// after
						// the framework is completely initialized wit the
						// system
						// context.
						String activator = manifest.getMainAttributes().getValue(EMBEDDED_ACTIVATOR);
						if (activator != null)
							activators.add(activator);
					}
				}
			}
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

	public List<String> getRunpath() {
		return classpath;
	}

	public Collection<String> getClasspath() {
		return classpath;
	}

	public Collection<String> getRunVM() {
		return runvm;
	}

	public Collection<String> getArguments() {
		return Collections.emptySet();
	}

	public Map<String,String> getRunProperties() {
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
		java.add(project.getProperty("java", "java"));
		java.add("-cp");
		java.add(Processor.join(getClasspath(), File.pathSeparator));
		java.addAll(getRunVM());
		java.add(getMainTypeName());
		java.addAll(getArguments());
		if (timeout != 0)
			java.setTimeout(timeout + 1000, TimeUnit.MILLISECONDS);

		try {
			int result = java.execute(System.in, System.err, System.err);
			if (result == Integer.MIN_VALUE)
				return TIMEDOUT;
			reportResult(result);
			return result;
		}
		finally {
			cleanup();
		}
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
				project.trace("Command terminated normal %s", java);
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

	public void cancel() {
		java.cancel();
	}

	public Map<String, ? extends Map<String,String>> getSystemPackages() {
		return runsystempackages.asMapMap();
	}

	public void setKeep(boolean keep) {
		this.keep = keep;
	}

	public boolean isKeep() {
		return keep;
	}

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
	 * 
	 * @return
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
	 * @param defaultSpec
	 *            The default spec for default jars
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

	public void clear() {
		errors.clear();
		warnings.clear();
	}

	public List<String> getErrors() {
		return Collections.unmodifiableList(errors);
	}

	public List<String> getWarnings() {
		return Collections.unmodifiableList(warnings);
	}

	protected void error(String message, Object... args) {
		String formatted = String.format(message, args);
		errors.add(formatted);
	}

	protected void warning(String message, Object... args) {
		String formatted = String.format(message, args);
		warnings.add(formatted);
	}
}
