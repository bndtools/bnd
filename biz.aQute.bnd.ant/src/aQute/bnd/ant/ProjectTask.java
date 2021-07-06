package aQute.bnd.ant;

/**
 * The idea of this task is to read all the properties as if bnd has read them.
 * This makes it easier to use bnd standalone on the same data.
 */
import java.io.File;

import org.apache.tools.ant.BuildException;

import aQute.bnd.build.Project;

public class ProjectTask extends BaseTask {
	File	basedir;
	boolean	underTest;

	@Override
	public void execute() throws BuildException {
		try {
			Project project = getBndProject(basedir);
			project.build(underTest);
			report(project);
		} catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
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

	public void setUnderTest(boolean underTest) {
		this.underTest = underTest;
	}
}
