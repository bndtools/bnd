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

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
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
import aQute.bnd.osgi.Jar;
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
	
	@Parameter(defaultValue = "${project.build.directory}/bnd-tmp", readonly = true)
	private File tempDir;
	
	@Parameter(property = "bnd.trace", defaultValue = "false", readonly = true)
	private boolean trace;

	public void execute() throws MojoExecutionException {
		File bndFile = new File(project.getBasedir(), Project.BNDFILE);
		
		Builder builder = new Builder();
		builder.setTrace(true);
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
			if (includes == null || includes.trim().length() == 0)
				builder.setProperty(Constants.INCLUDERESOURCE, classesDir.getPath());
			
			// Set Bundle-Version
			MavenVersion mvnVersion = new MavenVersion(project.getVersion());
			builder.setProperty(Constants.BUNDLE_VERSION, mvnVersion.toBndVersion());

			// Build bnd Jar (in memory)
			Jar bndJar = builder.build();
			
			// Generate errors and warnings
			List<String> errors = builder.getErrors();
			if (errors != null) for (String error : errors) {
				getLog().error(error);
			}
			List<String> warnings = builder.getWarnings();
			if (warnings != null) for (String warning : warnings) {
				getLog().warn(warning);
			}
			
			// Output manifest to <classes>/META-INF/MANIFEST.MF
			Files.createDirectories(manifestPath.toPath().getParent());
			FileOutputStream manifestOut = new FileOutputStream(manifestPath);
			try {
				bndJar.writeManifest(manifestOut);
			} finally {
				manifestOut.close();
			}

			// Expand Jar into target/bnd-temp
			Files.createDirectories(tempDir.toPath());
			bndJar.expand(tempDir);
			
			// Copy from target/bnd-temp back to target/classes
			IO.copy(tempDir, classesDir);

		} catch (Exception e) {
			throw new MojoExecutionException("bnd error", e);
		} finally {
			builder.close();
		}
	}

}
