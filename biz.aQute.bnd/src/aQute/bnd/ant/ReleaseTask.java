package aQute.bnd.ant;

import org.apache.tools.ant.BuildException;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

/**
 * <p>
 * ANT task to release into a repository, equivalent to: <code>&lt;bnd
 * command="release"/&gt;</code>
 * </p>
 * <p>
 * To release into the <em>default</em> repository (defined by
 * <code>-releaserepo</code> in <code>build.bnd</code>):
 * </p>
 *
 * <pre>
 *  &lt;bndrelease/&gt;
 * </pre>
 * <p>
 * To release into a specific named repository:
 *
 * <pre>
 *  &lt;bndrelease releaserepo="My Repository"/&gt;
 * </pre>
 *
 * @author Neil Bartlett
 * @see BndTask for setup instructions.
 */
public class ReleaseTask extends BaseTask {

	String releaseRepo = null;

	@Override
	public void execute() throws BuildException {
		try {
			Project project = Workspace.getProject(getProject().getBaseDir());
			if (releaseRepo == null) {
				project.release(false);
			} else {
				project.release(releaseRepo);
			}

			if (report(project))
				throw new BuildException("Release failed");
		} catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
	}

	public void setReleaserepo(String releaseRepo) {
		this.releaseRepo = releaseRepo;
	}

}
