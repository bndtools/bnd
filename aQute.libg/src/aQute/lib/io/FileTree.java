package aQute.lib.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import aQute.libg.glob.PathSet;

public class FileTree {
	private final List<File>	files	= new ArrayList<>();
	private final PathSet		paths	= new PathSet();

	public FileTree() {}

	/**
	 * Can be used to add specific files to the return value of
	 * {@link #getFiles(File, String...)} and {@link #getFiles(File, List)}.
	 *
	 * @param file A file to include in the return value of
	 *            {@link #getFiles(File, String...)} and
	 *            {@link #getFiles(File, List)}.
	 */
	public void addFile(File file) {
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
		paths.includes(includes);
	}

	/**
	 * Add an Ant-style glob to the include patterns.
	 *
	 * @param includes Add an Ant-style glob
	 */
	public void addIncludes(String... includes) {
		paths.include(includes);
	}

	/**
	 * Add an Ant-style glob to the exclude patterns.
	 *
	 * @param excludes Add an Ant-style glob
	 */
	public void addExcludes(String... excludes) {
		paths.exclude(excludes);
	}

	/**
	 * Add an Ant-style glob to the exclude patterns.
	 *
	 * @param excludes Add an Ant-style glob
	 */
	public void addExcludes(List<String> excludes) {
		paths.excludes(excludes);
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
		return getFiles(baseDir, files.isEmpty() ? paths.matches(defaultIncludes) : paths.matches());
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
		return getFiles(baseDir, files.isEmpty() ? paths.matches(defaultIncludes) : paths.matches());
	}

	private List<File> getFiles(File baseDir, Predicate<String> matches) throws IOException {
		return new TreeWalker(baseDir, matches, files).getFiles();
	}

	// Depth first tree walker. Sorts directory entries using IO.fileCollator.
	static class TreeWalker {
		private final Path				basePath;
		private final Predicate<String>	matches;
		private final List<File>		files;
		private final Collator			fileCollator	= IO.fileCollator();
		private final Deque<File>		queue			= new ArrayDeque<>();

		TreeWalker(File baseDir, Predicate<String> matches, List<File> files) {
			basePath = baseDir.toPath();
			this.matches = matches;
			this.files = new ArrayList<>(files);
			queueDirectoryContents(baseDir);
		}

		private void queueDirectoryContents(File dir) {
			if (dir.isDirectory()) {
				String[] names = dir.list();
				if (names != null) {
					final int length = names.length;
					CollationKey[] keys = new CollationKey[length];
					for (int i = 0; i < length; i++) {
						keys[i] = fileCollator.getCollationKey(names[i]);
					}
					Arrays.sort(keys);
					for (int i = length - 1; i >= 0; i--) {
						queue.addFirst(new File(dir, keys[i].getSourceString()));
					}
				}
			}
		}

		List<File> getFiles() {
			ArrayList<File> result = new ArrayList<>();
			while (!queue.isEmpty()) {
				File next = queue.pollFirst();
				queueDirectoryContents(next);
				Path path = basePath.relativize(next.toPath());
				if (matches.test(path.toString())) {
					files.remove(next);
					result.add(next);
				}
			}
			if (!files.isEmpty()) {
				result.addAll(files);
			}
			result.trimToSize();
			return result;
		}
	}

	@Override
	public String toString() {
		return String.format("[files: %s, paths: %s]", files, paths);
	}
}
