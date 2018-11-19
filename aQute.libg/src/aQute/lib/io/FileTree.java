package aQute.lib.io;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import aQute.libg.glob.PathSet;

public class FileTree extends PathSet {
	private final List<File> files = new ArrayList<>();

	public FileTree() {}

	/**
	 * Can be used by subclasses to add specific files to the return value of
	 * {@link #getFiles(File, String)}.
	 * 
	 * @param file A file to include in the return value of
	 *            {@link #getFiles(File, String)}.
	 */
	protected void addFile(File file) {
		if (file == null) {
			return;
		}
		if (!files.contains(file)) {
			files.add(file);
		}
	}

	/**
	 * Add an Ant-style glob to the include patterns.
	 * 
	 * @param includes Add an Ant-style glob
	 */
	public void addIncludes(List<String> includes) {
		includes(includes);
	}

	/**
	 * Add an Ant-style glob to the include patterns.
	 * 
	 * @param includes Add an Ant-style glob
	 */
	public void addIncludes(String... includes) {
		include(includes);
	}

	/**
	 * Add an Ant-style glob to the exclude patterns.
	 * 
	 * @param excludes Add an Ant-style glob
	 */
	public void addExcludes(String... excludes) {
		exclude(excludes);
	}

	/**
	 * Add an Ant-style glob to the exclude patterns.
	 * 
	 * @param excludes Add an Ant-style glob
	 */
	public void addExcludes(List<String> excludes) {
		excludes(excludes);
	}

	/**
	 * Return a list of files using the specified baseDir and the configured
	 * include and exclude Ant-style glob expressions.
	 * 
	 * @param baseDir The base directory for locating files.
	 * @param defaultIncludes The default include patterns to use if no include
	 *            patterns were configured.
	 * @return A list of files.
	 * @throws IOException If an exception occurs.
	 */
	public List<File> getFiles(File baseDir, String... defaultIncludes) throws IOException {
		return getFiles(baseDir, files.isEmpty() ? matches(defaultIncludes) : matches());
	}

	/**
	 * Return a list of files using the specified baseDir and the configured
	 * include and exclude Ant-style glob expressions.
	 * 
	 * @param baseDir The base directory for locating files.
	 * @param defaultIncludes The default include patterns to use if no include
	 *            patterns were configured.
	 * @return A list of files.
	 * @throws IOException If an exception occurs.
	 */
	public List<File> getFiles(File baseDir, List<String> defaultIncludes) throws IOException {
		return getFiles(baseDir, files.isEmpty() ? matches(defaultIncludes) : matches());
	}

	private List<File> getFiles(File baseDir, Predicate<String> matches) throws IOException {
		Path basePath = baseDir.toPath();
		try (Stream<Path> walker = Files.walk(basePath)
			.skip(1)) {
			List<File> result = Stream.concat(files.stream(), //
				walker.filter(p -> matches.test(basePath.relativize(p)
					.toString()))
					.sorted()
					.map(Path::toFile))
				.distinct()
				.collect(toList());
			return result;
		}
	}
}
