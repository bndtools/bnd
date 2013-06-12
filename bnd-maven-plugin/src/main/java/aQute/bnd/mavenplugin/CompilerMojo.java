package aQute.bnd.mavenplugin;

import java.io.*;
import java.util.*;

import org.apache.maven.execution.*;
import org.apache.maven.model.*;
import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.*;
import org.codehaus.plexus.util.xml.*;

import aQute.bnd.build.*;
import aQute.bnd.osgi.*;

/**
 * This class is instantiated to setup the build path based on the bnd project
 * that resides in the same directory.
 * 
 */
@Mojo(name = "compile")
public class CompilerMojo extends AbstractMojo {
	private static final String ORG_APACHE_MAVEN_PLUGINS_MAVEN_COMPILER_PLUGIN = "org.apache.maven.plugins:maven-compiler-plugin";
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
		try {
			Workspace workspace = Utils.getWorkspace(new File(session
					.getExecutionRootDirectory()));
			Project p = workspace.getProject(project.getArtifactId());
			if (p == null || !p.isValid())
				throw new MojoExecutionException("Cannot find valid project "
						+ project.getArtifactId() + " in "
						+ workspace.getBase());

			Plugin plugin = project
					.getPlugin(ORG_APACHE_MAVEN_PLUGINS_MAVEN_COMPILER_PLUGIN);
			if (plugin == null) {
				getLog().error(
						"For some weird reason cannot find compiler plugin "
								+ ORG_APACHE_MAVEN_PLUGINS_MAVEN_COMPILER_PLUGIN);
			} else {
				for (PluginExecution pe : plugin.getExecutions()) {
					Xpp3Dom config = (Xpp3Dom) pe.getConfiguration();
					System.out.println("Compiler config before " + config);
					set(config, "source", p.getProperty("javac.source"));
					set(config, "target", p.getProperty("javac.target"));
					set(config, "optimize", p.getProperty("javac.optimize"));
					set(config,
							"debug",
							""
									+ Processor.isTrue(p.getProperty(
											"javac.debug", "" + true)));
					pe.setConfiguration(config);
					plugin.setConfiguration(config);

					System.out.println("Compiler config after " + config
							+ " for " + pe);
				}

			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

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

}