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

package aQute.bnd.maven.plugin;

import static aQute.bnd.maven.lib.executions.PluginExecutions.isPackagingGoal;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
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
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.mapping.MappingUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

import aQute.bnd.build.Project;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.maven.PomPropertiesResource;
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

/**
 * Abstract base class for all bnd-maven-plugin mojos.
 */
public abstract class AbstractBndMavenPlugin extends AbstractMojo {
	protected final Logger	logger					= LoggerFactory.getLogger(getClass());
	static final String     LAST_MODIFIED           = "aQute.bnd.maven.plugin.BndMavenPlugin.lastModified";
	static final String		MARKED_FILES			= "aQute.bnd.maven.plugin.BndMavenPlugin.markedFiles";
	static final String		PACKAGING_JAR			= "jar";
	static final String		PACKAGING_WAR			= "war";
	static final String		TSTAMP					= "${tstamp}";
	static final String		SNAPSHOT				= "SNAPSHOT";

	/**
	 * Whether to include the contents of the {@code classesDir} directory
	 * in the generated bundle.
	 */
	@Parameter(defaultValue = "true")
	boolean					includeClassesDir;

	/**
	 * The directory where the webapp is built when packaging is {@code war}.
	 */
	@Parameter(alias = "warOutputDir", defaultValue = "${project.build.directory}/${project.build.finalName}")
	File                    webappDirectory;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject			project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	Settings				settings;

	@Parameter(defaultValue = "${mojoExecution}", readonly = true)
	MojoExecution			mojoExecution;

	/**
	 * The list of maven packaging types for which the plugin will execute.
	 */
	@Parameter(property = "bnd.packagingTypes", defaultValue = PACKAGING_JAR + "," + PACKAGING_WAR)
	List<String>			packagingTypes;

	/**
	 * Skip processing if {@link #includeClassesDir} is {@code true} and the
	 * {@code classesDir} directory is empty.
	 */
	@Parameter(property = "bnd.skipIfEmpty", defaultValue = "false")
	boolean					skipIfEmpty;

	/**
	 * Timestamp for reproducible output archive entries, either formatted as ISO 8601
	 * {@code yyyy-MM-dd'T'HH:mm:ssXXX} or as an int representing seconds since the epoch.
	 *
	 * @see <a href="https://maven.apache.org/guides/mini/guide-reproducible-builds.html">Configuring
	 * for Reproducible Builds</a>
	 */
	@Parameter(defaultValue = "${project.build.outputTimestamp}")
	String			outputTimestamp;

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
	String					bndfile;

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
	String					bnd;

	/**
	 * This is similar to the release parameter of the maven-compiler plugin and
	 * enables the processing of classes and resources for the given release,
	 * the default value is 0 that is using the default release (pre Java 9)
	 */
	@Parameter(defaultValue = "0")
	int						release;

	@Component
	BuildContext			buildContext;

	@Component
	MavenProjectHelper		projectHelper;

	@Component
	ArtifactHandlerManager artifactHandlerManager;

	File					propertiesFile;

	public abstract File getSourceDir();

	public abstract List<org.apache.maven.model.Resource> getResources();

	public abstract File getClassesDir();

	public abstract File getOutputDir();

	public abstract File getManifestPath();

	public abstract boolean isSkip();

	public Optional<String> getClassifier() {
		return Optional.empty();
	}

	public Optional<String> getType() {
		return Optional.empty();
	}

	File getWebappDirectory() {
		return webappDirectory;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// Exit without generating anything if the project packaging is not a
		// packaging type. Probably it's just a parent project.
		if (!packagingTypes.contains(project.getPackaging())) {
			logger.debug("skip project with packaging=" + project.getPackaging());
			return;
		}

		if (isSkip()) {
			logger.info("skip project as configured");
			return;
		}

		File classesDir = getClassesDir();

		if (skipIfEmpty && includeClassesDir && isEmpty(classesDir)) {
			logger.info(
				"skip project because includeClassesDir=true, compiler output directory is empty and skipIfEmpty=true");
			return;
		}

		Properties beanProperties = new BeanProperties();
		beanProperties.put("project", project);
		beanProperties.put("settings", settings);
		Properties mavenProperties = new Properties(beanProperties);
		Properties projectProperties = project.getProperties();
		for (Enumeration<?> propertyNames = projectProperties.propertyNames(); propertyNames.hasMoreElements();) {
			Object key = propertyNames.nextElement();
			mavenProperties.put(key, projectProperties.get(key));
		}

		try (Builder builder = new Builder(new Processor(mavenProperties, false))) {
			builder.setRelease(release);
			builder.setTrace(logger.isDebugEnabled());

			builder.setBase(project.getBasedir());
			propertiesFile = loadProperties(builder);
			builder.setProperty("project.output", getClassesDir().getCanonicalPath());

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
			if (classesDir.isDirectory()) {
				builder.addClasspath(classesDir);

				Jar classesDirJar;
				if (includeClassesDir) {
					classesDirJar = new Jar(project.getName(), classesDir);
				} else {
					classesDirJar = new Jar(project.getName()); // empty jar
				}
				classesDirJar.setManifest(new Manifest());
				builder.setJar(classesDirJar);
			}

			boolean isWab = PACKAGING_WAR.equals(project.getPackaging());
			boolean hasWablibs = builder.getProperty(Constants.WABLIB) != null;
			String wabProperty = builder.getProperty(Constants.WAB);

			if (isWab) {
				if (wabProperty == null) {
					builder.setProperty(Constants.WAB, "");
				}
				logger
					.info("WAB mode enabled. Bnd output will be expanded into the 'maven-war-plugin' <webappDirectory>:"
						+ getWebappDirectory());
			} else if ((wabProperty != null) || hasWablibs) {
				throw new MojoFailureException(
					Constants.WAB + " & " + Constants.WABLIB + " are not supported with packaging 'jar'");
			}

			// Compute bnd classpath
			Set<Artifact> artifacts = project.getArtifacts();
			List<Object> buildpath = new ArrayList<>(artifacts.size());
			List<String> wablibs = new ArrayList<>(artifacts.size());
			final ScopeArtifactFilter scopeFilter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
			for (Artifact artifact : artifacts) {
				File cpe = artifact.getFile()
					.getCanonicalFile();
				if (!cpe.exists()) {
					logger.debug("dependency {} does not exist", cpe);
					continue;
				}
				if (cpe.isDirectory()) {
					Jar cpeJar = new Jar(cpe);
					cpeJar.setSource(new File(cpe.getParentFile(), createArtifactName(artifact)));
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

					if (isWab && !hasWablibs && !artifact.isOptional() && scopeFilter.include(artifact)) {
						String fileNameMapping = MappingUtils
							.evaluateFileNameMapping(MappingUtils.DEFAULT_FILE_NAME_MAPPING, artifact);
						wablibs.add("WEB-INF/lib/" + fileNameMapping + "=" + cpe.getName() + ";lib:=true");
					}
				}
			}

			if (!wablibs.isEmpty()) {
				String wablib = String.join(",", wablibs);
				builder.setProperty(Constants.WABLIB, wablib);
			}

			processBuildPath(buildpath);

			builder.setProperty("project.buildpath", Strings.join(File.pathSeparator, buildpath));
			logger.debug("builder classpath: {}", builder.getProperty("project.buildpath"));

			// Compute bnd sourcepath
			boolean delta = !buildContext.isIncremental() || outOfDate();
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

			// https://maven.apache.org/guides/mini/guide-reproducible-builds.html
			boolean isReproducible = Strings.nonNullOrEmpty(outputTimestamp)
				// no timestamp configured (1 character configuration is useful
				// to override a full value during pom inheritance)
				&& ((outputTimestamp.length() > 1) || Character.isDigit(outputTimestamp.charAt(0)));
			if (isReproducible) {
				builder.setProperty(Constants.REPRODUCIBLE, outputTimestamp);
				if (builder.getProperty(Constants.NOEXTRAHEADERS) == null) {
					builder.setProperty(Constants.NOEXTRAHEADERS, Boolean.TRUE.toString());
				}
			}

			// Set Bundle-SymbolicName
			if (builder.getProperty(Constants.BUNDLE_SYMBOLICNAME) == null) {
				builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, project.getArtifactId());
			}
			// Set Bundle-Name
			if (builder.getProperty(Constants.BUNDLE_NAME) == null) {
				builder.setProperty(Constants.BUNDLE_NAME, project.getName());
			}
			// Set Bundle-Version
			String snapshot = isReproducible ? SNAPSHOT : null;
			if (builder.getProperty(Constants.BUNDLE_VERSION) == null) {
				Version version = new MavenVersion(project.getVersion()).getOSGiVersion();
				builder.setProperty(Constants.BUNDLE_VERSION, version.toString());
				if (snapshot == null) {
					snapshot = TSTAMP;
				}
			}
			if (snapshot != null) {
				if (builder.getProperty(Constants.SNAPSHOT) == null) {
					builder.setProperty(Constants.SNAPSHOT, snapshot);
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

				// If a bnd-maven-plugin packaging goal is used it means
				// this execution is responsible for creating and attaching the
				// target artifact to the project
				String goal = mojoExecution.getMojoDescriptor()
					.getGoal();
				if (isPackagingGoal(goal)) {
					// However, if extensions for this plugin are not enabled
					// the maven-(jar|war)-plugin will also execute, resulting in
					// conflicting results
					if (!mojoExecution.getPlugin()
						.isExtensions()) {
						throw new MojoExecutionException(String.format(
							"In order to use the bnd-maven-plugin packaging goal %s, <extensions>true</extensions> must be set on the plugin",
							goal));
					}
					if (isWab) {
						// Write Jar into webappDirectory
						File outputDirectory = getWebappDirectory();
						writeContent(bndJar, outputDirectory);
						File manifestPath = new File(outputDirectory, bndJar.getManifestName());
						writeManifest(bndJar, manifestPath);
					}
					// Add META-INF/maven metadata to jar
					addMavenMetadataToJar(bndJar);
					// Write the jar directly and attach it to the project
					attachArtifactToProject(bndJar);
				} else {
					File outputDirectory = isWab ? getWebappDirectory() : getOutputDir();
					// Write Jar content into outputDirectory
					writeContent(bndJar, outputDirectory);
					writeManifest(bndJar, getManifestPath());
				}
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

	private void attachArtifactToProject(Jar bndJar) throws Exception {
		File artifactFile = getArtifactFile();
		if (outOfDate(artifactFile) || artifactFile.lastModified() < bndJar.lastModified()) {
			if (logger.isDebugEnabled()) {
				if (artifactFile.exists())
					logger.debug(String.format("Updating lastModified: %tF %<tT.%<tL '%s'", artifactFile.lastModified(),
						artifactFile));
				else
					logger.debug("Creating '{}'", artifactFile);
			}

			Files.createDirectories(artifactFile.toPath()
				.getParent());
			try (OutputStream os = buildContext.newFileOutputStream(artifactFile)) {
				bndJar.write(os);
			}
			buildContext.setValue(LAST_MODIFIED, artifactFile.lastModified());
		}

		// If there is a classifier artifact must be attached to the
		// project using that classifier
		Optional<String> classifier = getClassifier();
		if (classifier.isPresent()) {
			projectHelper.attachArtifact(project, getType().orElse(""), classifier.get(), artifactFile);
		} else {
			// If there is no classifier, then this artifact is the main
			// artifact

			Artifact artifact = project.getArtifact();
			if (Optional.ofNullable(artifact.getFile())
				.map(File::isFile)
				.orElse(Boolean.FALSE)) {

				logger.warn("The main artifact on {} was already set. It will be replaced by {}", project,
					mojoExecution);
			}
			artifact.setFile(artifactFile);
		}
	}

	private void addMavenMetadataToJar(Jar bndJar) throws IOException {
		String groupId = project.getGroupId();
		String artifactId = project.getArtifactId();
		String version = project.getArtifact()
			.isSnapshot()
				? project.getArtifact()
					.getVersion()
				: project.getVersion();

		bndJar.putResource(String.format("META-INF/maven/%s/%s/pom.xml", groupId, artifactId),
			new FileResource(project.getFile()));
		PomPropertiesResource pomProperties = new PomPropertiesResource(groupId, artifactId, version);
		bndJar.putResource(pomProperties.getWhere(), pomProperties);
	}

	private File getArtifactFile() {
		return new File(getOutputDir(), project.getBuild()
			.getFinalName()
			+ getClassifier().map("-"::concat)
				.orElse("")
			+ "." + getExtension(project.getPackaging()));
	}

	private String getExtension(String type) {
		ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler(type);
		if (artifactHandler != null) {
			type = artifactHandler.getExtension();
		}
		return type;
	}

	private String createArtifactName(Artifact artifact) {
		String classifier = artifact.getClassifier();
		if ((classifier == null) || classifier.isEmpty()) {
			return String.format("%s-%s.%s", artifact.getArtifactId(), artifact.getVersion(), artifact.getType());
		}
		return String.format("%s-%s-%s.%s", artifact.getArtifactId(), artifact.getVersion(), classifier,
			artifact.getType());
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

	protected void reportErrorsAndWarnings(Builder builder) throws MojoFailureException {
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

	protected boolean isEmpty(File directory) {
		if (directory == null || !directory.isDirectory()) {
			return true;
		}

		Path path = directory.toPath();
		Path meta_inf = Paths.get("META-INF");
		try (Stream<Path> entries = Files.walk(path)) {
			return entries.allMatch(p -> Files.isDirectory(p) || path.relativize(p)
				.startsWith(meta_inf));
		} catch (IOException ioe) {
			throw Exceptions.duck(ioe);
		}
	}

	private void writeContent(Jar jar, File directory) throws Exception {
		final long lastModified = jar.lastModified();
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Bundle lastModified: %tF %<tT.%<tL", lastModified));
		}
		directory = directory.getAbsoluteFile();
		Files.createDirectories(directory.toPath());

		for (Map.Entry<String, Resource> entry : jar.getResources()
			.entrySet()) {
			File outFile = IO.getBasedFile(directory, entry.getKey());
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
	}

	private void writeManifest(Jar jar, File manifestPath) throws Exception {
		final long lastModified = jar.lastModified();
		if (outOfDate(manifestPath) || manifestPath.lastModified() < lastModified) {
			if (logger.isDebugEnabled()) {
				if (!outOfDate(manifestPath))
					logger.debug(String.format("Updating lastModified: %tF %<tT.%<tL '%s'",
						manifestPath.lastModified(), manifestPath));
				else
					logger.debug("Creating '{}'", manifestPath);
			}
			Files.createDirectories(manifestPath.toPath()
				.getParent());
			try (OutputStream manifestOut = buildContext.newFileOutputStream(manifestPath)) {
				jar.writeManifest(manifestOut);
			}
			buildContext.setValue(LAST_MODIFIED, manifestPath.lastModified());
		}
	}

	private boolean outOfDate() {
		String goal = mojoExecution.getMojoDescriptor()
			.getGoal();
		return outOfDate(isPackagingGoal(goal) ? getArtifactFile() : getManifestPath());
	}

	private boolean outOfDate(File target) {
		if (!target.isFile()) {
			return true;
		}

		long lastModified = 0L;
		if (buildContext.getValue(LAST_MODIFIED) != null) {
			lastModified = (Long) buildContext.getValue(LAST_MODIFIED);
		}
		return target.lastModified() != lastModified;
	}
}
