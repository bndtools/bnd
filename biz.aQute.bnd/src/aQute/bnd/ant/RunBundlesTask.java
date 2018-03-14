package aQute.bnd.ant;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;

public class RunBundlesTask extends BaseTask {

	private File	rootDir;
	private String	outputDir;
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
				log("Workspace not specified, using " + workspace.getBase()
					.getAbsolutePath());
			}
			// 2nd arg is not used, so no use trying to set it
			Project bndProject = new Project(workspace, null, bndFile);

			bndProject.exportRunbundles(bndFile.getName(), new File(outputDir));

			bndProject.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
	}

	private File createReleaseDir() {
		File releaseDir = new File(outputDir);
		try {
			IO.deleteWithException(releaseDir);
			log("Deleted directory " + outputDir);
		} catch (IOException e1) {
			// ignore
		}

		try {
			IO.mkdirs(releaseDir);
			log("Created directory " + outputDir);
		} catch (IOException e) {
			throw new BuildException("Output directory '" + outputDir + "' could not be created", e);
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
