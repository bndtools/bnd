package aQute.launcher.plugin;

import java.io.*;
import java.util.*;

import aQute.bnd.build.*;
import aQute.launcher.constants.*;
import aQute.lib.osgi.*;

public class ProjectLauncherImpl extends ProjectLauncher implements LauncherConstants {
	final private Project	project;
	final private File		propertiesFile;
	boolean					prepared;

	public ProjectLauncherImpl(Project project) throws Exception {
		super(project);
		this.project = project;
		propertiesFile = new File(project.getTarget(), "launch.properties");
		addRunVM("-D" + LauncherConstants.LAUNCH_PROPERTIES + "="
				+ propertiesFile.getAbsolutePath());
	}

	public String getMainTypeName() {
		return "aQute.launcher.Launcher";
	}

	public void update() throws Exception {
		propertiesFile.setLastModified(System.currentTimeMillis());
	}

	public int launch() throws Exception {
		prepare();
		return super.launch();
	}

	public void prepare() throws Exception {
		if (prepared)
			return;

		prepared = true;

		Properties properties = new Properties();
		properties.putAll(getRunProperties());
		properties.setProperty(LAUNCH_STORAGE_DIR, new File(project.getTarget(), "fw")
				.getAbsolutePath());
		properties.setProperty(LAUNCH_KEEP, "" + isKeep());
		properties.setProperty(LAUNCH_REPORT, "" + isReport());
		properties.setProperty(LAUNCH_RUNBUNDLES, Processor.join(getRunBundles()));
		properties.setProperty(LAUNCH_LOG_LEVEL, "" + getLogLevel());
		properties.setProperty(LAUNCH_TIMEOUT, "" + getTimeout());
		
		switch (super.getRunFramework()) {
		case NONE:
			properties.setProperty(LAUNCH_FRAMEWORK, Constants.RUNFRAMEWORK_NONE);
			break;

		default:
			properties.setProperty(LAUNCH_FRAMEWORK, Constants.RUNFRAMEWORK_SERVICES);
			break;
		}

		if (!getActivators().isEmpty())
			properties.setProperty(LAUNCH_ACTIVATORS, Processor.join(getActivators()));

		if (!getSystemPackages().isEmpty()) {
			String header = Processor.printClauses(getSystemPackages(), null);
			properties.setProperty(LAUNCH_SYSTEMPACKAGES, header);
		}

		OutputStream out = new FileOutputStream(propertiesFile);
		try {
			properties.store(out, "Launching " + project);
		} finally {
			out.close();
		}
	}

}
