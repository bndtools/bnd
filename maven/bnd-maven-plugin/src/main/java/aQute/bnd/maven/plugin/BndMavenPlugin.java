package aQute.bnd.maven.plugin;

/*
 * Copyright (c) Paremus and others (2015). All Rights Reserved.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

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

@Mojo(name = "bnd-process", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class BndMavenPlugin extends AbstractMojo {
	
	private static final String PACKAGING_POM = "pom";
	private static final String SNAPSHOT = "SNAPSHOT";
	private static final String TSTAMP = "${tstamp}";

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File targetDir;

	@Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
	private File sourceDir;

	@Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
	private File classesDir;
	
	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/MANIFEST.MF", readonly = true)
	private File manifestPath;
	
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;
	
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
		Properties mavenProperties = new Properties(beanProperties);
		mavenProperties.putAll(project.getProperties());

		Builder builder = new Builder(new Processor(mavenProperties, false));
		builder.setTrace(log.isDebugEnabled());
		
		File bndFile = new File(project.getBasedir(), Project.BNDFILE);
		try {
			builder.setBase(project.getBasedir());
			loadProjectProperties(builder, project);
			
			// Reject sub-bundle projects
			Collection<? extends Builder> subs = builder.getSubBuilders();
			if (subs.size() != 1)
				throw new MojoExecutionException("Sub-bundles not permitted in a maven build");
			File builderFile = builder.getPropertiesFile();
			if (builderFile != null && !bndFile.equals(builderFile))
				throw new MojoExecutionException("Sub-bundles not permitted in a maven build");

			// Set bnd classpath
			List<File> classpath = new LinkedList<File>();
			Set<Artifact> artifacts = project.getArtifacts();
			for (Artifact artifact : artifacts) {
				File artifactFile = artifact.getFile();
				if (artifactFile != null)
					classpath.add(artifactFile);
			}
			if (classesDir.isDirectory()) {
				classpath.add(classesDir);
			}
			builder.setClasspath(classpath.toArray(new File[classpath.size()]));
			
			// Set bnd sourcepath
			if (builder.hasSources() && sourceDir.isDirectory())
				builder.setSourcepath(new File[] { sourceDir });

			// Include local project packages automatically
			if (classesDir.isDirectory()) {
				String includes = builder.getProperty(Constants.INCLUDERESOURCE);
				StringBuilder newIncludes = new StringBuilder().append('"').append(classesDir.getAbsolutePath().replaceAll("\"", "\\\\\"")).append('"');
				if (includes == null || includes.trim().isEmpty())
					includes = newIncludes.toString();
				else
					includes = newIncludes.append(',').append(includes).toString();
				builder.setProperty(Constants.INCLUDERESOURCE, includes);
			}

			// Set Bundle-Version
			Version version = MavenVersion.parseString(project.getVersion()).getOSGiVersion();
			version = replaceSNAPSHOT(version);
			builder.setProperty(Constants.BUNDLE_VERSION, version.toString());

			// Build bnd Jar (in memory)
			Jar bndJar = builder.build();

			// Output manifest to <classes>/META-INF/MANIFEST.MF
			Files.createDirectories(manifestPath.toPath().getParent());
			FileOutputStream manifestOut = new FileOutputStream(manifestPath);
			try {
				bndJar.writeManifest(manifestOut);
			} finally {
				manifestOut.close();
			}

			// Expand Jar into target/classes
			expandJar(bndJar, classesDir);

			// Finally, report
			reportErrorsAndWarnings(builder);

		} catch (Exception e) {
			throw new MojoExecutionException("bnd error", e);
		} finally {
			IO.close(builder);
		}
	}

	private void loadProjectProperties(Builder builder, MavenProject project) {
		// Load parent project properties first
		MavenProject parentProject = project.getParent();
		if (parentProject != null) {
			loadProjectProperties(builder, parentProject);
		}
		
		// Merge in current project properties
		File bndFile = new File(project.getBasedir(), Project.BNDFILE);
		if (bndFile.isFile())
			builder.mergeProperties(bndFile, true);
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
	
	private static void expandJar(Jar jar, File dir) throws Exception {
		dir = dir.getAbsoluteFile();
		if (!dir.exists() && !dir.mkdirs()) {
			throw new IOException("Could not create directory " + dir);
		}
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("Not a dir: " + dir.getAbsolutePath());
		}

		for (Map.Entry<String,Resource> entry : jar.getResources().entrySet()) {
			File outFile = getFile(dir, entry.getKey());
			File outDir = outFile.getParentFile();
			if (!outDir.exists() && !outDir.mkdirs()) {
				throw new IOException("Could not create directory " + outDir);
			}

			// Skip the copy if the source and target file are the same
			Resource resource = entry.getValue();
			if (resource instanceof FileResource) {
				@SuppressWarnings("resource")
				FileResource fr = (FileResource) resource;
				if (outFile.equals(fr.getFile()))
					continue;
			}

			IO.copy(entry.getValue().openInputStream(), outFile);
		}
	}

	private Version replaceSNAPSHOT(Version version) {
		String qualifier = version.getQualifier();
		if (qualifier != null) {
			int i = qualifier.indexOf(SNAPSHOT);
			if (i >= 0) {
				qualifier = new StringBuilder()
					.append(qualifier.substring(0, i))
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
			Object value = null;
			try {
				final String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
				value = target.getClass().getMethod(getterName, (Class<?>) null).invoke(target, (Object[]) null);
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
