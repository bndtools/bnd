package aQute.bnd.ant;

import java.io.*;

import org.apache.tools.ant.*;

import aQute.bnd.build.*;
import aQute.bnd.build.Project;

public class RunBundlesTask extends BaseTask {

	private File			rootDir;
	private String			outputDir;
	private File	bndFile	= new File(Project.BNDFILE);

	@Override
	public void execute() throws BuildException {
		try {
			createReleaseDir();
			Workspace workspace;
			if (rootDir != null) {
				workspace = new Workspace(rootDir);
			} else {
				workspace = Workspace.findWorkspace(bndFile);
				log("Workspace not specified, using " + workspace.getBase().getAbsolutePath());
			}
			// 2nd arg is not used, so no use trying to set it
			Project bndProject = new Project(workspace, null, bndFile);
			
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

	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	public void setBndFile(File bndFile) {
		this.bndFile = bndFile;
	}
}
