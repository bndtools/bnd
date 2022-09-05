package aQute.bnd.maven.lib.configuration;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;

/**
 * A helper to read configurations for maven plugins consistently over the
 * various Mojos.
 *
 * @author Juergen Albert
 */
public class Configurations {

	protected final static Logger logger = LoggerFactory.getLogger(Configurations.class);

	public static File loadProperties(Processor processor, MavenProject project, MojoExecution mojoExecution)
		throws Exception {
		// Load parent project properties first
		loadParentProjectProperties(processor, project, mojoExecution);

		// Load current project properties
		Xpp3Dom configuration = Optional.ofNullable(project.getBuildPlugins())
			.flatMap(new FlatMapHelper(mojoExecution)::getConfiguration)
			.orElseGet(Configurations::defaultConfiguration);
		return loadProjectProperties(processor, project, project, configuration);
	}

	private static void loadParentProjectProperties(Processor builder, MavenProject currentProject,
		MojoExecution mojoExecution) throws Exception {
		MavenProject parentProject = currentProject.getParent();
		if (parentProject == null) {
			return;
		}
		loadParentProjectProperties(builder, parentProject, mojoExecution);

		// Get configuration from parent project
		Xpp3Dom configuration = Optional.ofNullable(parentProject.getBuildPlugins())
			.flatMap(new FlatMapHelper(mojoExecution)::getConfiguration)
			.orElse(null);
		if (configuration != null) {
			// Load parent project's properties
			loadProjectProperties(builder, parentProject, parentProject, configuration);
			return;
		}

		// Get configuration in project's pluginManagement
		configuration = Optional.ofNullable(currentProject.getPluginManagement())
			.map(PluginManagement::getPlugins)
			.flatMap(new FlatMapHelper(mojoExecution)::getConfiguration)
			.orElseGet(Configurations::defaultConfiguration);
		// Load properties from parent project's bnd file or configuration in
		// project's pluginManagement
		loadProjectProperties(builder, parentProject, currentProject, configuration);
	}

	private static File loadProjectProperties(Processor processor, MavenProject bndProject, MavenProject pomProject,
		Xpp3Dom configuration) throws Exception {
		// check for bnd file configuration
		File baseDir = bndProject.getBasedir();
		if (baseDir != null) { // file system based pom
			File pomFile = bndProject.getFile();
			processor.updateModified(pomFile.lastModified(), "POM: " + pomFile);
			// check for bnd file
			Xpp3Dom bndfileElement = configuration.getChild("bndfile");
			String bndFileName = (bndfileElement != null) ? bndfileElement.getValue() : Project.BNDFILE;
			File bndFile = IO.getFile(baseDir, bndFileName);
			if (bndFile.isFile()) {
				logger.debug("loading bnd properties from file: {}", bndFile);
				// we use setProperties to handle -include
				processor.setProperties(bndFile.getParentFile(), processor.loadProperties(bndFile));
				return bndFile;
			}
			// no bnd file found, so we fall through
		}

		// check for bnd-in-pom configuration
		baseDir = pomProject.getBasedir();
		File pomFile = pomProject.getFile();
		if (baseDir != null) {
			processor.updateModified(pomFile.lastModified(), "POM: " + pomFile);
		}
		Xpp3Dom bndElement = configuration.getChild("bnd");
		if (bndElement != null) {
			logger.debug("loading bnd properties from bnd element in pom: {}", pomProject);
			UTF8Properties properties = new UTF8Properties();
			properties.load(bndElement.getValue(), pomFile, processor);
			// we use setProperties to handle -include
			processor.setProperties(baseDir, properties.replaceHere(baseDir));
		}
		return pomFile;
	}

	private static class FlatMapHelper {

		private MojoExecution mojoExecution;

		public FlatMapHelper(MojoExecution mojoExecution) {
			this.mojoExecution = mojoExecution;
		}

		public Optional<Xpp3Dom> getConfiguration(List<Plugin> plugins) {
			return plugins.stream()
				.filter(p -> Objects.equals(p, mojoExecution.getPlugin()))
				.map(Plugin::getExecutions)
				.flatMap(List::stream)
				.filter(e -> Objects.equals(e.getId(), mojoExecution.getExecutionId()))
				.findFirst()
				.map(PluginExecution::getConfiguration)
				.map(Xpp3Dom.class::cast)
				.map(Xpp3Dom::new);
		}
	}

	private static Xpp3Dom defaultConfiguration() {
		return new Xpp3Dom("configuration");
	}

}
