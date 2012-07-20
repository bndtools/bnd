package aQute.bnd.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;

import aQute.bnd.build.*;
import aQute.bnd.build.Project;

public class ProjectBuildOrderTask extends BaseTask {

	private static final String	PROP_BUILD_ORDER	= "buildorder";

	private String				separator			= ",";
	private File				workspaceLocation;

	@Override
	public void execute() throws BuildException {
		try {

			if (workspaceLocation == null) {
				throw new BuildException("The given workspace dir is not set");
			}

			if (!workspaceLocation.isDirectory()) {
				throw new BuildException("The given workspace dir  not exist " + workspaceLocation);
			}

			Collection<Project> projects;
			try {
				workspaceLocation = workspaceLocation.getCanonicalFile();
				Workspace workspace = Workspace.getWorkspace(workspaceLocation);
				projects = workspace.getBuildOrder();
			}
			catch (Exception e) {
				throw new BuildException(e);
			}

			StringBuilder sb = new StringBuilder();
			String sep = "";
			for (Project project : projects) {
				sb.append(sep);
				sb.append(project.getName());
				sep = separator;
			}

			getProject().setProperty(PROP_BUILD_ORDER, sb.toString());
		}
		catch (Exception e) {
			throw new BuildException(e);
		}
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public void setWorkspaceLocation(File workspaceLocation) {
		this.workspaceLocation = workspaceLocation;
	}
}
