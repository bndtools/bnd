package aQute.bnd.ant;

import java.io.*;

import org.apache.tools.ant.*;

import aQute.bnd.build.*;
import aQute.bnd.build.Project;

public class RunBundlesTask extends Task {

	private File			rootDir;
	private File			buildProject;
	private String			outputDir;
	private File			bndFile;

	@Override
	public void execute() throws BuildException {
		try {
			createReleaseDir();
			Project bndProject = new Project(new Workspace(rootDir), buildProject, bndFile);
			
			bndProject.exportRunbundles(bndFile.getName(), new File(outputDir));

			bndProject.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
	}

	private File createReleaseDir() {
		File releaseDir = new File(outputDir);
		boolean deleted = releaseDir.delete();
		if (deleted) {
			log("Deleted directory " + outputDir);
		}

		boolean created = releaseDir.mkdir();
		if (created) {
			log("Created directory " + outputDir);
		} else {
			throw new BuildException("Output directory '" + outputDir + "' could not be created");
		}

		return releaseDir;
	}

	public void setRootDir(File rootDir) {
		this.rootDir = rootDir;
	}

	public void setBuildProject(File buildProject) {
		this.buildProject = buildProject;
	}

	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	public void setBndFile(File bndFile) {
		this.bndFile = bndFile;
	}
}
