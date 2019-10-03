package aQute.bnd.main;

import org.junit.Test;

import aQute.bnd.main.testrules.WatchedFolder.FileStatus;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;

public class TestBndMain extends TestBndMainBase {

	private static final String	WARNINGS_FRAME	= "-----------------" + System.lineSeparator() + "Warnings";
	private static final String	EMPTY_JAR_MSG	= "000: p: The JAR is empty: The instructions for the JAR named p did not cause any content to be included, this is likely wrong";

	@Test
	public void testRunStandalone() throws Exception {
		initTestData(STANDALONE);

		executeBndCmd("run", "standalone.bndrun");

		expectNoError(true);
		expectOutput("Gesundheit!");
	}

	@Test
	public void testRunWorkspace() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("run", "p/workspace.bndrun");

		expectNoError(true);
		expectOutput("Gesundheit!");
	}

	@Test
	public void testPackageBndrunStandalone() throws Exception {
		final String output = "generated/export-standalone.jar";

		initTestData(STANDALONE);

		executeBndCmd("package", "-o", output, "standalone.bndrun");

		expectNoError();

		expectFileStatus(FileStatus.CREATED, output);

		// validate exported jar content
		try (Jar result = new Jar(folder.getFile(output))) {
			expectJarEntry(result, "jar/biz.aQute.launcher-" + getVersion() + ".jar");
			expectJarEntry(result, "jar/org.apache.felix.framework-5.6.10.jar");
			expectJarEntry(result, "jar/printAndExit-1.0.0.jar");
		}
	}

	@Test
	public void testBrewFormulaExpectedOutput() throws Exception {
		executeBndCmd(IO.getPath("testdata/brew"), "resolve", "resolve", "-b", "launch.bndrun");
		/*
		 * If the expected output changes for this test, we will need to modify
		 * the brew formula for the bnd command to change the output asserts.
		 * See
		 * https://github.com/Homebrew/homebrew-core/blob/master/Formula/bnd.rb
		 */
		expectOutputContainsPattern(
			"BUNDLES\\s+org.apache.felix.gogo.runtime;version='\\[1.0.0,1.0.1\\)'");
		expectNoError();
	}

	@Test
	public void testPackageBndrunWorkspace() throws Exception {
		final String output = "generated/export-workspace.jar";

		initTestData(WORKSPACE);

		executeBndCmd("package", "-o", output, "p/workspace.bndrun");

		expectNoError();

		expectFileStatus(FileStatus.CREATED, output);

		// validate exported jar content
		try (Jar result = new Jar(folder.getFile(output))) {
			expectJarEntry(result, "jar/biz.aQute.launcher-" + getVersion() + ".jar");
			expectJarEntry(result, "jar/org.apache.felix.framework-5.6.10.jar");
			expectJarEntry(result, "jar/printAndExit-1.0.0.jar");
		}
	}

	@Test
	public void testPackageProject() throws Exception {
		final String output = "generated/export-workspace-project.jar";

		initTestData(WORKSPACE);

		executeBndCmd("-t", "package", "-o", output, "p2");
		expectNoError();

		expectFileStatus(FileStatus.CREATED, output);

		// validate exported jar content
		try (Jar result = new Jar(folder.getFile(output))) {
			expectJarEntry(result, "jar/biz.aQute.launcher-" + getVersion() + ".jar");
			expectJarEntry(result, "jar/org.apache.felix.framework-5.6.10.jar");
			expectJarEntry(result, "jar/p2.jar");
		}
	}

	@Test
	public void testClean() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("clean");

		expectNoError();

		expectFileStatus(FileStatus.DELETED, "p2/bin/somepackage/SomeOldClass.class");
		expectFileStatus(FileStatus.DELETED, "p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCleanP() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("clean", "-p", "p2");

		expectNoError();

		expectFileStatus(FileStatus.DELETED, "p2/bin/somepackage/SomeOldClass.class");
		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, "p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCleanWS() throws Exception {
		initTestDataAll();

		executeBndCmd("clean", "--workspace", WORKSPACE);

		expectNoError();

		expectFileStatus(FileStatus.DELETED, WORKSPACE + "/p2/bin/somepackage/SomeOldClass.class");
		expectFileStatus(FileStatus.DELETED, WORKSPACE + "/p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCleanWSp() throws Exception {
		initTestDataAll();

		executeBndCmd("clean", "--workspace", WORKSPACE, "--project", WORKSPACE + "/p2");

		expectNoError();

		expectFileStatus(FileStatus.DELETED, WORKSPACE + "/p2/bin/somepackage/SomeOldClass.class");
		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, WORKSPACE + "/p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCleanIncl() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("clean", "--exclude", "p3", "p*");

		expectNoError();

		expectFileStatus(FileStatus.DELETED, "p2/bin/somepackage/SomeOldClass.class");
		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, "p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testPar() throws Exception {
		initTestData(WORKSPACE);
		executeBndCmd("_par");

		expectNoError(false, EMPTY_JAR_MSG, WARNINGS_FRAME);

		expectFileStatus(FileStatus.CREATED, "p2/bin/somepackage/SomeClass.class");
		expectFileStatus(FileStatus.CREATED, "p3/bin/somepackage/SomeClass.class");
		expectFileStatus(FileStatus.CREATED, "p4/bin/req/RequireAnnotationOne.class");
		expectFileStatus(FileStatus.CREATED, "p/generated/p.jar");
		expectFileStatus(FileStatus.CREATED, "p2/generated/p2.jar");
		expectFileStatus(FileStatus.CREATED, "p3/generated/p3.jar");
		expectFileStatus(FileStatus.CREATED, "p4/generated/p4.jar");

	}

	@Test
	public void testCompile() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("compile");

		expectNoError();

		expectFileStatus(FileStatus.CREATED, "p2/bin/somepackage/SomeClass.class");
		expectFileStatus(FileStatus.CREATED, "p3/bin/somepackage/SomeClass.class");
		expectFileStatus(FileStatus.CREATED, "p4/bin/req/RequireAnnotationOne.class");
		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, "p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCompileP4MavenDeps() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("compile", "-p", "p4");

		expectNoError();

		expectFileStatus(FileStatus.CREATED, "p4/bin/req/RequireAnnotationOne.class");
	}

	@Test
	public void testCompileP() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("compile", "-p", "p2");

		expectNoError();

		expectFileStatus(FileStatus.CREATED, "p2/bin/somepackage/SomeClass.class");
		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, "p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCompileWS() throws Exception {
		initTestDataAll();

		executeBndCmd("compile", "--workspace", WORKSPACE);

		expectNoError();

		expectFileStatus(FileStatus.CREATED, WORKSPACE + "/p2/bin/somepackage/SomeClass.class");
		expectFileStatus(FileStatus.CREATED, WORKSPACE + "/p3/bin/somepackage/SomeClass.class");
		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, WORKSPACE + "/p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCompileWSp() throws Exception {
		initTestDataAll();

		executeBndCmd("compile", "--workspace", WORKSPACE, "--project", WORKSPACE + "/p2");

		expectNoError();

		expectFileStatus(FileStatus.CREATED, WORKSPACE + "/p2/bin/somepackage/SomeClass.class");
		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, WORKSPACE + "/p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCompileIncl() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("compile", "--exclude", "p3", "p*");

		expectNoError();

		expectFileStatus(FileStatus.CREATED, "p2/bin/somepackage/SomeClass.class");
		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, "p3/bin/somepackage/SomeOldClass.class");
	}

	// Build
	@Test
	public void testBuild() throws Exception {
		initTestData(WORKSPACE);
		executeBndCmd("clean");
		executeBndCmd("compile");
		executeBndCmd("build");

		expectNoError(false, EMPTY_JAR_MSG, WARNINGS_FRAME);

		expectFileStatus(FileStatus.CREATED, "p/generated/p.jar");
		expectFileStatus(FileStatus.CREATED, "p2/generated/p2.jar");
		expectFileStatus(FileStatus.CREATED, "p3/generated/p3.jar");
	}

	@Test
	public void testBuildP() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("clean");
		executeBndCmd("compile");
		executeBndCmd("build", "-p", "p2");

		expectNoError();

		expectFileStatus(FileStatus.UNMODIFIED_NOT_EXISTS, "p/generated/p.jar");
		expectFileStatus(FileStatus.CREATED, "p2/generated/p2.jar");
		expectFileStatus(FileStatus.UNMODIFIED_NOT_EXISTS, "p3/generated/p3.jar");
	}

	@Test
	public void testBuildWS() throws Exception {
		initTestDataAll();

		executeBndCmd("clean", "--workspace", WORKSPACE);
		executeBndCmd("compile", "--workspace", WORKSPACE);
		executeBndCmd("build", "--workspace", WORKSPACE);

		expectNoError(false, EMPTY_JAR_MSG, WARNINGS_FRAME);

		expectFileStatus(FileStatus.CREATED, WORKSPACE + "/p/generated/p.jar");
		expectFileStatus(FileStatus.CREATED, WORKSPACE + "/p2/generated/p2.jar");
		expectFileStatus(FileStatus.CREATED, WORKSPACE + "/p3/generated/p3.jar");
	}

	@Test
	public void testBuildWSp() throws Exception {
		initTestDataAll();

		executeBndCmd("clean", "--workspace", WORKSPACE);
		executeBndCmd("compile", "--workspace", WORKSPACE);
		executeBndCmd("build", "--workspace", WORKSPACE, "--project", WORKSPACE + "/p2");

		expectNoError();

		expectFileStatus(FileStatus.UNMODIFIED_NOT_EXISTS, WORKSPACE + "/p/generated/p.jar");
		expectFileStatus(FileStatus.CREATED, WORKSPACE + "/p2/generated/p2.jar");
		expectFileStatus(FileStatus.UNMODIFIED_NOT_EXISTS, WORKSPACE + "/p3/generated/p3.jar");
	}

	@Test
	public void testBuildIncl() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("clean");
		executeBndCmd("compile");
		executeBndCmd("build", "--exclude", "p", "p*");

		expectNoError();

		expectFileStatus(FileStatus.UNMODIFIED_NOT_EXISTS, "p/generated/p.jar");
		expectFileStatus(FileStatus.CREATED, "p2/generated/p2.jar");
		expectFileStatus(FileStatus.CREATED, "p3/generated/p3.jar");
	}
}
