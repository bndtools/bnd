package aQute.bnd.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

public class DeployTask extends BaseTask {
	private final static Logger	logger		= LoggerFactory.getLogger(DeployTask.class);
	private String				deployRepo	= null;
	List<FileSet>				filesets	= new ArrayList<>();

	@Override
	public void execute() throws BuildException {
		try {
			Project project = Workspace.getProject(getProject().getBaseDir());

			// Deploy the files that need to be released
			for (FileSet fileset : filesets) {
				DirectoryScanner ds = fileset.getDirectoryScanner(getProject());
				String[] files = ds.getIncludedFiles();
				if (files.length == 0)
					logger.debug("No files included");

				for (int i = 0; i < files.length; i++) {
					File file = new File(ds.getBasedir(), files[i]);
					try {
						if (file.isFile() && file.getName()
							.endsWith(".jar")) {
							if (deployRepo != null)
								project.deploy(deployRepo, file);
							else
								project.deploy(file);
						} else
							messages.NotAJarFile_(file);
					} catch (Exception e) {
						messages.FailedToDeploy_Exception_(file, e);
					}
				}
			}
			report(project);
			if (project.getErrors()
				.size() > 0)
				throw new BuildException("Deploy failed");
		} catch (Throwable t) {
			t.printStackTrace();
			throw new BuildException(t);
		}
	}

	public void setDeployrepo(String name) {
		this.deployRepo = name;
	}

	public void addFileset(FileSet files) {
		this.filesets.add(files);
	}

}
