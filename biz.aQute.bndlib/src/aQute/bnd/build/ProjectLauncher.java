package aQute.bnd.build;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;

import aQute.lib.osgi.*;
import aQute.libg.command.*;
import aQute.libg.generics.*;

/**
 * A Project Launcher is a base class to be extended by launchers. Launchers are
 * JARs that launch a framework and install a number of bundles and then run the
 * framework. A launcher jar must specify a Launcher-Class manifest header. This
 * class is instantiated and cast to a LauncherPlugin. This plug in is then
 * asked to provide a ProjectLauncher. This project launcher is then used by the
 * project to run the code. Launchers must extend this class.
 * 
 */
public abstract class ProjectLauncher {
	private final Project							project;
	private long									timeout				= 60 * 60 * 1000;
	private final Collection<Container>				runpath;
	private final Collection<String>				classpath			= new ArrayList<String>();
	private final List<File>						runbundles			= Create.list();
	private final List<String>						runvm				= new ArrayList<String>();
	private final Map<String, String>				runproperties;
	private Command									java;
	private final Map<String, Map<String, String>>	runsystempackages;
	private final List<String>						activators			= Create.list();

	private int										loglevel;
	private boolean									keep;
	private boolean									report;

	// MUST BE ALIGNED WITH LAUNCHER
	public final static int							OK					= 0;
	public final static int							WARNING				= -1;
	public final static int							ERROR				= -2;
	public final static int							TIMEDOUT			= -3;
	public final static int							UPDATE_NEEDED		= -4;
	public final static int							CANCELED			= -5;
	public final static int							DUPLICATE_BUNDLE	= -6;
	public final static int							RESOLVE_ERROR		= -7;
	public final static int							ACTIVATOR_ERROR		= -8;
	public final static int							CUSTOM_LAUNCHER		= -128;

	public final static String						EMBEDDED_ACTIVATOR	= "Embedded-Activator";

	public ProjectLauncher(Project project) throws Exception {
		this.project = project;
		runbundles.addAll(project.toFile(project.getRunbundles()));
		File[] builds = project.build();
		if (builds != null)
			runbundles.addAll(Arrays.asList(builds));
		runpath = project.getRunpath();
		runsystempackages = project.parseHeader(project.getProperty(Constants.RUNSYSTEMPACKAGES));

		for (Container c : runpath) {
			addRunpath(c);
		}
		runvm.addAll(project.getRunVM());
		runproperties = project.getRunProperties();
	}

	public void addRunpath(Container container) throws Exception {
		if (container.getError() != null) {
			project.error("Cannot launch because %s has reported %s", container.getProject(),
					container.getError());
		} else {
			Collection<Container> members = container.getMembers();
			for (Container m : members) {
				classpath.add(m.getFile().getAbsolutePath());

				Manifest manifest = m.getManifest();
				if (manifest != null) {
					Map<String, Map<String, String>> exports = project.parseHeader(manifest
							.getMainAttributes().getValue("Export-Package"));
					runsystempackages.putAll(exports);

					// Allow activators on the runpath. They are called after
					// the framework is completely initialized wit the system
					// context.
					String activator = manifest.getMainAttributes().getValue(EMBEDDED_ACTIVATOR);
					if (activator != null)
						activators.add(activator);
				}
			}
		}
	}

	public void addRunBundle(File f) {
		runbundles.add(f);
	}

	public Collection<File> getRunBundles() {
		return runbundles;
	}

	public void addRunVM(String arg) {
		runvm.add(arg);
	}

	public Collection<Container> getRunpath() {
		return runpath;
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

	public Map<String, String> getRunProperties() {
		return runproperties;
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

		int result = java.execute(System.in, System.out, System.err);
		reportResult(result);
		return result;
	}

	protected void reportResult(int result) {
		switch (result) {
		case OK:
			project.trace("Command terminated normal %s", java);
			break;
		case TIMEDOUT:
			project.error("Launch timedout: %s", java);
			break;

		case ERROR:
			project.error("Launch errored: %s", java);
			break;

		case WARNING:
			project.warning("Launch had a warning %s", java);
			break;
		default:
			project.warning("Unknown code %d from launcher: %s", result, java);
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

	public Map<String, Map<String, String>> getSystemPackages() {
		return runsystempackages;
	}

	public void setKeep(boolean keep) {
		this.keep = keep;
	}

	public boolean isKeep() {
		return keep;
	}

	public void setReport(boolean report) {
		this.report = report;
	}

	public boolean isReport() {
		return report;
	}

	public void setLogLevel(int level) {
		this.loglevel = level;
	}

	public int getLogLevel() {
		return this.loglevel;
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
}
