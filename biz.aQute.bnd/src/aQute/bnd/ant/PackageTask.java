package aQute.bnd.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;

import aQute.bnd.build.*;
import aQute.bnd.build.Project;
import aQute.bnd.osgi.*;
import aQute.lib.io.*;

public class PackageTask extends BaseTask {

	String	runFilePath	= null;
	File	output		= null;

	@Override
	public void execute() throws BuildException {
		if (output == null)
			throw new BuildException("Output file must be specified");

		OutputStream outStream = null;
		try {
			// Prepare the project to be packaged
			List<Project> projects;
			File baseDir = getProject().getBaseDir();
			Project baseProject = Workspace.getProject(baseDir);

			Project packageProject;
			if (runFilePath == null || runFilePath.length() == 0 || ".".equals(runFilePath)) {
				packageProject = baseProject;
			} else {
				File runFile = new File(baseDir, runFilePath);
				if (!runFile.isFile())
					throw new BuildException(String.format("Run file %s does not exist (or is not a file).", runFile.getAbsolutePath()));
				packageProject = new Project(baseProject.getWorkspace(), baseDir, runFile);
				packageProject.setParent(baseProject);
			}

			// Package it
			packageProject.clear();
			ProjectLauncher launcher = packageProject.getProjectLauncher();
			Jar jar = launcher.executable();

			outStream = new FileOutputStream(output);
			jar.write(outStream);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
		finally {
			IO.close(outStream);
		}
	}

	public void setRunfile(String runFile) {
		this.runFilePath = runFile != null ? runFile.trim() : null;
	}

	public void setOutput(File output) {
		this.output = output;
	}


}
