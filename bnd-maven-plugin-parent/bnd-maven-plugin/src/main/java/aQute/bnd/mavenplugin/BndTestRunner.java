package aQute.bnd.mavenplugin;

import java.io.*;

import org.apache.maven.execution.*;
import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.*;

import aQute.bnd.build.*;

@Mojo(name = "integration-test")
public class BndTestRunner extends AbstractMojo {
	@Component
	MavenSession session;

	@Component
	MavenProject mavenProject;

	@Component
	BndWorkspace bndWorkspace;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			Workspace workspace = bndWorkspace.getWorkspace(session);
			Project bndProject = workspace.getProject(mavenProject.getArtifactId());
			if ( bndProject == null)
				throw new MojoExecutionException("Cannot find the bnd project " + mavenProject.getArtifactId() + " in workspace " + workspace );
			getLog().info("Running the Bnd OSGi integration tests");
			bndProject.test();

			if (bndProject.getErrors().size() > 0) {
				String errMsg = "Errors during test execution. See " + bndProject.getTargetDir().getAbsolutePath() +
					File.separator + "test-reports for more details.";
				throw new MojoFailureException(errMsg);
			}

			getLog().info("Finished running the Bnd OSGi integration tests");
		} catch (MojoExecutionException mjee) {
			throw mjee;
		} catch (MojoFailureException mfe) {
			throw mfe;
		} catch (Exception e) {
			throw new MojoExecutionException("Problem building the project.", e);
		}
	}

}
