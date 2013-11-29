package aQute.bnd.mavenplugin;


import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * This class is instantiated to setup the build path based on the bnd project
 * information that resides in the same directory.
 *
 */
@Mojo(name = "prepare")
public class ConfigureMavenProject extends AbstractMojo {
	private static final String ORG_APACHE_MAVEN_PLUGINS_MAVEN_COMPILER_PLUGIN = "org.apache.maven.plugins:maven-compiler-plugin";
	Set<Project> built = new HashSet<Project>();
	@Component
	private MavenSession session;

	@Component
	protected MavenProject project;

	@Component
	private BndWorkspace bndWorkspace;

	/**
	 * Called to setup this project.
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// Ensure this is an ordered set!
		Set<Artifact> classpath = new LinkedHashSet<Artifact>();

		try {
			Workspace workspace = bndWorkspace.getWorkspace(session);

			Project bndProject = workspace.getProject(project.getArtifactId());
			if ( bndProject == null) {
				bndProject = Workspace.getProject(project.getBasedir());
			}
			if (bndProject == null || !bndProject.isValid())
				throw new MojoExecutionException("Cannot find valid project "
						+ project.getArtifactId() + " in "
						+ workspace.getBase());

			// The bnd project might have some different ideas about
			// source and bin folders. So get them and set them.
			setBndDirsInMvnProject(bndProject, project);

            // We might want to change this to methods in bnd's Project.
			if ( project.getVersion() != null && !project.getVersion().isEmpty())
				project.setVersion( bndProject.getProperty(Constants.BUNDLE_VERSION, "0"));

            Build build = project.getBuild();
			build.setTestSourceDirectory(bndProject.getProperty("src.test",
					bndProject.getSrc().getAbsolutePath()));
			build.setTestOutputDirectory(bndProject.getProperty("bin.test",
					bndProject.getOutput().getAbsolutePath()));

			// Now we have to configure the compiler so that we have the same
			// options in Eclipse and maven

			Plugin plugin = project
					.getPlugin(ORG_APACHE_MAVEN_PLUGINS_MAVEN_COMPILER_PLUGIN);
			if (plugin == null) {
				// TODO understand why we must list the compiler plugin
				// in the project pom?
				getLog().error(
						"Cannot find compiler plugin "
								+ ORG_APACHE_MAVEN_PLUGINS_MAVEN_COMPILER_PLUGIN);
			} else {

				// Configuration is per PluginExecution, so configure all the
				// executions
				// TODO understand the life cycle ... have no clue how the
				// configuration
				// works. Seems impossible to override the pom configuration :-(

				for (PluginExecution pe : plugin.getExecutions()) {
					Xpp3Dom config = (Xpp3Dom) pe.getConfiguration();
					if (config == null)
						config = new Xpp3Dom("configuration");
					set(config, "source",
							bndProject.getProperty("javac.source"));
					set(config, "target",
							bndProject.getProperty("javac.target"));
					set(config, "optimize",
							bndProject.getProperty("javac.optimize"));
					set(config, "debugLevel",
							bndProject.getProperty("javac.debugLevel"));
					set(config, "encoding",
							bndProject.getProperty("javac.encoding", "UTF-8"));

					set(config,
							"debug",
							""
									+ Processor.isTrue(bndProject.getProperty(
											"javac.debug", "" + true)));
					set(config, "compilerVersion",
							bndProject.getProperty("javac.compilerVersion"));
					set(config, "compilerArgument",
							bndProject.getProperty("javac.compilerArgument"));
					getLog().debug(
							"[bnd] compiler configuration "
									+ config.toString().replace('\n', ' '));
					pe.setConfiguration(config);
				}

			}
			// Hmm, the compiler seems special ...
			for (File src : bndProject.getSourcePath())
				project.addCompileSourceRoot(src.getAbsolutePath());

			// Handle any source resources since the standard
			// java compiler does not copy them
			// TODO shoul do this in bnd some way since the ant
			// and gradle build also suffer from this

			copyResourcesFromSourcePath(bndProject);

			// Setup the classpath

			for (Container entry : bndProject.getBuildpath()) {
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

			getLog().info("[bnd] classpath " + classpath);
			project.setResolvedArtifacts(classpath);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * Utility to handle this XML tag thingy
	 *
	 * @param config
	 *            the xml object
	 * @param key
	 *            tkey we want to change
	 * @param value
	 *            the value
	 *
	 *            TODO handle nested keys?
	 */
	private void set(Xpp3Dom config, String key, String value) {
		if (config != null && value != null) {
			Xpp3Dom child = config.getChild(key);
			if (child == null) {
				child = new Xpp3Dom(key);
				config.addChild(child);
			}
			child.setValue(value);
			project.getProperties().setProperty("maven.compiler." + key, value);
		}
	}

	/**
	 * The ecj compiler copies resources from source directories but the
	 * standard Java compiler does not. So this method will copy the non-Java
	 * files.
	 *
	 * @param p the bnd project
	 * @throws Exception
	 */
	private void copyResourcesFromSourcePath(Project p) throws Exception {
		for (File src : p.getSourcePath())
			copyResources(src, p.getOutput());
	}

	/**
	 * Recursive routine to traverse the directory
	 *
	 * @param src the src directory
	 * @param output the output directory
	 * @throws IOException
	 */
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

	/**
	 * Transfer directory settings from the Bnd project to the Maven project.
	 * @param bndProject The bnd project that is the source of information.
	 * @param mavenProject The maven project that needs to be configured.
	 */
    static void setBndDirsInMvnProject(Project bndProject, MavenProject mavenProject) throws Exception {
        mavenProject.getBuild().setDirectory(bndProject.getTarget().getAbsolutePath());
        mavenProject.getBuild().setSourceDirectory(bndProject.getSrc().getAbsolutePath());
        mavenProject.getBuild().setOutputDirectory(bndProject.getOutput().getAbsolutePath());
        mavenProject.getBuild().setTestOutputDirectory(bndProject.getOutput().getAbsolutePath());
    }
}