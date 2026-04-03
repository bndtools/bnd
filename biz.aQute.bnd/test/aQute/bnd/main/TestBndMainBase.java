package aQute.bnd.main;

import static aQute.libg.re.Catalog.caseInsenstive;
import static aQute.libg.re.Catalog.lit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
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
import aQute.lib.strings.Strings;
import aQute.libg.re.RE;

public class TestBndMainBase {

	@Rule
	public WatchedFolder							folder					= new WatchedTemporaryFolder();

	@Rule
	public CapturedSystemOutput						capturedStdIO			= new CapturedSystemOutput();

	protected static final Function<Path, String>	DEFAULT_SNAPSHOT_FUNC	= p -> (p.toFile()
		.lastModified() + "");

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
			.snapshot(DEFAULT_SNAPSHOT_FUNC, 1000L);
		// delay after snapshot to correctly reflect file-modifications based on
		// timestampchanges
	}

	protected void initTestDataAll() throws IOException {
		folder.copyDataFrom(Paths.get("", TESTDATA_BASE_DIR))
			.snapshot(DEFAULT_SNAPSHOT_FUNC, 1000L);
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
		assertThat(capturedStdIO.getSystemOutContent()).as("missing output")
			.contains(expected);
	}

	protected void expectOutputContainsPattern(String regex) {
		assertThat(capturedStdIO.getSystemOutContent()).as("output does not contain pattern")
			.containsPattern(regex);
	}

	protected void expectErrorContains(String expected) {
		assertThat(capturedStdIO.getSystemErrContent()).as("missing error")
			.contains(expected);
	}

	protected void expectErrorContainsPattern(String regex) {
		assertThat(capturedStdIO.getSystemErrContent()).as("error does not contain pattern")
			.containsPattern(regex);
	}

	final static RE	WARNINGS_P	= caseInsenstive(lit("Warnings"));
	final static RE	ERRORS_P	= caseInsenstive(lit("Errors"));
	final static RE	SEPARATOR_P	= caseInsenstive(lit("-------"));

	protected void expectNoError(boolean ignoreWarnings, String... expects) {
		String errors = capturedStdIO.getSystemErrContent();
		List<String> lines = Strings.split("\\R", errors);
		boolean skip = false;
		line: for (Iterator<String> it = lines.iterator(); it.hasNext();) {
			String line = it.next();
			if (Strings.nonNullOrEmpty(Strings.trim(line))) {
				it.remove();
				continue;
			}

			if (SEPARATOR_P.lookingAt(line)
				.isPresent()) {
				it.remove();
				continue;
			}
			if (WARNINGS_P.lookingAt(line)
				.isPresent()) {
				skip = ignoreWarnings;
				it.remove();
				continue;
			} else if (ERRORS_P.lookingAt(line)
				.isPresent()) {
				skip = false;
				it.remove();
				continue;
			}
			if (expects != null) {
				for (String expect : expects) {
					if (skip)
						continue line;
					skip = line.contains(expect);
				}
			}
			if (skip)
				it.remove();
		}
		assertThat(lines).isEmpty();
	}

	protected void expectNoError() {
		expectNoError(true);
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
