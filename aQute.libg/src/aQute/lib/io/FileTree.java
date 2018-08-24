package aQute.lib.io;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
		if (!files.contains(file)) {
			files.add(file);
		}
	}

	/**
	 * Add an Ant-style glob to the include patterns.
	 * 
	 * @param include Add an Ant-style glob
	 */
	public void addInclude(String include) {
		includes.add(AntGlob.toPattern(include));
	}

	/**
	 * Add an Ant-style glob to the include patterns.
	 * 
	 * @param addIncludes Add an Ant-style glob
	 */
	public void addIncludes(List<String> addIncludes) {
		for (String include : addIncludes) {

			includes.add(AntGlob.toPattern(include));
		}
	}

	/**
	 * Add an Ant-style glob to the exclude patterns.
	 * 
	 * @param exclude Add an Ant-style glob
	 */
	public void addExclude(String exclude) {
		excludes.add(AntGlob.toPattern(exclude));
	}

	/**
	 * Add an Ant-style glob to the exclude patterns.
	 * 
	 * @param addExcludes Add an Ant-style glob
	 */
	public void addExcludes(List<String> addExcludes) {

		for (String exclude : addExcludes) {

			excludes.add(AntGlob.toPattern(exclude));
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
	public List<File> getFiles(File baseDir, List<String> defaultIncludes) throws IOException {

		List<Pattern> includePatterns = includes.isEmpty() && files.isEmpty() && (defaultIncludes != null)
			? defaultIncludes.stream()
				.map(AntGlob::toPattern)
				.collect(toList())
			: includes;
		if (includePatterns.isEmpty()) {
			return files;
		}
		List<Pattern> excludePatterns = excludes;

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
