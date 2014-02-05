package aQute.bnd.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;

import aQute.bnd.build.*;

public class WorkspaceSettingsEchoTask extends BaseTask {
	File	basedir;

	@Override
	public void execute() throws BuildException {
		if (basedir == null || !basedir.isDirectory())
			throw new BuildException("The given base directory does not exist " + basedir);

		Workspace ws = null;
		try {
			ws = Workspace.getWorkspace(basedir);
			Properties props = ws.getProperties();
			SettingsEchoHelpers.printProperties(props);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
		finally {
			if (ws != null) {
				ws.close();
				ws = null;
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
