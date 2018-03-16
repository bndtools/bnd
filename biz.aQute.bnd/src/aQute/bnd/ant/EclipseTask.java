package aQute.bnd.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;

import aQute.bnd.osgi.eclipse.EclipseClasspath;

public class EclipseTask extends BaseTask {
	private String		prefix		= "project.";
	private List<File>	prebuild	= new ArrayList<>();
	private File		workspaceLocation;
	private String		separator	= ",";
	private File		projectLocation;

	@Override
	public void execute() throws BuildException {
		try {
			if (projectLocation == null)
				projectLocation = getProject().getBaseDir();

			if (workspaceLocation == null)
				workspaceLocation = projectLocation.getParentFile();

			EclipseClasspath eclipse = new EclipseClasspath(this, workspaceLocation, projectLocation);

			if (report())
				throw new BuildException("Errors during Eclipse Path inspection");

			addProperty(prefix + "classpath", join(eclipse.getClasspath(), separator));

			addProperty(prefix + "bootclasspath", join(eclipse.getBootclasspath(), separator));

			if (!eclipse.getSourcepath()
				.isEmpty())
				addProperty(prefix + "sourcepath", join(eclipse.getSourcepath(), separator));

			addProperty(prefix + "output", eclipse.getOutput()
				.getAbsolutePath());

			/**
			 * The prebuild is an attribute that is prepended to the dependency
			 * path derived from the Eclipse project
			 */

			List<File> dependents = new ArrayList<>();
			addCareful(dependents, prebuild);
			addCareful(dependents, eclipse.getDependents());
			if (dependents.size() > 0) {
				addProperty(prefix + "buildpath", join(dependents, separator));
			}
		} catch (Exception e) {
			throw new BuildException("Error during parsing Eclipse .classpath files", e);
		}
	}

	private void addCareful(List<File> result, Collection<File> projects) {
		for (Iterator<File> i = projects.iterator(); i.hasNext();) {
			File d = i.next();
			if (!result.contains(d))
				result.add(d);
		}
	}

	protected void addProperty(String n, String v) {
		if (v != null)
			getProject().setProperty(n, v);
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setPrebuild(String prebuild) {
		StringTokenizer st = new StringTokenizer(prebuild, " ,");
		while (st.hasMoreTokens()) {
			this.prebuild.add(getFile(getProject().getBaseDir()
				.getParentFile(), st.nextToken()));
		}
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public void setProjectLocation(File projectLocation) {
		this.projectLocation = projectLocation;
	}

	public void setWorkspaceLocation(File workspaceLocation) {
		this.workspaceLocation = workspaceLocation;
	}
}
