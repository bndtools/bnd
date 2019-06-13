package aQute.bnd.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import aQute.bnd.main.testrules.WatchedFolder.FileStatus;

public class TestBndIndex extends TestBndMainBase {

	private static final String	DEFAULT_INDEX_FILE_COMPRESSED	= IndexCommand.DEFAULT_INDEX_FILE
		+ IndexCommand.COMPRESSED_FILE_EXTENSION;

	private static final String	NON_DEFAULT_INDEX_FILE			= "other_" + IndexCommand.DEFAULT_INDEX_FILE;

	private static final String	REPONAME						= "TEST_REPOSITORY_NAME";

	@Test
	public void testIndexDefaultClassic() throws Exception {
		initTestData(BUNDLES);

		final String repoRootDir = folder.getRootPath()
			.toString();

		executeBndCmd("index", "-d", repoRootDir, "-v", "com.liferay.item.selector.taglib.jar",
			"biz.aQute.bndlib/biz.aQute.bndlib-3.5.0.jar");

		expectNoError();

		expectFileStatus(FileStatus.CREATED, IndexCommand.DEFAULT_INDEX_FILE);

		final long count = getFileContent(IndexCommand.DEFAULT_INDEX_FILE).stream()
			.filter(line -> line.contains("<resource>"))
			// .peek(this::print)
			.count();

		assertEquals(count, 2L);

		print(getFileContent(IndexCommand.DEFAULT_INDEX_FILE) + "");
	}

	@Test
	public void testIndexDefault() throws Exception {
		initTestData(BUNDLES);

		final String repoRootDir = folder.getRootPath()
			.toString();

		print(repoRootDir);

		executeBndCmd("index", "-d", repoRootDir, "-v", "**/*.jar");

		expectNoError();

		expectFileStatus(FileStatus.CREATED, IndexCommand.DEFAULT_INDEX_FILE);

		final long count = getFileContent(IndexCommand.DEFAULT_INDEX_FILE).stream()
			.filter(line -> line.contains("<resource>"))
			// .peek(this::print)
			.count();

		assertEquals(8L, count);
	}

	@Test
	public void testIndexNonDefault() throws Exception {
		initTestData(BUNDLES);

		final String repoRootDir = folder.getRootPath()
			.toString();

		final String repoIndexFile = Paths.get(repoRootDir, NON_DEFAULT_INDEX_FILE)
			.toString();

		executeBndCmd("index", "-d", repoRootDir, "-r", repoIndexFile, "**/*.jar");

		expectNoError();

		expectFileStatus(FileStatus.CREATED, NON_DEFAULT_INDEX_FILE);

		// folder.print(getSystemOut(), true);
	}

	@Test
	public void testIndexDefaultGZ() throws Exception {
		initTestData(BUNDLES);

		final String repoRootDir = folder.getRootPath()
			.toString();

		final String repoIndexFile = Paths.get(repoRootDir, DEFAULT_INDEX_FILE_COMPRESSED)
			.toString();

		executeBndCmd("index", "-d", repoRootDir, "-r", repoIndexFile, "**/*.jar");

		expectNoError();

		expectFileStatus(FileStatus.CREATED, DEFAULT_INDEX_FILE_COMPRESSED);
	}

	@Test
	public void testIndexSetName() throws Exception {
		initTestData(BUNDLES);

		final String repoRootDir = folder.getRootPath()
			.toString();

		executeBndCmd("index", "-d", repoRootDir, "--name", REPONAME, "**/*.jar");

		expectNoError();

		expectFileStatus(FileStatus.CREATED, IndexCommand.DEFAULT_INDEX_FILE);

		final List<String> fileContent = getFileContent(IndexCommand.DEFAULT_INDEX_FILE);

		final String substr = "<repository xmlns=\"http://www.osgi.org/xmlns/repository/v1.0.0\"";

		final long count = fileContent.stream()
			.filter(line -> line.contains(substr))
			.filter(line -> line.contains("name=\"" + REPONAME + "\""))
			// .peek(this::print)
			.count();

		assertTrue(count == 1);
	}

	private List<String> getFileContent(String relFilePath) throws IOException {
		final Path file = Paths.get(folder.getRootPath()
			.toString(), relFilePath);

		return Files.lines(file)
			.collect(Collectors.toList());
	}
}
