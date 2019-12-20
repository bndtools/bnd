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
import java.util.stream.Collectors;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.mapping.MappingUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

import aQute.bnd.build.Project;
import aQute.bnd.header.OSGiHeader;
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

public abstract class AbstractBndMavenPlugin extends AbstractMojo {
	private static final Logger						logger					= LoggerFactory
		.getLogger(AbstractBndMavenPlugin.class);
	static final String						MANIFEST_LAST_MODIFIED	= "aQute.bnd.maven.plugin.BndMavenPlugin.manifestLastModified";
	static final String						MARKED_FILES			= "aQute.bnd.maven.plugin.BndMavenPlugin.markedFiles";
	static final String						PACKAGING_JAR			= "jar";
	static final String						PACKAGING_WAR			= "war";
	static final String						TSTAMP					= "${tstamp}";

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	File									targetDir;

	@Parameter(defaultValue = "true")
	boolean									includeClassesDir;

	@Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}")
	File									warOutputDir;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject							project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	Settings								settings;

	@Parameter(defaultValue = "${mojoExecution}", readonly = true)
	MojoExecution							mojoExecution;

	@Parameter(property = "bnd.packagingTypes", defaultValue = PACKAGING_JAR + "," + PACKAGING_WAR)
	List<String>							packagingTypes;

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
	String									bndfile;

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
	String									bnd;

	@Component
	BuildContext							buildContext;

	File									propertiesFile;

	public abstract File getSourceDir();

	public abstract List<org.apache.maven.model.Resource> getResources();

	public abstract File getClassesDir();

	public abstract File getOutputDir();

	public abstract File getManifestPath();

	public abstract boolean isSkip();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (isSkip()) {
			logger.debug("skip project as configured");
			return;
		}

		// Exit without generating anything if this is neither a jar or war
		// project. Probably it's just a parent project.
		if (!packagingTypes.contains(project.getPackaging())) {
			logger.info("skip project with packaging=" + project.getPackaging());
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

			// always add the outputDirectory to the classpath, but
			// handle projects with no output directory, like
			// 'test-wrapper-bundle'
			if (getClassesDir().isDirectory()) {
				builder.addClasspath(getClassesDir());

				Jar classesDirJar = new Jar(project.getName(), getClassesDir());
				if (!includeClassesDir) {
					classesDirJar.removePrefix(""); // clear the jar
				}
				classesDirJar.setManifest(new Manifest());
				builder.setJar(classesDirJar);
			}

			boolean isWab = PACKAGING_WAR.equals(project.getPackaging());
			boolean hasWablibs = builder.getProperty(Constants.WABLIB) != null;
			String wabProperty = builder.getProperty(Constants.WAB);

			File outputDir = getOutputDir();

			if (isWab) {
				if (wabProperty == null) {
					builder.setProperty(Constants.WAB, "");
				}
				outputDir = warOutputDir;
				logger
					.info("WAB mode enabled. Bnd output will be expanded into the 'maven-war-plugin' <webappDirectory>:"
						+ outputDir);
			} else if ((wabProperty != null) || hasWablibs) {
				throw new MojoFailureException(
					Constants.WAB + " & " + Constants.WABLIB + " are not supported with packaging 'jar'");
			}

			// Compute bnd classpath
			Set<Artifact> artifacts = project.getArtifacts();
			List<Object> buildpath = new ArrayList<>(artifacts.size());
			List<String> wablibs = new ArrayList<>(artifacts.size());
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

					if (isWab && !hasWablibs && !Artifact.SCOPE_PROVIDED.equals(artifact.getScope())
						&& !Artifact.SCOPE_TEST.equals(artifact.getScope()) && !artifact.isOptional()) {

						String fileNameMapping = MappingUtils
							.evaluateFileNameMapping(MappingUtils.DEFAULT_FILE_NAME_MAPPING, artifact);
						wablibs.add("WEB-INF/lib/" + fileNameMapping + "=" + cpe.getName() + ";lib:=true");
					}
				}
			}

			if (!wablibs.isEmpty()) {
				String wablib = wablibs.stream()
					.collect(Collectors.joining(","));
				builder.setProperty(Constants.WABLIB, wablib);
			}

			processBuildPath(buildpath);

			builder.setProperty("project.buildpath", Strings.join(File.pathSeparator, buildpath));
			logger.debug("builder classpath: {}", builder.getProperty("project.buildpath"));

			// Compute bnd sourcepath
			boolean delta = !buildContext.isIncremental() || manifestOutOfDate();
			List<File> sourcepath = new ArrayList<>();
			if (getSourceDir().exists()) {
				sourcepath.add(getSourceDir().getCanonicalFile());
				delta |= buildContext.hasDelta(getSourceDir());
			}
			for (org.apache.maven.model.Resource resource : getResources()) {
				File resourceDir = new File(resource.getDirectory());
				if (resourceDir.exists()) {
					sourcepath.add(resourceDir.getCanonicalFile());
					delta |= buildContext.hasDelta(resourceDir);
				}
			}
			builder.setProperty("project.sourcepath", Strings.join(File.pathSeparator, sourcepath));
			logger.debug("builder sourcepath: {}", builder.getProperty("project.sourcepath"));

			processBuilder(builder);

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
				Version version = new MavenVersion(project.getVersion()).getOSGiVersion();
				builder.setProperty(Constants.BUNDLE_VERSION, version.toString());
				if (builder.getProperty(Constants.SNAPSHOT) == null) {
					builder.setProperty(Constants.SNAPSHOT, TSTAMP);
				}
			}

			// Set Bundle-Description
			if (builder.getProperty(Constants.BUNDLE_DESCRIPTION) == null) {
				// may be null
				if (StringUtils.isNotBlank(project.getDescription())) {
					builder.setProperty(Constants.BUNDLE_DESCRIPTION, project.getDescription());
				}
			}

			// Set Bundle-Vendor
			if (builder.getProperty(Constants.BUNDLE_VENDOR) == null) {
				if (project.getOrganization() != null && StringUtils.isNotBlank(project.getOrganization()
					.getName())) {
					builder.setProperty(Constants.BUNDLE_VENDOR, project.getOrganization()
						.getName());
				}
			}

			// Set Bundle-License
			if (builder.getProperty(Constants.BUNDLE_LICENSE) == null) {
				StringBuilder licenses = new StringBuilder();
				for (License license : project.getLicenses()) {
					addHeaderValue(licenses, license.getName(), ',');
					// link is optional
					if (StringUtils.isNotBlank(license.getUrl())) {
						addHeaderAttribute(licenses, "link", license.getUrl(), ';');
					}
					// comment is optional
					if (StringUtils.isNotBlank(license.getComments())) {
						addHeaderAttribute(licenses, "description", license.getComments(), ';');
					}
				}
				if (licenses.length() > 0) {
					builder.setProperty(Constants.BUNDLE_LICENSE, licenses.toString());
				}
			}

			// Set Bundle-SCM
			if (builder.getProperty(Constants.BUNDLE_SCM) == null) {
				StringBuilder scm = new StringBuilder();
				if (project.getScm() != null) {
					if (StringUtils.isNotBlank(project.getScm()
						.getUrl())) {
						addHeaderAttribute(scm, "url", project.getScm()
							.getUrl(), ',');
					}
					if (StringUtils.isNotBlank(project.getScm()
						.getConnection())) {
						addHeaderAttribute(scm, "connection", project.getScm()
							.getConnection(), ',');
					}
					if (StringUtils.isNotBlank(project.getScm()
						.getDeveloperConnection())) {
						addHeaderAttribute(scm, "developer-connection", project.getScm()
							.getDeveloperConnection(), ',');
					}
					if (StringUtils.isNotBlank(project.getScm()
						.getTag())) {
						addHeaderAttribute(scm, "tag", project.getScm()
							.getTag(), ',');
					}
					if (scm.length() > 0) {
						builder.setProperty(Constants.BUNDLE_SCM, scm.toString());
					}
				}
			}

			// Set Bundle-Developers
			if (builder.getProperty(Constants.BUNDLE_DEVELOPERS) == null) {
				StringBuilder developers = new StringBuilder();
				// this is never null
				for (Developer developer : project.getDevelopers()) {
					// id is mandatory for OSGi but not enforced in the pom.xml
					if (StringUtils.isNotBlank(developer.getId())) {
						addHeaderValue(developers, developer.getId(), ',');
						// all attributes are optional
						if (StringUtils.isNotBlank(developer.getEmail())) {
							addHeaderAttribute(developers, "email", developer.getEmail(), ';');
						}
						if (StringUtils.isNotBlank(developer.getName())) {
							addHeaderAttribute(developers, "name", developer.getName(), ';');
						}
						if (StringUtils.isNotBlank(developer.getOrganization())) {
							addHeaderAttribute(developers, "organization", developer.getOrganization(), ';');
						}
						if (StringUtils.isNotBlank(developer.getOrganizationUrl())) {
							addHeaderAttribute(developers, "organizationUrl", developer.getOrganizationUrl(), ';');
						}
						if (!developer.getRoles()
							.isEmpty()) {
							addHeaderAttribute(developers, "roles", StringUtils.join(developer.getRoles()
								.iterator(), ","), ';');
						}
						if (StringUtils.isNotBlank(developer.getTimezone())) {
							addHeaderAttribute(developers, "timezone", developer.getTimezone(), ';');
						}
					} else {
						logger.warn(
							"Cannot consider developer in line '{}' of file '{}' for bundle header '{}' as it does not contain the mandatory id.",
							developer.getLocation("")
								.getLineNumber(),
							developer.getLocation("")
								.getSource()
								.getLocation(),
							Constants.BUNDLE_DEVELOPERS);
					}
				}
				if (developers.length() > 0) {
					builder.setProperty(Constants.BUNDLE_DEVELOPERS, developers.toString());
				}
			}

			// Set Bundle-DocURL
			if (builder.getProperty(Constants.BUNDLE_DOCURL) == null) {
				if (StringUtils.isNotBlank(project.getUrl())) {
					builder.setProperty(Constants.BUNDLE_DOCURL, project.getUrl());
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
				expandJar(bndJar, outputDir);
			} else {
				logger.debug("No build");
			}

			// Finally, report
			reportErrorsAndWarnings(builder);
		} catch (MojoExecutionException | MojoFailureException e) {
			throw e;
		} catch (Exception e) {
			throw new MojoExecutionException("bnd error: " + e.getMessage(), e);
		}
	}

	/**
	 * If a mojo needs to tweak the builder for any particular reason, do it
	 * here.
	 *
	 * @param builder the Builder created to analyze the jar contents
	 * @throws MojoFailureException if an issue is encountered
	 */
	protected void processBuilder(Builder builder) throws MojoFailureException {}

	/**
	 * If a mojo needs to update the buildpath for any particular reason, do it
	 * here.
	 *
	 * @param buildpath the set of jars and class directories used while
	 *            analyzing the jar contents
	 */
	protected void processBuildPath(List<Object> buildpath) {}

	private static StringBuilder addHeaderValue(StringBuilder builder, String value, char separator) {
		if (builder.length() > 0) {
			builder.append(separator);
		}
		// use quoted string if necessary
		OSGiHeader.quote(builder, value);
		return builder;
	}

	private static StringBuilder addHeaderAttribute(StringBuilder builder, String key, String value, char separator) {
		if (builder.length() > 0) {
			builder.append(separator);
		}
		builder.append(key)
			.append("=");
		// use quoted string if necessary
		OSGiHeader.quote(builder, value);
		return builder;
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

	private void reportErrorsAndWarnings(Builder builder) throws MojoFailureException {
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
				throw new MojoFailureException(errors.get(0));
			else
				throw new MojoFailureException("Errors in bnd processing, see log for details.");
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
			File outFile = IO.getBasedFile(dir, entry.getKey());
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

		if (manifestOutOfDate() || getManifestPath().lastModified() < lastModified) {
			if (logger.isDebugEnabled()) {
				if (!manifestOutOfDate())
					logger.debug(String.format("Updating lastModified: %tF %<tT.%<tL '%s'",
						getManifestPath().lastModified(), getManifestPath()));
				else
					logger.debug("Creating '{}'", getManifestPath());
			}
			Files.createDirectories(getManifestPath().toPath()
				.getParent());
			try (OutputStream manifestOut = buildContext.newFileOutputStream(getManifestPath())) {
				jar.writeManifest(manifestOut);
			}
			buildContext.setValue(MANIFEST_LAST_MODIFIED, getManifestPath().lastModified());
		}
	}

	private boolean manifestOutOfDate() {
		if (!getManifestPath().isFile()) {
			return true;
		}

		long manifestLastModified = 0L;
		if (buildContext.getValue(MANIFEST_LAST_MODIFIED) != null) {
			manifestLastModified = (Long) buildContext.getValue(MANIFEST_LAST_MODIFIED);
		}
		return getManifestPath().lastModified() != manifestLastModified;
	}
}
