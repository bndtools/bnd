package aQute.bnd.maven.lib.configuration;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;

/**
 * A helper to read Bnd configuration for maven plugins consistently over the
 * various Mojos.
 */
public class BndConfiguration {
	static final String			TSTAMP	= "${tstamp}";
	static final String			SNAPSHOT	= "SNAPSHOT";
	private final static Logger	logger	= LoggerFactory.getLogger(BndConfiguration.class);

	private final MavenProject	project;
	private final MojoExecution	mojoExecution;

	public BndConfiguration(MavenProject project, MojoExecution mojoExecution) {
		this.project = requireNonNull(project);
		this.mojoExecution = requireNonNull(mojoExecution);
	}

	public File loadProperties(Processor processor) throws Exception {
		// Load parent project properties first
		loadParentProjectProperties(processor, project);

		// Load current project properties
		Xpp3Dom configuration = Optional.ofNullable(project.getBuildPlugins())
			.flatMap(this::getConfiguration)
			.orElseGet(this::defaultConfiguration);
		return loadProjectProperties(processor, project, project, configuration);
	}

	public void inheritPropertiesDefaults(Processor processor) throws Exception {
		Xpp3Dom configuration = getConfiguration(project)
			.orElseGet(this::defaultConfiguration);
		// https://maven.apache.org/guides/mini/guide-reproducible-builds.html
		String outputTimestamp = Optional.ofNullable(configuration.getChild("outputTimestamp"))
			.map(xpp -> xpp.getValue())
			.orElse(project.getProperties()
				.getProperty("project.build.outputTimestamp"));
		boolean isReproducible = Strings.nonNullOrEmpty(outputTimestamp)
			// no timestamp configured (1 character configuration is useful
			// to override a full value during pom inheritance)
			&& ((outputTimestamp.length() > 1) || Character.isDigit(outputTimestamp.charAt(0)));
		if (isReproducible) {
			processor.setProperty(Constants.REPRODUCIBLE, outputTimestamp);
			if (processor.getProperty(Constants.NOEXTRAHEADERS) == null) {
				processor.setProperty(Constants.NOEXTRAHEADERS, Boolean.TRUE.toString());
			}
		}
		// Set Bundle-SymbolicName
		if (processor.getProperty(Constants.BUNDLE_SYMBOLICNAME) == null) {
			processor.setProperty(Constants.BUNDLE_SYMBOLICNAME, project.getArtifactId());
		}
		// Set Bundle-Name
		if (processor.getProperty(Constants.BUNDLE_NAME) == null) {
			processor.setProperty(Constants.BUNDLE_NAME, project.getName());
		}
		// Set Bundle-Version
		String snapshot = isReproducible ? SNAPSHOT : null;
		if (processor.getProperty(Constants.BUNDLE_VERSION) == null) {
			Version version = new MavenVersion(project.getVersion()).getOSGiVersion();
			processor.setProperty(Constants.BUNDLE_VERSION, version.toString());
			if (snapshot == null) {
				snapshot = TSTAMP;
			}
		}
		if (snapshot != null) {
			if (processor.getProperty(Constants.SNAPSHOT) == null) {
				processor.setProperty(Constants.SNAPSHOT, snapshot);
			}
		}

		// Set Bundle-Description
		if (processor.getProperty(Constants.BUNDLE_DESCRIPTION) == null) {
			// may be null
			if (StringUtils.isNotBlank(project.getDescription())) {
				processor.setProperty(Constants.BUNDLE_DESCRIPTION, project.getDescription());
			}
		}

		// Set Bundle-Vendor
		if (processor.getProperty(Constants.BUNDLE_VENDOR) == null) {
			if (project.getOrganization() != null && StringUtils.isNotBlank(project.getOrganization()
				.getName())) {
				processor.setProperty(Constants.BUNDLE_VENDOR, project.getOrganization()
					.getName());
			}
		}

		// Set Bundle-License
		if (processor.getProperty(Constants.BUNDLE_LICENSE) == null) {
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
				processor.setProperty(Constants.BUNDLE_LICENSE, licenses.toString());
			}
		}

		// Set Bundle-SCM
		if (processor.getProperty(Constants.BUNDLE_SCM) == null) {
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
					processor.setProperty(Constants.BUNDLE_SCM, scm.toString());
				}
			}
		}

		// Set Bundle-Developers
		if (processor.getProperty(Constants.BUNDLE_DEVELOPERS) == null) {
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
				processor.setProperty(Constants.BUNDLE_DEVELOPERS, developers.toString());
			}
		}

		// Set Bundle-DocURL
		if (processor.getProperty(Constants.BUNDLE_DOCURL) == null) {
			if (StringUtils.isNotBlank(project.getUrl())) {
				processor.setProperty(Constants.BUNDLE_DOCURL, project.getUrl());
			}
		}
	}

	private void loadParentProjectProperties(Processor builder, MavenProject currentProject) throws Exception {
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

	private File loadProjectProperties(Processor processor, MavenProject bndProject, MavenProject pomProject,
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

	private Optional<Xpp3Dom> getConfiguration(MavenProject mavenProject) {
		if (mavenProject == null) {
			return Optional.empty();
		}
		return getConfiguration(mavenProject.getBuildPlugins()).or(() -> getConfiguration(mavenProject.getParent()));
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
}
