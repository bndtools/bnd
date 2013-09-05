package aQute.bnd.ant;

/**
 * The idea of this task is to read all the properties as if bnd has read them.
 * This makes it easier to use bnd standalone on the same data.
 */

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;

import aQute.bnd.build.*;
import aQute.bnd.build.Project;

public class PrepareTask extends BaseTask {
	File	basedir;
	boolean	print	= false;
	String	top;

	@Override
	public void execute() throws BuildException {
		try {
			if (basedir == null || !basedir.isDirectory())
				throw new BuildException("The given base dir does not exist " + basedir);

			Workspace workspace = Workspace.getWorkspace(basedir.getParentFile());
			workspace.addBasicPlugin(new ConsoleProgress());
			
			Project project = workspace.getProject(basedir.getName());
			if (project == null)
				throw new BuildException("Unable to find bnd project in directory: " + basedir);

			project.setProperty("in.ant", "true");
			project.setProperty("environment", "ant");

			// Check if we are in a sub build, in that case
			// top will be set to the target directory at the
			// top project.
			if (top != null && top.length() > 0 && !top.startsWith("$"))
				project.setProperty("top", top);

			project.setExceptions(true);
			Properties properties = project.getFlattenedProperties();
			if (report() || report(workspace) || report(project))
				throw new BuildException("Errors during Eclipse Path inspection");

			copyProperties(properties);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
	}

	private void copyProperties(Properties flattened) {
		for (Enumeration< ? > k = flattened.propertyNames(); k.hasMoreElements();) {
			String key = (String) k.nextElement();
			String value = flattened.getProperty(key);
			if (isPrint())
				System.err.printf("%-20s = %s%n", key, value);

			// We override existing values.
			getProject().setProperty(key, value.trim());
		}
	}

	public boolean isPrint() {
		return print;
	}

	/**
	 * Print out the properties when they are set in sorted order
	 * 
	 * @param print
	 */
	public void setPrint(boolean print) {
		this.print = print;
	}

	/**
	 * Set the base directory of the project. This property MUST be set.
	 * 
	 * @param basedir
	 */
	public void setBasedir(File basedir) {
		this.basedir = basedir;
	}

	/**
	 * Set the base directory of the project. This property MUST be set.
	 * 
	 * @param basedir
	 */
	public void setTop(String top) {
		this.top = top;
	}
}
