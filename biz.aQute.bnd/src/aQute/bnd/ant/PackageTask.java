package aQute.bnd.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;

import aQute.bnd.build.Workspace;

public class PackageTask extends BaseTask {
	String	runFilePath	= null;
	File	output		= null;
	boolean	keep		= false;

	@SuppressWarnings("deprecation")
	@Override
	public void execute() throws BuildException {
		if (output == null)
			throw new BuildException("Output file must be specified");

		try {
			Workspace.getProject(getProject().getBaseDir())
				.export(runFilePath, keep, output);
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}

	public void setRunfile(String runFile) {
		this.runFilePath = runFile != null ? runFile.trim() : null;
	}

	public void setOutput(File output) {
		this.output = output;
	}
}
