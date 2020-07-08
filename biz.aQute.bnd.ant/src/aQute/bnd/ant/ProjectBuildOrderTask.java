package aQute.bnd.ant;

import java.io.File;
import java.util.Collection;

import org.apache.tools.ant.BuildException;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

/**
 * ProjectBuildOrderTask calculates the correct build order for all of the bnd
 * projects in a workspace. The bndFile property can be set to calculate the
 * build order for a specific project instead of the whole workspace.
 */
public class ProjectBuildOrderTask extends BaseTask {

	private static final String	PROP_BUILD_ORDER		= "buildorder";
	private String				propertyName			= PROP_BUILD_ORDER;

	private String				separator				= ",";
	private File				workspaceLocation;
	private boolean				fullpath				= false;

	private File				projectLocation			= null;
	private String				bndFile					= Project.BNDFILE;
	private boolean				delayRunDependencies	= true;

	@Override
	public void execute() throws BuildException {
		try {

			if (workspaceLocation == null) {
				throw new BuildException("The given workspace dir is not set");
			}

			if (!workspaceLocation.isDirectory()) {
				throw new BuildException("The given workspace dir  not exist " + workspaceLocation);
			}

			if (projectLocation != null && bndFile == null) {
				throw new BuildException("Attributes projectLocation and bndFile must be used together.");
			}

			Collection<Project> projects;
			workspaceLocation = workspaceLocation.getCanonicalFile();
			Workspace workspace = Workspace.getWorkspace(workspaceLocation);

			if (projectLocation == null) {
				// all projects in workspace
				try {
					for (Project project : workspace.getAllProjects()) {
						project.setDelayRunDependencies(this.delayRunDependencies);
					}
					projects = workspace.getBuildOrder();
				} catch (Exception e) {
					throw new BuildException(e);
				}
			} else {
				try (Project p = new Project(workspace, projectLocation, new File(projectLocation, bndFile))) {
					p.setDelayRunDependencies(this.delayRunDependencies);
					projects = p.getDependson();
				}
			}

			StringBuilder sb = new StringBuilder();
			String sep = "";
			for (Project project : projects) {
				sb.append(sep);
				if (fullpath) {
					sb.append(project.getBase()
						.getAbsolutePath());
				} else {
					sb.append(project.getName());
				}
				sep = separator;
			}

			getProject().setProperty(propertyName, sb.toString());
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}

	/**
	 * Sets character (or string) separator between projects in resultant ant
	 * property.
	 *
	 * @param separator character (or string) separator
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * Sets Bnd workspace location.
	 *
	 * @param workspaceLocation Bnd workspace location.
	 */
	public void setWorkspaceLocation(File workspaceLocation) {
		this.workspaceLocation = workspaceLocation;
	}

	/**
	 * Sets whether to use project names or full absolute paths to projects in
	 * the resultant ant property. Default is project names.
	 *
	 * @param fullpath true for full absolete paths to project, false for
	 *            project names.
	 */
	public void setFullPath(boolean fullpath) {
		this.fullpath = fullpath;
	}

	/**
	 * Sets the project directory which contains the bndFile. Must be used with
	 * the bndFile parameter. Default is unset (null), which instructions
	 * ProjectBuildOrderTask to acquire the build order for the entire workspace
	 *
	 * @param projectLocation Bnd project directory
	 */
	public void setProjectDir(File projectLocation) {
		if (projectLocation != null && projectLocation.isDirectory()) {
			this.projectLocation = projectLocation;
		} else {
			throw new BuildException("Invalid projectDir!");
		}
	}

	/**
	 * Sets a single bnd file for ProjectBuildOrderTask to acquire the build
	 * order from. Default is bnd.bnd. Default is unset (null), which
	 * instructions ProjectBuildOrderTask to acquire the build order for the
	 * entire workspace
	 *
	 * @param bndFileParam bnd file
	 */
	public void setBndFile(String bndFileParam) {
		if (bndFileParam != null && bndFileParam.length() > 0) {
			this.bndFile = bndFileParam;
		} else {
			throw new BuildException("Invalid bndFile parameter!");
		}
	}

	/**
	 * Sets the ant property that will contain the list of projects in build
	 * order. If not provided, the default ant property name is buildorder.
	 *
	 * @param newProperty ant property name
	 */
	public void setProperty(String newProperty) {
		if (newProperty != null && newProperty.length() > 0) {
			propertyName = newProperty;
		} else {
			throw new BuildException("Invalid property property!");
		}
	}

	/**
	 * Set true to ignore runbundles dependencies. Set false to include
	 * runbundles dependencies in buildorder.
	 *
	 * @param b true/false
	 */
	public void setDelayRunDependencies(boolean b) {
		this.delayRunDependencies = b;
	}
}
