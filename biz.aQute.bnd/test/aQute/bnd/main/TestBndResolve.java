package aQute.bnd.main;

import org.junit.Test;

import aQute.bnd.main.testrules.WatchedFolder.FileStatus;

public class TestBndResolve extends TestBndMainBase {

	@Test
	public void testResolve() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("resolve", "resolve", "--write");

		expectNoError();

		expectFileStataus(FileStatus.MODIFIED, "p/workspace.bndrun");
		expectFileStataus(FileStatus.MODIFIED, "p2/workspace.bndrun");
		expectFileStataus(FileStatus.MODIFIED, "p3/workspace.bndrun");
		expectFileStataus(FileStatus.MODIFIED, "p3/workspace2.bndrun");
	}

	@Test
	public void testResolveP() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("resolve", "resolve", "--write", "-p", "p2");

		expectNoError();

		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, "p/workspace.bndrun");
		expectFileStataus(FileStatus.MODIFIED, "p2/workspace.bndrun");
		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, "p3/workspace.bndrun");
		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, "p3/workspace2.bndrun");

	}

	@Test
	public void testResolveWS() throws Exception {
		initTestDataAll();

		executeBndCmd("resolve", "resolve", "--write", "--workspace", WORKSPACE);
		expectNoError();

		expectFileStataus(FileStatus.MODIFIED, WORKSPACE + "/p/workspace.bndrun");
		expectFileStataus(FileStatus.MODIFIED, WORKSPACE + "/p2/workspace.bndrun");
		expectFileStataus(FileStatus.MODIFIED, WORKSPACE + "/p3/workspace.bndrun");
		expectFileStataus(FileStatus.MODIFIED, WORKSPACE + "/p3/workspace2.bndrun");

	}

	@Test
	public void testResolveWSp() throws Exception {
		initTestDataAll();

		executeBndCmd("resolve", "resolve", "--write", "--workspace", WORKSPACE, "--project", WORKSPACE + "/p3");

		expectNoError();

		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, WORKSPACE + "/p/workspace.bndrun");
		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, WORKSPACE + "/p2/workspace.bndrun");
		expectFileStataus(FileStatus.MODIFIED, WORKSPACE + "/p3/workspace.bndrun");
		expectFileStataus(FileStatus.MODIFIED, WORKSPACE + "/p3/workspace2.bndrun");

	}

	@Test
	public void testResolveIncl() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("resolve", "resolve", "--write", "--exclude", "**/*2.bndrun", "**/*.bndrun");

		expectNoError();

		expectFileStataus(FileStatus.MODIFIED, "p/workspace.bndrun");
		expectFileStataus(FileStatus.MODIFIED, "p2/workspace.bndrun");
		expectFileStataus(FileStatus.MODIFIED, "p3/workspace.bndrun");
		expectFileStataus(FileStatus.UNMODIFIED_EXISTS, "p3/workspace2.bndrun");

	}

}
