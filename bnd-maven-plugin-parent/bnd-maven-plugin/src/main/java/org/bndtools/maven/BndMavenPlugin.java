package org.bndtools.maven;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;

@Mojo(name = "bnd-process", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class BndMavenPlugin extends AbstractMojo {
	
	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File targetDir;
	
	@Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
	private File classesDir;
	
	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/MANIFEST.MF", readonly = true)
	private File manifestPath;
	
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;
	
	@Parameter(property = "bnd.trace", defaultValue = "false", readonly = true)
	private boolean trace;

	public void execute() throws MojoExecutionException {
		File bndFile = new File(project.getBasedir(), Project.BNDFILE);
		
		Builder builder = new Builder();
		builder.setTrace(trace);
		try {
			if (bndFile.isFile())
				builder.setProperties(bndFile);
			
			// Reject sub-bundle projects
			Collection<? extends Builder> subs = builder.getSubBuilders();
			if (subs.size() != 1)
				throw new MojoExecutionException("Sub-bundles not permitted in a maven build");
			File builderFile = builder.getPropertiesFile();
			if (builderFile != null && !bndFile.equals(builderFile))
				throw new MojoExecutionException("Sub-bundles not permitted in a maven build");

			// Set bnd classpath
			List<File> classpath = new LinkedList<File>();
			@SuppressWarnings("unchecked")
			Set<Artifact> artifacts = project.getArtifacts();
			for (Artifact artifact : artifacts) {
				File artifactFile = artifact.getFile();
				if (artifactFile != null)
					classpath.add(artifactFile);
			}
			classpath.add(classesDir);
			builder.setClasspath(classpath.toArray(new File[classpath.size()]));

			// Include local project packages automatically
			String includes = builder.getProperty(Constants.INCLUDERESOURCE);
			StringBuilder newIncludes = new StringBuilder().append(classesDir.getPath());
			if (includes == null || includes.trim().length() == 0)
				includes = newIncludes.toString();
			else
				includes = newIncludes.append(',').append(includes).toString();
			builder.setProperty(Constants.INCLUDERESOURCE, includes);

			// Set Bundle-Version
			MavenVersion mvnVersion = new MavenVersion(project.getVersion());
			builder.setProperty(Constants.BUNDLE_VERSION, mvnVersion.toBndVersion());

			// Build bnd Jar (in memory)
			Jar bndJar = builder.build();
			
			// Generate errors and warnings
			List<String> warnings = builder.getWarnings();
			if (warnings != null) for (String warning : warnings) {
				getLog().warn(warning);
			}
			List<String> errors = builder.getErrors();
			if (errors != null && !errors.isEmpty()) {
				for (String error : errors) {
					getLog().error(error);
				}

				if (errors.size() == 1)
					throw new MojoExecutionException(errors.get(0));
				else
					throw new MojoExecutionException("Errors in bnd processing, see log for details.");
			}

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

		} catch (Exception e) {
			throw new MojoExecutionException("bnd error", e);
		} finally {
			builder.close();
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

}
