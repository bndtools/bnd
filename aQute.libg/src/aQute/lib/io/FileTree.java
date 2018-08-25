package aQute.lib.io;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import aQute.libg.glob.AntGlob;

public class FileTree {
	private List<File>		files		= new ArrayList<>();
	private List<Pattern>	includes	= new ArrayList<>();
	private List<Pattern>	excludes	= new ArrayList<>();

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
		if (includes == null) {
			return;
		}
		for (String include : includes) {
			if (include == null) {
				continue;
			}
			this.includes.add(AntGlob.toPattern(include));
		}
	}

	/**
	 * Add an Ant-style glob to the include patterns.
	 * 
	 * @param includes Add an Ant-style glob
	 */
	public void addIncludes(String... includes) {
		if (includes == null) {
			return;
		}
		for (String include : includes) {
			if (include == null) {
				continue;
			}
			this.includes.add(AntGlob.toPattern(include));
		}
	}

	/**
	 * Add an Ant-style glob to the exclude patterns.
	 * 
	 * @param excludes Add an Ant-style glob
	 */
	public void addExcludes(String... excludes) {
		if (excludes == null) {
			return;
		}
		for (String exclude : excludes) {
			if (exclude == null) {
				continue;
			}
			this.excludes.add(AntGlob.toPattern(exclude));
		}
	}

	/**
	 * Add an Ant-style glob to the exclude patterns.
	 * 
	 * @param excludes Add an Ant-style glob
	 */
	public void addExcludes(List<String> excludes) {
		if (excludes == null) {
			return;
		}
		for (String exclude : excludes) {
			if (exclude == null) {
				continue;
			}
			this.excludes.add(AntGlob.toPattern(exclude));
		}
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
		if (includes.isEmpty() && files.isEmpty() && (defaultIncludes != null) && (defaultIncludes.length > 0)) {
			return getFiles(baseDir, toPatterns(Stream.of(defaultIncludes)), excludes);
		}
		return getFiles(baseDir, includes, excludes);
	}

	private List<Pattern> toPatterns(Stream<String> globs) {
		return globs.filter(Objects::nonNull)
			.map(AntGlob::toPattern)
			.collect(toList());
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
		if (includes.isEmpty() && files.isEmpty() && (defaultIncludes != null) && !defaultIncludes.isEmpty()) {
			return getFiles(baseDir, toPatterns(defaultIncludes.stream()), excludes);
		}
		return getFiles(baseDir, includes, excludes);
	}

	private List<File> getFiles(File baseDir, List<Pattern> includePatterns, List<Pattern> excludePatterns)
		throws IOException {
		if (includePatterns.isEmpty()) {
			return new ArrayList<>(files);
		}
		Path basePath = baseDir.toPath();
		try (Stream<Path> walker = Files.walk(basePath)
			.skip(1)) {
			List<File> result = Stream.concat(files.stream(), //
				walker.filter(p -> {
					String path = basePath.relativize(p)
						.toString();
					return includePatterns.stream()
						.anyMatch(i -> i.matcher(path)
							.matches())
						&& !excludePatterns.stream()
							.anyMatch(e -> e.matcher(path)
								.matches());
				})
					.sorted()
					.map(Path::toFile))
				.distinct()
				.collect(toList());
			return result;
		}
	}
}
