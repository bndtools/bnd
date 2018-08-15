package aQute.bnd.main.testrules;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

import org.junit.rules.TestRule;

public interface WatchedFolder extends TestRule {
	/**
	 * Gets the root path of the folder.
	 *
	 * @return the root path of the folder
	 */
	Path getRootPath();

	/**
	 * Copy data from source to watched folder.
	 *
	 * @param srcDir the source directory
	 * @return the watched folder
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	WatchedFolder copyDataFrom(Path srcDir) throws IOException;

	/**
	 * Takes a snapshot of the actual content of the folder.
	 *
	 * @param func the function applied to a path to recognice changes of the
	 *            underlying file.
	 * @throws RuntimeException Signals that an exception has occurred.
	 */
	void snapshot(Function<Path, String> func) throws RuntimeException;

	/**
	 * Takes a snapshot of the actual content of the folder.
	 *
	 * @param func the function applied to a path to recognice changes of the
	 *            underlying file.
	 * @param delay time in millis to wait after snapshot.
	 * @throws RuntimeException Signals that an exception has occurred.
	 */
	void snapshot(Function<Path, String> func, long delay) throws RuntimeException;

	/**
	 * Check file against the snapshot if exists.
	 *
	 * @param relPath the releative path to the root of the watched folder.
	 * @return the file status
	 */
	FileStatus checkFile(Path relPath);

	/**
	 * Gets the file.
	 *
	 * @param relPath the relative path with respect to folder's root
	 * @return the file
	 */
	File getFile(String relPath);

	/**
	 * Prints the content of the folder.
	 *
	 * @param printStream the printstream
	 * @param relativize the iff true print pathes relative to folder's root
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void print(PrintStream printStream, boolean relativize) throws IOException;

	/**
	 * Creates a statistic of FileStatus of the folder with respect to the last
	 * snapshot.
	 *
	 * @param relativize the iff true print pathes relative to folder's root
	 * @return The map containing the actual {@link FileStatus} of each file of
	 *         the folder.
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	Map<Path, FileStatus> createFileStatistic(boolean relativize) throws IOException;

	/**
	 * The Enum FileStatus.
	 */
	public enum FileStatus {
		// File exists and is not part of snapshot.
		CREATED,

		// File exists and is part of snapshot with different value.
		MODIFIED,

		// File exists and is part of snapshot with same value.
		UNMODIFIED_EXISTS,

		// File does not exists, but is part of snapshot.
		DELETED,

		// File does not exists and is not part of snapshot.
		UNMODIFIED_NOT_EXISTS;
	}

}
