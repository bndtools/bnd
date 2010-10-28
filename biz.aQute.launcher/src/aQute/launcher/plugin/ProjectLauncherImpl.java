package aQute.launcher.plugin;

import java.io.*;

import aQute.bnd.build.*;
import aQute.launcher.constants.*;
import aQute.lib.osgi.*;

public class ProjectLauncherImpl extends ProjectLauncher  {
	final private Project	project;
	final private File		propertiesFile;
	boolean					prepared;

	public ProjectLauncherImpl(Project project) throws Exception {
		super(project);
		project.trace("created a aQute launcher plugin");
		this.project = project;
		propertiesFile = new File(project.getTarget(), "launch.properties");
		addRunVM("-D" + LauncherConstants.LAUNCHER_PROPERTIES + "="
				+ propertiesFile.getAbsolutePath());

		if (project.getRunProperties().get("noframework") != null) {
			setRunFramework(NONE);
			project
					.warning("The noframework property in -runproperties is replaced by a project setting: '-runframework: none'");
		}

		super.addDefault(Constants.DEFAULT_LAUNCHER_BSN);
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

		project.trace("preparing the aQute launcher plugin");

		prepared = true;
		LauncherConstants lc = new LauncherConstants();

		lc.runProperties = getRunProperties();
		lc.storageDir = new File(project.getTarget(), "fw");
		lc.keep =  isKeep();
		lc.runbundles.addAll(getRunBundles());
		lc.trace = getTrace();
		lc.timeout = getTimeout();
		lc.services = super.getRunFramework() ==  SERVICES ? true : false;
		lc.activators.addAll( getActivators());
		
		if (!getSystemPackages().isEmpty()) {
			lc.systemPackages = Processor.printClauses(getSystemPackages(), null);
		}

		OutputStream out = new FileOutputStream(propertiesFile);
		try {
			lc.getProperties().store(out, "Launching " + project);
		} finally {
			out.close();
		}
	}

}
