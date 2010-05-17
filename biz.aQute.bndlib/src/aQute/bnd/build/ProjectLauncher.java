package aQute.bnd.build;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import aQute.lib.osgi.*;
import aQute.libg.command.*;
import aQute.libg.header.*;
import bndtools.launcher.*;

public class ProjectLauncher extends Processor {
	final Project			project;
	long					timeout		= 60 * 60 * 1000;
	File					run;
	boolean					debug;
	Collection<Container>	runpath;
	List<File>				runbundles	= new ArrayList<File>();
	File					storage;

	public ProjectLauncher(Project project) throws Exception {
		super(project);
		this.project = project;
		run = File.createTempFile("bnd-" + project, ".properties");
		run.deleteOnExit();
		runbundles.addAll(project.toFile(project.getRunbundles()));
		runbundles.addAll(Arrays.asList(project.build()));
		runpath = project.getRunpath();
	}

	public void addBundle(File f) {
		runbundles.add(f);
	}

	public Collection<File> getClasspath() throws Exception {
		return project.toFile(runpath);
	}

	public String getMainTypeName() {
		for (Container c : runpath) {
			String mainType = c.getAttributes().get(ATTR_MAIN);
			if (mainType != null)
				return mainType;
		}
		return "bndtools.launcher.Main";
	}

	public Collection<String> getVMArguments() {
		return Processor.split(getProperty(RUNVM));
	}

	public Collection<String> getArguments() {
		List<String> args = new ArrayList<String>();
		if (debug) {
			args.add("--debug");
		}
		args.add(run.getAbsolutePath());
		return args;
	}

	public void update() throws Exception {
		Properties p = new Properties();
		p.put(LauncherConstants.PROP_RUN_BUNDLES, Processor.join(runbundles));

		Map<String, String> properties = OSGiHeader.parseProperties(getProperty(RUNPROPERTIES));
		p.putAll(properties);

		if (getProperty(RUNSYSTEMPACKAGES) != null)
			p.put(org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
					getProperty(RUNSYSTEMPACKAGES));
		p.put(org.osgi.framework.Constants.FRAMEWORK_STORAGE,
				new File(project.getTarget(), "fwtmp").getAbsolutePath());

		
		FileOutputStream fout = new FileOutputStream(run);
		try {
			p.store(fout, "OSGi Run for ");
		} finally {
			fout.close();
		}
	}

	public void launch() throws Exception {
		update();
		Command c = new Command();
		c.add(getProperty("java", "java"));
		c.add("-cp");
		c.add(Processor.join(getClasspath(), File.pathSeparator));
		c.addAll(getVMArguments());
		c.add(getMainTypeName());
		c.addAll(getArguments());
		if (timeout != 0)
			c.setTimeout(timeout, TimeUnit.MILLISECONDS);
		int result = c.execute(System.in, System.out, System.err);
		if (result < 0) {
			error("Command %s failed %d", c, result);
		}
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
}
