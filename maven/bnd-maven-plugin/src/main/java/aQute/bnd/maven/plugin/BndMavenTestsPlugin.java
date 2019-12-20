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
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;

@Mojo(name = "bnd-process-tests", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class BndMavenTestsPlugin extends AbstractBndMavenPlugin {

	/**
	 * If true, make the tests artifact a fragment using
	 * <code>$&#123;project.artifactId&#125;</code> as the {@code Fragment-Host}
	 * header and setting the {@code Bundle-SymbolicName} of the tests artifact
	 * to <code>$&#123;project.artifactId&#125;-tests</code>.
	 */
	@Parameter(defaultValue = "false")
	private boolean									artifactFragment;

	/**
	 * Possible values are {@link TestCases#junit3 junit3},
	 * {@link TestCases#junit4 junit4}, {@link TestCases#junit5 junit5},
	 * {@link TestCases#all all} and {@link TestCases#useTestCasesHeader}
	 */
	@Parameter(defaultValue = "junit5")
	private TestCases								testCases;

	@Parameter(defaultValue = "${project.build.testSourceDirectory}", readonly = true)
	private File									sourceDir;

	@Parameter(defaultValue = "${project.build.testResources}", readonly = true)
	private List<org.apache.maven.model.Resource>	resources;

	@Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
	private File									mainClassesDir;

	@Parameter(defaultValue = "${project.build.testOutputDirectory}", readonly = true)
	private File									classesDir;

	@Parameter(defaultValue = "${project.build.testOutputDirectory}", readonly = true)
	private File									outputDir;

	@Parameter(defaultValue = "${project.build.testOutputDirectory}/META-INF/MANIFEST.MF", readonly = true)
	private File									manifestPath;

	@Parameter(property = "bnd-tests.skip", defaultValue = "false")
	private boolean									skip;

	@Override
	public File getSourceDir() {
		return sourceDir;
	}

	@Override
	public List<Resource> getResources() {
		return resources;
	}

	@Override
	public File getClassesDir() {
		return classesDir;
	}

	@Override
	public File getOutputDir() {
		return outputDir;
	}

	@Override
	public File getManifestPath() {
		return manifestPath;
	}

	@Override
	public boolean isSkip() {
		return skip;
	}

	@Override
	protected void processBuildPath(List<Object> buildpath) {
		// Add the main classes directory at the tip of the build path
		buildpath.add(0, mainClassesDir);
	}

	@Override
	protected void processBuilder(Builder builder) throws MojoFailureException {
		String defaultBsn = project.getArtifactId();
		if (artifactFragment) {
			builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, defaultBsn + "-tests");
			builder.setProperty(Constants.FRAGMENT_HOST, defaultBsn);
		} else if (builder.getProperty(Constants.BUNDLE_SYMBOLICNAME) == null) {
			builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, defaultBsn + "-tests");
		}

		if (testCases != TestCases.useTestCasesHeader) {
			builder.setProperty(Constants.TESTCASES, "${sort;${uniq;" + testCases.filter() + "}}");
		} else if (builder.getProperty(Constants.TESTCASES) == null) {
			throw new MojoFailureException(
				"<testCases> specified " + TestCases.useTestCasesHeader + " but no Test-Cases header was found");
		}
	}

}
