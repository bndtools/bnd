package aQute.bnd.main;

import org.junit.Test;

import aQute.bnd.main.testrules.WatchedFolder.FileStatus;

public class TestBndResolve extends TestBndMainBase {

	@Test
	public void testResolve() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("resolve", "resolve", "--write");

		expectNoError();

		expectFileStatus(FileStatus.MODIFIED, "p/workspace.bndrun");
		expectFileStatus(FileStatus.MODIFIED, "p2/workspace.bndrun");
		expectFileStatus(FileStatus.MODIFIED, "p3/workspace.bndrun");
		expectFileStatus(FileStatus.MODIFIED, "p3/workspace2.bndrun");
	}

	@Test
	public void testResolveP() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("resolve", "resolve", "--write", "-p", "p2");

		expectNoError();

		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, "p/workspace.bndrun");
		expectFileStatus(FileStatus.MODIFIED, "p2/workspace.bndrun");
		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, "p3/workspace.bndrun");
		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, "p3/workspace2.bndrun");

	}

	@Test
	public void testResolveWS() throws Exception {
		initTestDataAll();

		executeBndCmd("resolve", "resolve", "--write", "--workspace", WORKSPACE);
		expectNoError();

		expectFileStatus(FileStatus.MODIFIED, WORKSPACE + "/p/workspace.bndrun");
		expectFileStatus(FileStatus.MODIFIED, WORKSPACE + "/p2/workspace.bndrun");
		expectFileStatus(FileStatus.MODIFIED, WORKSPACE + "/p3/workspace.bndrun");
		expectFileStatus(FileStatus.MODIFIED, WORKSPACE + "/p3/workspace2.bndrun");

	}

	@Test
	public void testResolveWSp() throws Exception {
		initTestDataAll();

		executeBndCmd("resolve", "resolve", "--write", "--workspace", WORKSPACE, "--project", WORKSPACE + "/p3");

		expectNoError();

		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, WORKSPACE + "/p/workspace.bndrun");
		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, WORKSPACE + "/p2/workspace.bndrun");
		expectFileStatus(FileStatus.MODIFIED, WORKSPACE + "/p3/workspace.bndrun");
		expectFileStatus(FileStatus.MODIFIED, WORKSPACE + "/p3/workspace2.bndrun");

	}

	@Test
	public void testResolveIncl() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd("resolve", "resolve", "--write", "--exclude", "**/*2.bndrun", "**/*.bndrun");

		expectNoError();

		expectFileStatus(FileStatus.MODIFIED, "p/workspace.bndrun");
		expectFileStatus(FileStatus.MODIFIED, "p2/workspace.bndrun");
		expectFileStatus(FileStatus.MODIFIED, "p3/workspace.bndrun");
		expectFileStatus(FileStatus.UNMODIFIED_EXISTS, "p3/workspace2.bndrun");

	}

}
