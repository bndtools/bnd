package aQute.bnd.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;

import aQute.bnd.build.*;
import aQute.bnd.build.Project;

public class ProjectSettingsEchoTask extends BaseTask {
	File	basedir;

	@Override
	public void execute() throws BuildException {
		if (basedir == null || !basedir.isDirectory())
			throw new BuildException("The given base directory does not exist " + basedir);

		Project project = null;
		try {
			project = Workspace.getProject(basedir);
			project.prepare();
			Properties props = project.getProperties();
			SettingsEchoHelpers.printProperties(props);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
		finally {
			if (project != null) {
				project.close();
				project = null;
			}
		}
	}

	/**
	 * Set the base directory of the project. This property MUST be set.
	 * 
	 * @param basedir
	 */
	public void setBasedir(File basedir) {
		this.basedir = basedir;
	}
}
