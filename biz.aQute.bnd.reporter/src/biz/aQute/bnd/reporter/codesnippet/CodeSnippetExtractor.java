package biz.aQute.bnd.reporter.codesnippet;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import aQute.lib.fileset.FileSet;
import biz.aQute.bnd.reporter.codesnippet.dto.CodeSnippetDTO;
import biz.aQute.bnd.reporter.codesnippet.dto.CodeSnippetGroupDTO;
import biz.aQute.bnd.reporter.codesnippet.dto.CodeSnippetProgramDTO;
import biz.aQute.bnd.reporter.helpers.FileHelper;

/**
 * This class allows to extract code snippets from source codes contained in a
 * set of directory paths.
 * <p>
 * A code snippet is a sample of source codes with a title and a short
 * description that serves to illustrate a specific code usage. Code snippets
 * can either provide a single snippet or contain a list of steps, each one with
 * a title and a description.
 */
public class CodeSnippetExtractor {

	/* Contains the number of time an id has been used. */
	final Map<String, Integer>			_idCache	= new HashMap<>();

	/* A map of file extension to the appropriate snippet reader. */
	final Map<String, SnippetReader>	_readers	= new HashMap<>();

	/**
	 * Construct a code snippet extractor.
	 */
	public CodeSnippetExtractor() {
		/*
		 * To extend the extractor with new languages (xml, json, ...) add the
		 * appropriate readers below.
		 */
		addReader(new JavaSnippetReader());
	}

	/**
	 * Extract a list of code snippet by analyzing source codes contained in the
	 * directory paths given in argument.
	 * <p>
	 * Code snippets are ordered as they appear in the directories (files are
	 * first sorted in a lexicographic order).
	 *
	 * @param directoryPaths a list of directory paths from which code snippets
	 *            are looked up.
	 * @return an ordered list of code snippets.
	 */
	public List<CodeSnippetDTO> extract(final String... directoryPaths) {
		checkPath(directoryPaths);

		final List<CodeSnippetDTO> codeSnippets = new LinkedList<>();
		final Map<String, CodeSnippetGroupDTO> groupCache = new HashMap<>();
		final Map<String, List<CodeSnippetProgramDTO>> groupProgramCache = new HashMap<>();
		final List<File> sortedFiles = getSupportedFiles(directoryPaths);

		init();

		/*
		 * We go through each file and for each of them we extract zero or more
		 * snippet
		 */
		sortedFiles.stream()
			.flatMap(file -> extractSnippets(file))
			.forEach(snippet -> {
				/*
				 * There is three cases, the snippet has steps, the snippet is a
				 * step in a group or is a single snippet.
				 */
				if (snippet.getGroupName() != null) {
					/*
					 * If the snippet has steps, we place it in a cache, so
					 * later, steps can be added to it. We optionally add
					 * previously found steps.
					 */
					codeSnippets.add(snippet.getCodeSnippetGroup());
					groupCache.put(snippet.getGroupName(), snippet.getCodeSnippetGroup());
					if (groupProgramCache.get(snippet.getGroupName()) != null) {
						snippet.getCodeSnippetGroup().steps.addAll(groupProgramCache.get(snippet.getGroupName()));
					}
				} else if (snippet.getParentGroup() != null) {
					/*
					 * If the snippet is a step, we add it to the parent
					 * snippet, or if not found yet, to a cache.
					 */
					if (groupCache.get(snippet.getParentGroup()) != null) {
						groupCache.get(snippet.getParentGroup()).steps.add(snippet.getCodeSnippetProgram());
					} else {
						if (groupProgramCache.get(snippet.getParentGroup()) == null) {
							groupProgramCache.put(snippet.getParentGroup(), new LinkedList<>());
						}
						groupProgramCache.get(snippet.getParentGroup())
							.add(snippet.getCodeSnippetProgram());
					}
				} else {
					/* Case for a single snippet. */
					codeSnippets.add(snippet.getCodeSnippetProgram());
				}
			});

		return codeSnippets;
	}

	static private void checkPath(final String[] directoryPaths) {
		Objects.requireNonNull(directoryPaths, "directoryPaths");

		if (directoryPaths.length == 0) {
			throw new IllegalArgumentException("directoryPaths must not be empty.");
		}

		for (final String directoryPath : directoryPaths) {
			final File directory = new File(directoryPath);
			if (!directory.exists()) {
				throw new IllegalArgumentException("The directory " + directoryPath + " does not exist.");
			}
			if (!directory.isDirectory()) {
				throw new IllegalArgumentException(directoryPath + " is not a directory.");
			}
		}
	}

	private void addReader(final SnippetReader reader) {
		reader.init(this::generateId);
		reader.getSupportedExtension()
			.forEach(e -> _readers.put(e, reader));
	}

	private String generateId(final String baseId) {
		final Integer count = _idCache.put(baseId, Integer.valueOf(_idCache.getOrDefault(baseId, Integer.valueOf(0))
			.intValue() + 1));
		if (count != null) {
			return baseId + count;
		} else {
			return baseId;
		}
	}

	private void init() {
		_idCache.clear();
	}

	/*
	 * Return a sorted list of files that match the extractor's available
	 * snippet readers capability.
	 */
	private List<File> getSupportedFiles(final String[] directoryPaths) {
		final List<File> files = new ArrayList<>(50);
		final String spec = _readers.keySet()
			.stream()
			.map(e -> "**/*." + e)
			.reduce("", (p, n) -> p + "," + n);

		for (final String directoryPath : directoryPaths) {
			final FileSet fileSet = new FileSet(new File(directoryPath), spec);
			files.addAll(fileSet.getFiles());
		}

		files.sort(Comparator.comparing(File::getAbsolutePath, String::compareToIgnoreCase));

		return files;
	}

	/*
	 * Select the appropriate readers and extract zero or more snippet from the
	 * file in argument.
	 */
	private Stream<Snippet> extractSnippets(final File file) {
		final SnippetReader reader = _readers.get(FileHelper.getExtension(file));

		if (reader != null) {
			try {
				return reader.read(file)
					.stream();
			} catch (final Exception exception) {
				throw new RuntimeException("Failed to read snippet from file " + file.toPath(), exception);
			}
		} else {
			return Stream.empty();
		}
	}
}
