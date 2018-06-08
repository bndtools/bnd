package aQute.bnd.maven.plugin;

/*
 * Copyright (c) Paremus and others (2015, 2016). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static aQute.lib.io.IO.getFile;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

import aQute.bnd.build.Project;
import aQute.bnd.maven.lib.configuration.BeanProperties;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.service.reporter.Report.Location;

@Mojo(name = "bnd-process", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class BndMavenPlugin extends AbstractMojo {
	private static final Logger						logger					= LoggerFactory
		.getLogger(BndMavenPlugin.class);
	private static final String						MANIFEST_LAST_MODIFIED	= "aQute.bnd.maven.plugin.BndMavenPlugin.manifestLastModified";
	private static final String						MARKED_FILES			= "aQute.bnd.maven.plugin.BndMavenPlugin.markedFiles";
	private static final String						PACKAGING_POM			= "pom";
	private static final String						TSTAMP					= "${tstamp}";

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File									targetDir;

	@Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
	private File									sourceDir;

	@Parameter(defaultValue = "${project.build.resources}", readonly = true)
	private List<org.apache.maven.model.Resource>	resources;

	@Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
	private File									classesDir;

	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/MANIFEST.MF")
	private File									manifestPath;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject							project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings								settings;

	@Parameter(defaultValue = "${mojoExecution}", readonly = true)
	private MojoExecution							mojoExecution;

	@Parameter(property = "bnd.skip", defaultValue = "false")
	private boolean									skip;

	/**
	 * File path to a bnd file containing bnd instructions for this project.
	 * Defaults to {@code bnd.bnd}. The file path can be an absolute or relative
	 * to the project directory.
	 * <p>
	 * The bnd instructions for this project are merged with the bnd
	 * instructions, if any, for the parent project.
	 */
	@Parameter(defaultValue = Project.BNDFILE)
	// This is not used and is for doc only; see loadProjectProperties
	@SuppressWarnings("unused")
	private String									bndfile;

	/**
	 * Bnd instructions for this project specified directly in the pom file.
	 * This is generally be done using a {@code <![CDATA[]]>} section. If the
	 * projects has a {@link #bndfile bnd file}, then this configuration element
	 * is ignored.
	 * <p>
	 * The bnd instructions for this project are merged with the bnd
	 * instructions, if any, for the parent project.
	 */
	@Parameter
	// This is not used and is for doc only; see loadProjectProperties
	@SuppressWarnings("unused")
	private String									bnd;

	@Component
	private BuildContext							buildContext;

	private File									propertiesFile;

	@Override
	public void execute() throws MojoExecutionException {
		if (skip) {
			logger.debug("skip project as configured");
			return;
		}

		// Exit without generating anything if this is a pom-packaging project.
		// Probably it's just a parent project.
		if (PACKAGING_POM.equals(project.getPackaging())) {
			logger.info("skip project with packaging=pom");
			return;
		}

		Properties beanProperties = new BeanProperties();
		beanProperties.put("project", project);
		beanProperties.put("settings", settings);
		Properties mavenProperties = new Properties(beanProperties);
		mavenProperties.putAll(project.getProperties());

		try (Builder builder = new Builder(new Processor(mavenProperties, false))) {
			builder.setTrace(logger.isDebugEnabled());

			builder.setBase(project.getBasedir());
			propertiesFile = loadProperties(builder);
			builder.setProperty("project.output", targetDir.getCanonicalPath());

			// If no bundle to be built, we have nothing to do
			if (Processor.isTrue(builder.getProperty(Constants.NOBUNDLES))) {
				logger.debug(Constants.NOBUNDLES + ": true");
				return;
			}

			// Reject sub-bundle projects
			List<Builder> subs = builder.getSubBuilders();
			if ((subs.size() != 1) || !builder.equals(subs.get(0))) {
				throw new MojoExecutionException("Sub-bundles not permitted in a maven build");
			}

			// Reject wab projects
			if (builder.getProperty(Constants.WAB) != null) {
				throw new MojoExecutionException(Constants.WAB + " not supported in a maven build");
			}
			if (builder.getProperty(Constants.WABLIB) != null) {
				throw new MojoExecutionException(Constants.WABLIB + " not supported in a maven build");
			}

			// Include local project packages automatically
			if (classesDir.isDirectory()) {
				Jar classesDirJar = new Jar(project.getName(), classesDir);
				classesDirJar.setManifest(new Manifest());
				builder.setJar(classesDirJar);
			}

			// Compute bnd classpath
			Set<Artifact> artifacts = project.getArtifacts();
			List<Object> buildpath = new ArrayList<Object>(artifacts.size());
			for (Artifact artifact : artifacts) {
				File cpe = artifact.getFile()
					.getCanonicalFile();
				if (!cpe.exists()) {
					logger.debug("dependency {} does not exist", cpe);
					continue;
				}
				if (cpe.isDirectory()) {
					Jar cpeJar = new Jar(cpe);
					builder.addClose(cpeJar);
					builder.updateModified(cpeJar.lastModified(), cpe.getPath());
					buildpath.add(cpeJar);
				} else {
					if (!artifact.getType()
						.equals("jar")) {
						/*
						 * Check if it is a valid zip file. We don't create a
						 * Jar object here because we want to avoid the cost of
						 * creating the Jar object if we decide not to build.
						 */
						try (ZipFile zip = new ZipFile(cpe)) {
							zip.entries();
						} catch (ZipException e) {
							logger.debug("dependency {} is not a zip", cpe);
							continue;
						}
					}
					builder.updateModified(cpe.lastModified(), cpe.getPath());
					buildpath.add(cpe);
				}
			}
			builder.setProperty("project.buildpath", Strings.join(File.pathSeparator, buildpath));
			logger.debug("builder classpath: {}", builder.getProperty("project.buildpath"));

			// Compute bnd sourcepath
			boolean delta = !buildContext.isIncremental() || manifestOutOfDate();
			List<File> sourcepath = new ArrayList<File>();
			if (sourceDir.exists()) {
				sourcepath.add(sourceDir.getCanonicalFile());
				delta |= buildContext.hasDelta(sourceDir);
			}
			for (org.apache.maven.model.Resource resource : resources) {
				File resourceDir = new File(resource.getDirectory());
				if (resourceDir.exists()) {
					sourcepath.add(resourceDir.getCanonicalFile());
					delta |= buildContext.hasDelta(resourceDir);
				}
			}
			builder.setProperty("project.sourcepath", Strings.join(File.pathSeparator, sourcepath));
			logger.debug("builder sourcepath: {}", builder.getProperty("project.sourcepath"));

			// Set Bundle-SymbolicName
			if (builder.getProperty(Constants.BUNDLE_SYMBOLICNAME) == null) {
				builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, project.getArtifactId());
			}
			// Set Bundle-Name
			if (builder.getProperty(Constants.BUNDLE_NAME) == null) {
				builder.setProperty(Constants.BUNDLE_NAME, project.getName());
			}
			// Set Bundle-Version
			if (builder.getProperty(Constants.BUNDLE_VERSION) == null) {
				Version version = MavenVersion.parseString(project.getVersion())
					.getOSGiVersion();
				builder.setProperty(Constants.BUNDLE_VERSION, version.toString());
				if (builder.getProperty(Constants.SNAPSHOT) == null) {
					builder.setProperty(Constants.SNAPSHOT, TSTAMP);
				}
			}

			logger.debug("builder properties: {}", builder.getProperties());
			logger.debug("builder delta: {}", delta);

			if (delta || (builder.getJar() == null) || (builder.lastModified() > builder.getJar()
				.lastModified())) {
				// Set builder paths
				builder.setClasspath(buildpath);
				builder.setSourcepath(sourcepath.toArray(new File[0]));

				// Build bnd Jar (in memory)
				Jar bndJar = builder.build();

				// Expand Jar into target/classes
				expandJar(bndJar, classesDir);
			} else {
				logger.debug("No build");
			}

			// Finally, report
			reportErrorsAndWarnings(builder);
		} catch (MojoExecutionException e) {
			throw e;
		} catch (Exception e) {
			throw new MojoExecutionException("bnd error: " + e.getMessage(), e);
		}
	}

	private File loadProperties(Builder builder) throws Exception {
		// Load parent project properties first
		loadParentProjectProperties(builder, project);

		// Load current project properties
		Xpp3Dom configuration = Optional.ofNullable(project.getBuildPlugins())
			.flatMap(this::getConfiguration)
			.orElseGet(this::defaultConfiguration);
		return loadProjectProperties(builder, project, project, configuration);
	}

	private void loadParentProjectProperties(Builder builder, MavenProject currentProject) throws Exception {
		MavenProject parentProject = currentProject.getParent();
		if (parentProject == null) {
			return;
		}
		loadParentProjectProperties(builder, parentProject);

		// Get configuration from parent project
		Xpp3Dom configuration = Optional.ofNullable(parentProject.getBuildPlugins())
			.flatMap(this::getConfiguration)
			.orElse(null);
		if (configuration != null) {
			// Load parent project's properties
			loadProjectProperties(builder, parentProject, parentProject, configuration);
			return;
		}

		// Get configuration in project's pluginManagement
		configuration = Optional.ofNullable(currentProject.getPluginManagement())
			.map(PluginManagement::getPlugins)
			.flatMap(this::getConfiguration)
			.orElseGet(this::defaultConfiguration);
		// Load properties from parent project's bnd file or configuration in
		// project's pluginManagement
		loadProjectProperties(builder, parentProject, currentProject, configuration);
	}

	private File loadProjectProperties(Builder builder, MavenProject bndProject, MavenProject pomProject,
		Xpp3Dom configuration) throws Exception {
		// check for bnd file configuration
		File baseDir = bndProject.getBasedir();
		if (baseDir != null) { // file system based pom
			File pomFile = bndProject.getFile();
			builder.updateModified(pomFile.lastModified(), "POM: " + pomFile);
			// check for bnd file
			Xpp3Dom bndfileElement = configuration.getChild("bndfile");
			String bndFileName = (bndfileElement != null) ? bndfileElement.getValue() : Project.BNDFILE;
			File bndFile = IO.getFile(baseDir, bndFileName);
			if (bndFile.isFile()) {
				logger.debug("loading bnd properties from file: {}", bndFile);
				// we use setProperties to handle -include
				builder.setProperties(bndFile.getParentFile(), builder.loadProperties(bndFile));
				return bndFile;
			}
			// no bnd file found, so we fall through
		}

		// check for bnd-in-pom configuration
		baseDir = pomProject.getBasedir();
		File pomFile = pomProject.getFile();
		if (baseDir != null) {
			builder.updateModified(pomFile.lastModified(), "POM: " + pomFile);
		}
		Xpp3Dom bndElement = configuration.getChild("bnd");
		if (bndElement != null) {
			logger.debug("loading bnd properties from bnd element in pom: {}", pomProject);
			UTF8Properties properties = new UTF8Properties();
			properties.load(bndElement.getValue(), pomFile, builder);
			// we use setProperties to handle -include
			builder.setProperties(baseDir, properties.replaceHere(baseDir));
		}
		return pomFile;
	}

	private Optional<Xpp3Dom> getConfiguration(List<Plugin> plugins) {
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

	private Xpp3Dom defaultConfiguration() {
		return new Xpp3Dom("configuration");
	}

	private void reportErrorsAndWarnings(Builder builder) throws MojoExecutionException {
		@SuppressWarnings("unchecked")
		Collection<File> markedFiles = (Collection<File>) buildContext.getValue(MARKED_FILES);
		if (markedFiles == null) {
			buildContext.removeMessages(propertiesFile);
			markedFiles = builder.getIncluded();
		}
		if (markedFiles != null) {
			for (File f : markedFiles) {
				buildContext.removeMessages(f);
			}
		}
		markedFiles = new HashSet<>();

		List<String> warnings = builder.getWarnings();
		for (String warning : warnings) {
			Location location = builder.getLocation(warning);
			if (location == null) {
				location = new Location();
				location.message = warning;
			}
			File f = location.file == null ? propertiesFile : new File(location.file);
			markedFiles.add(f);
			buildContext.addMessage(f, location.line, location.length, location.message, BuildContext.SEVERITY_WARNING,
				null);
		}
		List<String> errors = builder.getErrors();
		for (String error : errors) {
			Location location = builder.getLocation(error);
			if (location == null) {
				location = new Location();
				location.message = error;
			}
			File f = location.file == null ? propertiesFile : new File(location.file);
			markedFiles.add(f);
			buildContext.addMessage(f, location.line, location.length, location.message, BuildContext.SEVERITY_ERROR,
				null);
		}
		buildContext.setValue(MARKED_FILES, markedFiles);
		if (!builder.isOk()) {
			if (errors.size() == 1)
				throw new MojoExecutionException(errors.get(0));
			else
				throw new MojoExecutionException("Errors in bnd processing, see log for details.");
		}
	}

	private void expandJar(Jar jar, File dir) throws Exception {
		final long lastModified = jar.lastModified();
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Bundle lastModified: %tF %<tT.%<tL", lastModified));
		}
		dir = dir.getAbsoluteFile();
		Files.createDirectories(dir.toPath());

		for (Map.Entry<String, Resource> entry : jar.getResources()
			.entrySet()) {
			File outFile = getFile(dir, entry.getKey());
			Resource resource = entry.getValue();
			// Skip the copy if the source and target are the same file
			if (resource instanceof FileResource) {
				@SuppressWarnings("resource")
				FileResource fr = (FileResource) resource;
				if (outFile.equals(fr.getFile())) {
					continue;
				}
			}
			if (!outFile.exists() || outFile.lastModified() < lastModified) {
				if (logger.isDebugEnabled()) {
					if (outFile.exists())
						logger.debug(String.format("Updating lastModified: %tF %<tT.%<tL '%s'", outFile.lastModified(),
							outFile));
					else
						logger.debug("Creating '{}'", outFile);
				}
				Files.createDirectories(outFile.toPath()
					.getParent());
				try (OutputStream out = buildContext.newFileOutputStream(outFile)) {
					IO.copy(resource.openInputStream(), out);
				}
			}
		}

		if (manifestOutOfDate() || manifestPath.lastModified() < lastModified) {
			if (logger.isDebugEnabled()) {
				if (!manifestOutOfDate())
					logger.debug(String.format("Updating lastModified: %tF %<tT.%<tL '%s'", manifestPath.lastModified(),
						manifestPath));
				else
					logger.debug("Creating '{}'", manifestPath);
			}
			Files.createDirectories(manifestPath.toPath()
				.getParent());
			try (OutputStream manifestOut = buildContext.newFileOutputStream(manifestPath)) {
				jar.writeManifest(manifestOut);
			}
			buildContext.setValue(MANIFEST_LAST_MODIFIED, manifestPath.lastModified());
		}
	}

	private boolean manifestOutOfDate() {
		if (!manifestPath.isFile()) {
			return true;
		}

		long manifestLastModified = 0L;
		if (buildContext.getValue(MANIFEST_LAST_MODIFIED) != null) {
			manifestLastModified = (Long) buildContext.getValue(MANIFEST_LAST_MODIFIED);
		}
		return manifestPath.lastModified() != manifestLastModified;
	}
}
