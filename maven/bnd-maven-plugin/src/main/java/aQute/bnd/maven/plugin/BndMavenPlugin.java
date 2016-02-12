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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import aQute.bnd.build.Project;
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

@Mojo(name = "bnd-process", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class BndMavenPlugin extends AbstractMojo {

	private static final String	PACKAGING_POM	= "pom";
	private static final String	SNAPSHOT		= "SNAPSHOT";
	private static final String	TSTAMP			= "${tstamp}";

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File				targetDir;

	@Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
	private File				sourceDir;

	@Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
	private File				classesDir;

	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/MANIFEST.MF", readonly = true)
	private File				manifestPath;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject		project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings			settings;

	@Component
	private BuildContext		buildContext;

	public void execute() throws MojoExecutionException {
		Log log = getLog();

		// Exit without generating anything if this is a pom-packaging project.
		// Probably it's just a parent project.
		if (PACKAGING_POM.equals(project.getPackaging())) {
			log.info("skip project with packaging=pom");
			return;
		}

		Properties beanProperties = new BeanProperties();
		beanProperties.put("project", project);
		beanProperties.put("settings", settings);
		Properties mavenProperties = new Properties(beanProperties);
		mavenProperties.putAll(project.getProperties());

		try (Builder builder = new Builder(new Processor(mavenProperties, false))) {
			builder.setTrace(log.isDebugEnabled());

			builder.setBase(project.getBasedir());
			loadProjectProperties(builder, project);
			builder.setProperty("project.output", targetDir.getCanonicalPath());

			// incremental build only if any resource in the project outside of
			// the targetDir is changed. This implicitly includes POM and bnd files.
			if (buildContext.isIncremental()) {
				boolean hasChanges = false;

				// compute relative target path. While changes here should never
				// be reported, sometimes they are, e.g. due to (temporary)
				// misconfiguration
				String basePath = project.getBasedir().getCanonicalPath();
				String targetPath = targetDir.getCanonicalPath();
				int i = targetPath.indexOf(basePath);
				if (i != 0) {
					// target is not relative to basedir
					targetPath = null;
				} else {
					targetPath = targetPath.substring(basePath.length() + 1);
					if (!targetPath.endsWith(File.separator)) {
						targetPath = targetPath + File.separator;
					}
				}

				Scanner changeScanner = buildContext.newScanner(project.getBasedir(), true);
				changeScanner.scan();
				for (String sourceFile : changeScanner.getIncludedFiles()) {
					if (targetPath == null || !sourceFile.startsWith(targetPath)) {
						if (!buildContext.isUptodate(manifestPath, new File(project.getBasedir(), sourceFile))) {
							hasChanges = true;
							break;
						}
					}
				}
				
				if (!hasChanges) {
					// check for deleted resources
					Scanner deleteScanner = buildContext.newDeleteScanner(project.getBasedir());
					deleteScanner.scan();
					for (String sourceFile : deleteScanner.getIncludedFiles()) {
						if (targetPath == null || !sourceFile.startsWith(targetPath)) {
							hasChanges = true;
							break;
						}
					}
				}

				if (!hasChanges) {
					log.info("no incremental changes");
					return;
				}
			}

			// Reject sub-bundle projects
			List<Builder> subs = builder.getSubBuilders();
			if ((subs.size() != 1) || !builder.equals(subs.get(0))) {
				throw new MojoExecutionException("Sub-bundles not permitted in a maven build");
			}

			// Include local project packages automatically
			if (classesDir.isDirectory()) {
				Jar classesDirJar = new Jar(project.getName(), classesDir);
				classesDirJar.setManifest(new Manifest());
				builder.setJar(classesDirJar);
			}

			// Set bnd classpath
			Set<Artifact> artifacts = project.getArtifacts();
			List<File> buildpath = new ArrayList<File>(artifacts.size());
			for (Artifact artifact : artifacts) {
				if (!artifact.getType().equals("jar")) {
					continue;
				}
				buildpath.add(artifact.getFile().getCanonicalFile());
			}
			builder.setProperty("project.buildpath", Strings.join(File.pathSeparator, buildpath));
			builder.setClasspath(buildpath.toArray(new File[buildpath.size()]));
			if (log.isDebugEnabled()) {
				log.debug("builder classpath: " + builder.getProperty("project.buildpath"));
			}

			// Set bnd sourcepath
			List<File> sourcepath = new ArrayList<File>();
			if (sourceDir.exists()) {
				sourcepath.add(sourceDir.getCanonicalFile());
			}
			builder.setProperty("project.sourcepath", Strings.join(File.pathSeparator, sourcepath));
			builder.setSourcepath(sourcepath.toArray(new File[sourcepath.size()]));
			if (log.isDebugEnabled()) {
				log.debug("builder sourcepath: " + builder.getProperty("project.sourcepath"));
			}

			// Set Bundle-Version
			Version version = MavenVersion.parseString(project.getVersion()).getOSGiVersion();
			version = replaceSNAPSHOT(version);
			builder.setProperty(Constants.BUNDLE_VERSION, version.toString());

			if (log.isDebugEnabled()) {
				log.debug("builder properties: " + builder.getProperties());
			}

			// Build bnd Jar (in memory)
			Jar bndJar = builder.build();

			// Expand Jar into target/classes
			expandJar(bndJar, classesDir);

			// Finally, report
			reportErrorsAndWarnings(builder);

		} catch (Exception e) {
			throw new MojoExecutionException("bnd error", e);
		}
	}

	private void loadProjectProperties(Builder builder, MavenProject project) throws Exception {
		// Load parent project properties first
		MavenProject parentProject = project.getParent();
		if (parentProject != null) {
			loadProjectProperties(builder, parentProject);
		}

		// Merge in current project properties
		File baseDir = project.getBasedir();
		File bndFile = new File(baseDir, Project.BNDFILE);
		if (bndFile.isFile()) { // we use setProperties to handle -include
			builder.setProperties(baseDir, builder.loadProperties(bndFile));
		}
		File pomFile = project.getFile(); // pom files can affect dependencies
		if ((pomFile != null) && pomFile.isFile()) {
			builder.updateModified(pomFile.lastModified(), "POM: " + pomFile);
		}
	}

	private void reportErrorsAndWarnings(Builder builder) throws MojoExecutionException {
		Log log = getLog();

		List<String> warnings = builder.getWarnings();
		for (String warning : warnings) {
			log.warn(warning);
		}
		List<String> errors = builder.getErrors();
		for (String error : errors) {
			log.error(error);
		}
		if (!builder.isOk()) {
			if (errors.size() == 1)
				throw new MojoExecutionException(errors.get(0));
			else
				throw new MojoExecutionException("Errors in bnd processing, see log for details.");
		}
	}

	private void expandJar(Jar jar, File dir) throws Exception {
		final long lastModified = jar.lastModified();
		dir = dir.getAbsoluteFile();
		Files.createDirectories(dir.toPath());

		for (Map.Entry<String,Resource> entry : jar.getResources().entrySet()) {
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
				Files.createDirectories(outFile.toPath().getParent());
				try (OutputStream out = buildContext.newFileOutputStream(outFile)) {
					IO.copy(resource.openInputStream(), out);
				}
			}
		}

		if (!manifestPath.exists() || manifestPath.lastModified() < lastModified) {
			Files.createDirectories(manifestPath.toPath().getParent());
			try (OutputStream manifestOut = buildContext.newFileOutputStream(manifestPath)) {
				jar.writeManifest(manifestOut);
			}
		}
	}

	private Version replaceSNAPSHOT(Version version) {
		String qualifier = version.getQualifier();
		if (qualifier != null) {
			int i = qualifier.indexOf(SNAPSHOT);
			if (i >= 0) {
				qualifier = new StringBuilder().append(qualifier.substring(0, i))
						.append(TSTAMP)
						.append(qualifier.substring(i + SNAPSHOT.length()))
						.toString();
				version = new Version(version.getMajor(), version.getMinor(), version.getMicro(), qualifier);
			}
		}
		return version;
	}

	private class BeanProperties extends Properties {
		private static final long serialVersionUID = 1L;

		BeanProperties() {
			super();
		}

		@Override
		public String getProperty(String key) {
			final int i = key.indexOf('.');
			final String name = (i > 0) ? key.substring(0, i) : key;
			Object value = get(name);
			if ((value != null) && (i > 0)) {
				value = getField(value, key.substring(i + 1));
			}
			if (value == null) {
				return null;
			}
			return value.toString();
		}

		private Object getField(Object target, String key) {
			final int i = key.indexOf('.');
			final String fieldName = (i > 0) ? key.substring(0, i) : key;
			final String getterSuffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
			Object value = null;
			try {
				Class< ? > targetClass = target.getClass();
				while (!Modifier.isPublic(targetClass.getModifiers())) {
					targetClass = targetClass.getSuperclass();
				}
				Method getter;
				try {
					getter = targetClass.getMethod("get" + getterSuffix);
				} catch (NoSuchMethodException nsme) {
					getter = targetClass.getMethod("is" + getterSuffix);
				}
				value = getter.invoke(target);
			} catch (Exception e) {
				getLog().debug("Could not find getter method for field: " + fieldName, e);
			}
			if ((value != null) && (i > 0)) {
				value = getField(value, key.substring(i + 1));
			}
			return value;
		}
	}
}
