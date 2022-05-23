package aQute.lib.io;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.file.Path;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
	 */
	public List<File> getFiles(File baseDir, String... defaultIncludes) {
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
	 */
	public List<File> getFiles(File baseDir, List<String> defaultIncludes) {
		return getFiles(baseDir, files.isEmpty() ? paths.matches(defaultIncludes) : paths.matches());
	}

	/**
	 * Return a list of files using the specified baseDir and the specified
	 * relative path matching predicate.
	 *
	 * @param baseDir The base directory for locating files.
	 * @param matches The path matching predicate. The predicate only matches
	 *            against the relative path from the specified baseDir.
	 * @return A list of files.
	 */
	public List<File> getFiles(File baseDir, Predicate<String> matches) {
		ArrayList<File> result = new ArrayList<>();
		new FileTreeSpliterator(baseDir, matches, files).forEachRemaining(result::add);
		result.trimToSize();
		return result;
	}

	/**
	 * Return a stream of files using the specified baseDir and the configured
	 * include and exclude Ant-style glob expressions.
	 *
	 * @param baseDir The base directory for locating files.
	 * @param defaultIncludes The default include patterns to use if no include
	 *            patterns were configured.
	 * @return A stream of files.
	 */
	public Stream<File> stream(File baseDir, String... defaultIncludes) {
		return stream(baseDir, files.isEmpty() ? paths.matches(defaultIncludes) : paths.matches());
	}

	/**
	 * Return a stream of files using the specified baseDir and the configured
	 * include and exclude Ant-style glob expressions.
	 *
	 * @param baseDir The base directory for locating files.
	 * @param defaultIncludes The default include patterns to use if no include
	 *            patterns were configured.
	 * @return A stream of files.
	 */
	public Stream<File> stream(File baseDir, List<String> defaultIncludes) {
		return stream(baseDir, files.isEmpty() ? paths.matches(defaultIncludes) : paths.matches());
	}

	/**
	 * Return a stream of files using the specified baseDir and the specified
	 * relative path matching predicate.
	 *
	 * @param baseDir The base directory for locating files.
	 * @param matches The path matching predicate. The predicate only matches
	 *            against the relative path from the specified baseDir.
	 * @return A stream of files.
	 */
	public Stream<File> stream(File baseDir, Predicate<String> matches) {
		return StreamSupport.stream(new FileTreeSpliterator(baseDir, matches, files), false);
	}

	// Spliterator which performs a depth-first walk.
	// Sorts entries within a directory using IO.fileCollator.
	final static class FileTreeSpliterator extends AbstractSpliterator<File> {
		private final Path				basePath;
		private final Predicate<String>	matches;
		private final List<File>		files;
		private final Spliterator<File>	extra;
		private final Collator			fileCollator	= IO.fileCollator();
		private final Deque<File>		queue			= new ArrayDeque<>();

		FileTreeSpliterator(File baseDir, Predicate<String> matches, List<File> files) {
			super(Long.MAX_VALUE,
				Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.DISTINCT | Spliterator.NONNULL);
			basePath = baseDir.toPath();
			this.matches = requireNonNull(matches);
			List<File> copy = new ArrayList<>(files); // mutable copy
			this.files = copy;
			extra = copy.spliterator(); // late-binding
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

		@Override
		public void forEachRemaining(Consumer<? super File> action) {
			for (File next; (next = queue.pollFirst()) != null;) {
				queueDirectoryContents(next);
				Path path = basePath.relativize(next.toPath());
				if (matches.test(path.toString())) {
					files.remove(next);
					action.accept(next);
				}
			}
			extra.forEachRemaining(action);
		}

		@Override
		public boolean tryAdvance(Consumer<? super File> action) {
			for (File next; (next = queue.pollFirst()) != null;) {
				queueDirectoryContents(next);
				Path path = basePath.relativize(next.toPath());
				if (matches.test(path.toString())) {
					files.remove(next);
					action.accept(next);
					return true;
				}
			}
			return extra.tryAdvance(action);
		}
	}

	@Override
	public String toString() {
		return String.format("[files: %s, paths: %s]", files, paths);
	}
}
