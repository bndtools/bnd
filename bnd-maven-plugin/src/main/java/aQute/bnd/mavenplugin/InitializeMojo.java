package aQute.bnd.mavenplugin;

import java.util.*;

import org.apache.maven.execution.*;
import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.*;

@Mojo(name = "init")
public class InitializeMojo extends AbstractMojo {
	@Component
	protected MavenSession session;

	@Component
	protected MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			getLog().info("Current " + session.getCurrentProject()  + session.getProjects());
			ProjectDependencyGraph pg = session.getProjectDependencyGraph();
			List<MavenProject> projects = session.getProjects();
			project.addProjectReference(project);
		} catch (Exception e) {
			e.printStackTrace();
			new RuntimeException(e);
		}

	}
	// set up the reactor.

}
