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

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Processes the test target classes to generate OSGi metadata.
 * <p>
 * This goal has the default phase of "process-test-classes".
 */
@Mojo(name = "bnd-process-tests", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class BndMavenTestsPlugin extends AbstractBndMavenPlugin {

	/**
	 * Whether the test artifact is an OSGi fragment.
	 * <p>
	 * If true, make the tests artifact a fragment using
	 * <code>$&#123;project.artifactId&#125;</code> as the {@code Fragment-Host}
	 * header and setting the {@code Bundle-SymbolicName} of the tests artifact
	 * to <code>$&#123;project.artifactId&#125;-tests</code>.
	 */
	@Parameter(defaultValue = "false")
	private boolean									artifactFragment;

	/**
	 * The test case types to generate into the {@code Test-Cases}
	 * manifest header.
	 * <p>
	 * Possible values are {@link TestCases#junit3 junit3},
	 * {@link TestCases#junit4 junit4}, {@link TestCases#junit5 junit5},
	 * {@link TestCases#all all}, {@link TestCases#testng testng}, and
	 * {@link TestCases#useTestCasesHeader}
	 */
	@Parameter(defaultValue = "junit5")
	private TestCases								testCases;

	@Parameter(defaultValue = "${project.build.testSourceDirectory}", readonly = true)
	private File									sourceDir;

	@Parameter(defaultValue = "${project.build.testResources}", readonly = true)
	private List<org.apache.maven.model.Resource>	resources;

	/**
	 * The directory where the {@code maven-compiler-plugin} placed the main output.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}")
	private File									mainClassesDir;

	/**
	 * The directory where the {@code maven-compiler-plugin} places its output.
	 */
	@Parameter(defaultValue = "${project.build.testOutputDirectory}")
	private File									classesDir;

	/**
	 * The directory where this plugin will store its output.
	 */
	@Parameter(defaultValue = "${project.build.testOutputDirectory}")
	private File									outputDir;

	/**
	 * Specify the path to store the generated manifest file.
	 */
	@Parameter(defaultValue = "${project.build.testOutputDirectory}/META-INF/MANIFEST.MF")
	private File									manifestPath;

	/**
	 * Skip this goal. The goal can also be skipped with the {@link #skipGoal}
	 * configuration.
	 */
	@Parameter(property = "maven.test.skip", defaultValue = "false")
	private boolean									skip;

	/**
	 * Skip this goal. The goal can also be skipped with the {@link #skip}
	 * configuration.
	 */
	@Parameter(property = "bnd-tests.skip", defaultValue = "false")
	private boolean									skipGoal;

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
		return skip || skipGoal;
	}

	@Override
	protected void processBuildPath(List<Object> buildpath) {
		// Add the main classes directory at the tip of the build path
		if (!isEmpty(mainClassesDir)) {
			buildpath.add(0, mainClassesDir);
		}
	}

	@Override
	protected void processBuilder(Builder builder) throws MojoFailureException {
		String defaultBsn = project.getArtifactId();
		String bsn = builder.getProperty(Constants.BUNDLE_SYMBOLICNAME, defaultBsn + "-tests");

		builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, bsn);

		if (artifactFragment) {
			String fragmentHost = builder.getProperty(Constants.FRAGMENT_HOST, defaultBsn);
			builder.setProperty(Constants.FRAGMENT_HOST, fragmentHost);
		}

		if (testCases != TestCases.useTestCasesHeader) {
			builder.setProperty(Constants.TESTCASES, "${sort;${uniq;" + testCases.filter() + "}}");
		} else if (builder.getUnexpandedProperty(Constants.TESTCASES) == null) {
			throw new MojoFailureException(
				"<testCases> specified " + TestCases.useTestCasesHeader + " but no Test-Cases header was found");
		}
	}

	@Override
	protected void reportErrorsAndWarnings(Builder builder) throws MojoFailureException {
		if (builder.getProperty(Constants.TESTCASES, "")
			.trim()
			.isEmpty()) {
			builder.warning("The Test-Cases header is empty. No test case classes were found.");
		}
		super.reportErrorsAndWarnings(builder);
	}

}
