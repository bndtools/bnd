package aQute.bnd.mavenplugin;

import static aQute.bnd.mavenplugin.Utils.*;

import java.io.*;
import java.util.*;

import org.apache.maven.artifact.*;
import org.apache.maven.execution.*;
import org.apache.maven.model.*;
import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.*;

import aQute.bnd.build.*;
import aQute.lib.io.*;

/**
 * This class is instantiated to setup the build path based on the bnd project
 * that resides in the same directory.
 * 
 */
@Mojo(name = "prepare")
public class BuildpathhMojo extends AbstractMojo {
	Set<Project> built = new HashSet<Project>();
	@Component
	private MavenSession session;

	@Component
	protected MavenProject project;

	/**
	 * Called to setup this project.
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Set<Artifact> classpath = new LinkedHashSet<Artifact>();

		try {
			Workspace workspace = getWorkspace(new File(
					session.getExecutionRootDirectory()));
			Project p = workspace.getProject(project.getArtifactId());
			if (p == null || !p.isValid())
				throw new MojoExecutionException("Cannot find valid project "
						+ project.getArtifactId() + " in "
						+ workspace.getBase());

			// The bnd project might have some different ideas about
			// source and bin folders. So get them and set them.

			Build build = project.getBuild();
			build.setOutputDirectory(p.getOutput().getAbsolutePath());
			build.setTestOutputDirectory(p.getOutput().getAbsolutePath());

			// We might want to change this to methods in project.

			build.setTestSourceDirectory(p.getProperty("src.test", p.getSrc()
					.getAbsolutePath()));
			build.setTestOutputDirectory(p.getProperty("bin.test", p
					.getOutput().getAbsolutePath()));

			build.setDirectory(p.getTarget().getAbsolutePath());
			build.setSourceDirectory(p.getSrc().getAbsolutePath());

			// Hmm, the compiler seems special ...
			for (File src : p.getSourcePath())
				project.addCompileSourceRoot(src.getAbsolutePath());


			
			// Ensure we've build all our dependencies
			// before we compile ourselves
			for (Project dep : p.getDependson()) {
				if ( dep.isStale())
					getLog().error("Depends on " + dep
							+ " but this project is stale. Might be building in wrong order" );
				
				if (!dep.isOk())
					getLog().error("Depends on " + dep
							+ " but this project cannot be build: "
							+ dep.getErrors());
			}

			// Handle any source resources since the standard
			// java compiler does not copy them
			copyResources(p);
			
			for (Container entry : p.getBuildpath()) {
				if (entry.getError() != null) {
					getLog().error(
							"Erroneous dependency "
									+ entry.getBundleSymbolicName() + "-"
									+ entry.getVersion() + " = "
									+ entry.getError());
				} else {
					Artifact artifact = new BndArtifact(entry);
					classpath.add(artifact);
				}
			}

			getLog().info("Class path " + classpath);
			project.setResolvedArtifacts(classpath);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void copyResources(Project p) throws Exception {
		for (File src : p.getSourcePath())
			copyResources(src, p.getOutput());
	}

	//
	// The ecj compiler copies resources from source directories
	// but the standard Java compiler does not. So this method
	// will copy the non-Java files.
	//
	private void copyResources(File src, File output) throws IOException {
		if (src.isFile()) {
			if (!src.getName().endsWith(".java"))
				IO.copy(src, output);
		} else if (src.isDirectory()) {
			output.mkdirs();
			for (String sub : src.list()) {
				copyResources(new File(src, sub), new File(output, sub));
			}
		}
	}
}