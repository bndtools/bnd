package aQute.bnd.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Rule;

import aQute.bnd.main.testrules.CapturedSystemOutput;
import aQute.bnd.main.testrules.WatchedFolder;
import aQute.bnd.main.testrules.WatchedFolder.FileStatus;
import aQute.bnd.main.testrules.WatchedTemporaryFolder;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Jar;

public class TestBndMainBase {

	private static final String						WARNING_LINE			= "(?m)^WARNING: .*$";

	@Rule
	public WatchedFolder							folder					= new WatchedTemporaryFolder();

	@Rule
	public CapturedSystemOutput						capturedStdIO			= new CapturedSystemOutput();

	protected static final Function<Path, String>	DEFAULT_SNAPSHOT_FUNC	= p -> {
																				return p.toFile()
																					.lastModified() + "";
																			};

	private static final String						TESTDATA_BASE_DIR		= "testdata";

	protected static final String					BUNDLES					= "bundles";

	protected static final String					STANDALONE				= "standalone";

	protected static final String					WORKSPACE				= "workspace";

	/* BndCmd */
	protected void executeBndCmd(String... cmd) throws Exception {
		bnd.mainNoExit(cmd, folder.getRootPath());
	}

	protected void executeBndCmd(Path subBase, String... cmd) throws Exception {
		bnd.mainNoExit(cmd, folder.getRootPath()
			.resolve(subBase));
	}

	protected String getVersion() {
		return About.CURRENT.getWithoutQualifier()
			.toString();
	}

	/* Folder based helper */
	protected void initTestData(final String subdir) throws IOException {
		folder.copyDataFrom(Paths.get(TESTDATA_BASE_DIR, subdir))
			.snapshot(DEFAULT_SNAPSHOT_FUNC, 1000l);
		// delay after snapshot to correctly reflect file-modifications based on
		// timestampchanges
	}

	protected void initTestDataAll() throws IOException {
		folder.copyDataFrom(Paths.get("", TESTDATA_BASE_DIR))
			.snapshot(DEFAULT_SNAPSHOT_FUNC, 1000l);
		// delay after snapshot to correctly reflect file-modifications based on
		// timestampchanges
	}

	protected void expectFileStatus(FileStatus expectedFileStatus, String p) {
		expectFileStatus(expectedFileStatus, p.split("/"));
	}

	protected void expectFileStatus(FileStatus expectedFileStatus, String... p) {

		Path path = Paths.get("", p);
		assertEquals(path.toString(), expectedFileStatus, folder.checkFile(path));
	}

	protected Map<FileStatus, Long> countFilesForAllStatus() throws IOException {
		return folder.createFileStatistic(true)
			.values()
			.stream()
			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
	}

	protected Long countFilesForStatus(FileStatus fileStatus) throws IOException {
		return count(countFilesForAllStatus(), fileStatus);
	}

	protected void expectFileCounts(Long created, Long deleted) throws IOException {
		final Map<FileStatus, Long> counts = countFilesForAllStatus();

		assertEquals(created, count(counts, FileStatus.CREATED));
		assertEquals(deleted, count(counts, FileStatus.DELETED));
	}

	private Long count(Map<FileStatus, Long> counts, FileStatus fileStatus) {
		final Long count = counts.get(fileStatus);
		return count == null ? 0 : count;
	}

	/* IO */
	protected void expectOutput(String expected) {
		assertEquals("wrong output", expected, capturedStdIO.getSystemOutContent());
	}

	protected void expectOutputContains(String expected) {
		assertThat("missing output", capturedStdIO.getSystemOutContent(), containsString(expected));
	}

	protected void expectOutputContainsPattern(String regex) {
		assertThat(capturedStdIO.getSystemOutContent()).as("output does not contain pattern")
			.containsPattern(regex);
	}

	protected void expectErrorContains(String expected) {
		assertThat("missing error", capturedStdIO.getSystemErrContent(), containsString(expected));
	}

	protected void expectErrorContainsPattern(String regex) {
		assertThat(capturedStdIO.getSystemErrContent()).as("error does not contain pattern")
			.containsPattern(regex);
	}

	protected void expectNoError(boolean ignoreWarnings, String... expects) {
		String errors = capturedStdIO.getSystemErrContent();

		if (ignoreWarnings) {
			errors = errors.replaceAll(WARNING_LINE, "")
				.trim();
		}
		if (expects != null) {
			for (String expect : expects) {
				assertThat("missing error", capturedStdIO.getSystemErrContent(), containsString(expect));
				errors = errors.replaceAll(expect, "")
					.trim();
			}
		}
		assertEquals("non-empty error output", "", errors);
	}

	protected void expectNoError() {
		expectNoError(false);
	}

	protected void expectJarEntry(Jar jar, String path) {
		assertNotNull("missing entry in jar: " + path, jar.getResource(path));
	}

	/* print etc. */
	protected String getSystemOutContent() {
		return capturedStdIO.getSystemOutContent();
	}

	protected String getSystemErrContent() {
		return capturedStdIO.getSystemErrContent();
	}

	protected PrintStream getSystemOut() {
		return capturedStdIO.getSystemOut();
	}

	protected PrintStream getSystemErr() {
		return capturedStdIO.getSystemErr();
	}

	protected void print(final String str) {
		getSystemOut().print(str);
	}

	protected void println(final String str) {
		getSystemOut().println(str);
	}
}
