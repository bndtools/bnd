package aQute.bnd.main;

import org.junit.Test;

import aQute.bnd.main.testrules.WatchedFolder.FileStatus;
import aQute.bnd.osgi.Jar;

public class TestBndMain extends TestBndMainBase {

	@Test
	public void testRunStandalone() throws Exception {
		initTestData(STANDALONE);

		executeBndCmd("run", "standalone.bndrun");

		expectNoError();
		expectOutput("Gesundheit!");
	}

	@Test
	public void testRunWorkspace() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("run", "p/workspace.bndrun");

		expectNoError();
		expectOutput("Gesundheit!");
	}

	@Test
	public void testPackageBndrunStandalone() throws Exception {
		final String output = "generated/export-standalone.jar";

		initTestData(STANDALONE);

		executeBndCmd("package", "-o", output, "standalone.bndrun");

		expectNoError();

		expectFileStataus(FileStatus.CREATED, output);

		// validate exported jar content
		try (Jar result = new Jar(folder.getFile(output))) {
			expectJarEntry(result, "jar/biz.aQute.launcher-" + getVersion() + ".jar");
			expectJarEntry(result, "jar/org.apache.felix.framework-5.6.10.jar");
			expectJarEntry(result, "jar/printAndExit-1.0.0.jar");
		}
	}

	@Test
	public void testPackageBndrunWorkspace() throws Exception {
		final String output = "generated/export-workspace.jar";

		initTestData(WORKSPACE);

		executeBndCmd("package", "-o", output, "p/workspace.bndrun");

		expectNoError();

		expectFileStataus(FileStatus.CREATED, output);

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

		expectFileStataus(FileStatus.CREATED, output);

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

		expectFileStataus(FileStatus.DELETED, "p2/bin/somepackage/SomeOldClass.class");
		expectFileStataus(FileStatus.DELETED, "p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCleanP() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("clean", "-p", "p2");

		expectNoError();

		expectFileStataus(FileStatus.DELETED, "p2/bin/somepackage/SomeOldClass.class");
		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, "p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCleanWS() throws Exception {
		initTestDataAll();

		executeBndCmd("clean", "--workspace", WORKSPACE);

		expectNoError();

		expectFileStataus(FileStatus.DELETED, WORKSPACE + "/p2/bin/somepackage/SomeOldClass.class");
		expectFileStataus(FileStatus.DELETED, WORKSPACE + "/p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCleanWSp() throws Exception {
		initTestDataAll();

		executeBndCmd("clean", "--workspace", WORKSPACE, "--project", WORKSPACE + "/p2");

		expectNoError();

		expectFileStataus(FileStatus.DELETED, WORKSPACE + "/p2/bin/somepackage/SomeOldClass.class");
		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, WORKSPACE + "/p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCleanIncl() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("clean", "--exclude", "p3", "p*");

		expectNoError();

		expectFileStataus(FileStatus.DELETED, "p2/bin/somepackage/SomeOldClass.class");
		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, "p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCompile() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("compile");

		expectNoError();

		expectFileStataus(FileStatus.CREATED, "p2/bin/somepackage/SomeClass.class");
		expectFileStataus(FileStatus.CREATED, "p3/bin/somepackage/SomeClass.class");
		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, "p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCompileP() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("compile", "-p", "p2");

		expectNoError();

		expectFileStataus(FileStatus.CREATED, "p2/bin/somepackage/SomeClass.class");
		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, "p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCompileWS() throws Exception {
		initTestDataAll();

		executeBndCmd("compile", "--workspace", WORKSPACE);

		expectNoError();

		expectFileStataus(FileStatus.CREATED, WORKSPACE + "/p2/bin/somepackage/SomeClass.class");
		expectFileStataus(FileStatus.CREATED, WORKSPACE + "/p3/bin/somepackage/SomeClass.class");
		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, WORKSPACE + "/p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCompileWSp() throws Exception {
		initTestDataAll();

		executeBndCmd("compile", "--workspace", WORKSPACE, "--project", WORKSPACE + "/p2");

		expectNoError();

		expectFileStataus(FileStatus.CREATED, WORKSPACE + "/p2/bin/somepackage/SomeClass.class");
		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, WORKSPACE + "/p3/bin/somepackage/SomeOldClass.class");
	}

	@Test
	public void testCompileIncl() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("compile", "--exclude", "p3", "p*");

		expectNoError();

		expectFileStataus(FileStatus.CREATED, "p2/bin/somepackage/SomeClass.class");
		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, "p3/bin/somepackage/SomeOldClass.class");
	}

	// Build
	@Test
	public void testBuild() throws Exception {
		initTestData(WORKSPACE);
		executeBndCmd("clean");
		executeBndCmd("compile");
		executeBndCmd("build");

		capturedStdIO.getSystemErrContent()
			.contains(
			"000: p: The JAR is empty: The instructions for the JAR named p did not cause any content to be included, this is likely wrong");

		expectFileStataus(FileStatus.CREATED, "p/generated/p.jar");
		expectFileStataus(FileStatus.CREATED, "p2/generated/p2.jar");
		expectFileStataus(FileStatus.CREATED, "p3/generated/p3.jar");
	}

	@Test
	public void testBuildP() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("clean");
		executeBndCmd("compile");
		executeBndCmd("build", "-p", "p2");

		expectNoError();

		expectFileStataus(FileStatus.UNMODIFIED_NOT_EXISTS, "p/generated/p.jar");
		expectFileStataus(FileStatus.CREATED, "p2/generated/p2.jar");
		expectFileStataus(FileStatus.UNMODIFIED_NOT_EXISTS, "p3/generated/p3.jar");
	}

	@Test
	public void testBuildWS() throws Exception {
		initTestDataAll();

		executeBndCmd("clean", "--workspace", WORKSPACE);
		executeBndCmd("compile", "--workspace", WORKSPACE);
		executeBndCmd("build", "--workspace", WORKSPACE);

		capturedStdIO.getSystemErrContent()
			.contains(
				"000: p: The JAR is empty: The instructions for the JAR named p did not cause any content to be included, this is likely wrong");

		expectFileStataus(FileStatus.CREATED, WORKSPACE+"/p/generated/p.jar");
		expectFileStataus(FileStatus.CREATED, WORKSPACE+"/p2/generated/p2.jar");
		expectFileStataus(FileStatus.CREATED, WORKSPACE+"/p3/generated/p3.jar");
	}

	@Test
	public void testBuildWSp() throws Exception {
		initTestDataAll();

		executeBndCmd("clean","--workspace", WORKSPACE);
		executeBndCmd("compile", "--workspace", WORKSPACE);
		executeBndCmd("build", "--workspace", WORKSPACE, "--project", WORKSPACE + "/p2");

		expectNoError();

		expectFileStataus(FileStatus.UNMODIFIED_NOT_EXISTS, WORKSPACE + "/p/generated/p.jar");
		expectFileStataus(FileStatus.CREATED, WORKSPACE + "/p2/generated/p2.jar");
		expectFileStataus(FileStatus.UNMODIFIED_NOT_EXISTS, WORKSPACE + "/p3/generated/p3.jar");
	}

	@Test
	public void testBuildIncl() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("clean");
		executeBndCmd("compile");
		executeBndCmd("build", "--exclude", "p", "p*");

		expectNoError();

		expectFileStataus(FileStatus.UNMODIFIED_NOT_EXISTS, "p/generated/p.jar");
		expectFileStataus(FileStatus.CREATED, "p2/generated/p2.jar");
		expectFileStataus(FileStatus.CREATED, "p3/generated/p3.jar");
	}
}
