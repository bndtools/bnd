package bndtools.editor.project;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;

/**
 * Detects merge-property conflicts: plain merge-stem properties (e.g.
 * {@code -runprogramargs}) defined in more than one file of the include tree
 * (values shadow each other instead of merging), and any property key defined
 * more than once inside the same file (later definitions silently overwrite
 * earlier ones). Include-tree conflict markers go on the including (edited)
 * file; in-file duplicate markers go on the file containing the duplicates.
 */
public class IncludeConflictDetector {

	public static final String	MARKER_TYPE		= "bndtools.core.includeconflict";
	public static final String	ATTR_KEY		= "conflictKey";
	/** ';'-joined absolute paths of the files defining the conflicting key. */
	public static final String	ATTR_SOURCES	= "conflictSources";
	/** Boolean: true when the key is duplicated within a single file. */
	public static final String	ATTR_IN_FILE	= "conflictInFile";

	/** Recomputes the conflict markers for the edited resource and its include tree. Never throws. */
	public static void updateMarkers(IResource resource, BndEditModel model) {
		if (resource == null || !resource.exists() || model == null)
			return;
		try {
			Map<String, List<File>> conflicts = findConflicts(model);
			List<InFileDuplicate> duplicates = findInFileDuplicates(model);
			List<IFile> includedFiles = new ArrayList<>();
			Processor owner = model.getOwner();
			if (owner != null && owner.getIncluded() != null) {
				for (File included : owner.getIncluded()) {
					IFile iFile = ResourcesPlugin.getWorkspace()
						.getRoot()
						.getFileForLocation(new Path(included.getAbsolutePath()));
					if (iFile != null && iFile.exists())
						includedFiles.add(iFile);
				}
			}
			IWorkspaceRunnable runnable = monitor -> {
				resource.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
				// In-file duplicate markers on included files are context-free: recomputed
				// identically by any editor, so deleting and recreating them is idempotent.
				for (IFile included : includedFiles) {
					for (IMarker m : included.findMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO)) {
						if (m.getAttribute(ATTR_IN_FILE, false))
							m.delete();
					}
				}
				for (Map.Entry<String, List<File>> e : conflicts.entrySet()) {
					String key = e.getKey();
					String files = e.getValue()
						.stream()
						.map(File::getName)
						.collect(Collectors.joining(", "));
					IMarker marker = resource.createMarker(MARKER_TYPE);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.MESSAGE, "Property '" + key
						+ "' is defined in multiple files of the include tree (" + files
						+ "); the values shadow each other instead of merging. Rename to merged syntax, e.g. '" + key
						+ ".<suffix>'.");
					marker.setAttribute(IMarker.LINE_NUMBER, findLineNumber(resource, key));
					marker.setAttribute(ATTR_KEY, key);
					marker.setAttribute(ATTR_SOURCES, e.getValue()
						.stream()
						.map(File::getAbsolutePath)
						.collect(Collectors.joining(";")));
				}
				for (InFileDuplicate d : duplicates) {
					// Marker goes on the file containing the duplicates, not on files including it.
					IResource target = ResourcesPlugin.getWorkspace()
						.getRoot()
						.getFileForLocation(new Path(d.file()
							.getAbsolutePath()));
					if (target == null || !target.exists())
						continue;
					IMarker marker = target.createMarker(MARKER_TYPE);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.MESSAGE, "Property '" + d.key() + "' is defined " + d.count()
						+ " times in " + d.file()
							.getName()
						+ "; later definitions silently overwrite earlier ones. Rename to merged syntax, e.g. '"
						+ d.key() + ".<suffix>'.");
					marker.setAttribute(IMarker.LINE_NUMBER, findDuplicateLine(d));
					marker.setAttribute(ATTR_KEY, d.key());
					marker.setAttribute(ATTR_SOURCES, d.file()
						.getAbsolutePath());
					marker.setAttribute(ATTR_IN_FILE, true);
				}
			};
			resource.getWorkspace()
				.run(runnable, resource.getWorkspace()
					.getRoot(), IWorkspace.AVOID_UPDATE, null);
		} catch (Exception e) {
			// validation only: never break the editor
		}
	}

	/** Maps each plain merge-stem key to the files defining it; entries with more than one source conflict. */
	private static Map<String, List<File>> findConflicts(BndEditModel model) throws Exception {
		Map<String, List<File>> sources = new LinkedHashMap<>();
		File docFile = model.getBndResource();
		if (docFile != null) {
			for (String key : model.getDocumentProperties()
				.stringPropertyNames()) {
				if (isPlainMergeStem(key))
					sources.computeIfAbsent(key, k -> new ArrayList<>())
						.add(docFile);
			}
		}
		Processor owner = model.getOwner();
		if (owner != null && owner.getIncluded() != null) {
			for (File included : owner.getIncluded()) {
				if (!included.isFile())
					continue;
				UTF8Properties p = new UTF8Properties();
				p.load(IO.collect(included), included, null);
				for (String key : p.stringPropertyNames()) {
					if (isPlainMergeStem(key))
						sources.computeIfAbsent(key, k -> new ArrayList<>())
							.add(included);
				}
			}
		}
		Map<String, List<File>> conflicts = new LinkedHashMap<>();
		sources.forEach((key, files) -> {
			if (files.size() > 1)
				conflicts.put(key, files);
		});
		return conflicts;
	}

	/** Plain key without suffix that supports merged syntax. */
	private static boolean isPlainMergeStem(String key) {
		return Constants.MERGED_HEADERS.contains(key);
	}

	/** A key defined more than once within a single file. */
	record InFileDuplicate(File file, String key, int count) {}

	/** Finds keys defined more than once inside the edited file or any included file. */
	private static List<InFileDuplicate> findInFileDuplicates(BndEditModel model) {
		List<File> files = new ArrayList<>();
		File docFile = model.getBndResource();
		if (docFile != null && docFile.isFile())
			files.add(docFile);
		Processor owner = model.getOwner();
		if (owner != null && owner.getIncluded() != null) {
			for (File included : owner.getIncluded())
				if (included.isFile())
					files.add(included);
		}
		List<InFileDuplicate> result = new ArrayList<>();
		for (File file : files) {
			try {
				Map<String, Integer> counts = new LinkedHashMap<>();
				scanLogicalKeys(IO.collect(file), (key, line) -> counts.merge(key, 1, Integer::sum));
				counts.forEach((key, count) -> {
					if (count > 1)
						result.add(new InFileDuplicate(file, key, count));
				});
			} catch (Exception e) {
				// skip unreadable file
			}
		}
		return result;
	}

	/** Calls the consumer with each logical property key and its 1-based physical line number. */
	private static void scanLogicalKeys(String content, java.util.function.ObjIntConsumer<String> consumer) {
		String[] lines = content.split("\r?\n", -1);
		boolean continuation = false;
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (continuation) {
				continuation = endsWithContinuation(line);
				continue;
			}
			String t = line.stripLeading();
			if (t.isEmpty() || t.charAt(0) == '#' || t.charAt(0) == '!') {
				continue;
			}
			String key = keyToken(t);
			if (!key.isEmpty())
				consumer.accept(key, i + 1);
			continuation = endsWithContinuation(line);
		}
	}

	/** The key token at the start of a (left-trimmed) logical property line. */
	private static String keyToken(String trimmedLine) {
		int end = 0;
		while (end < trimmedLine.length()) {
			char c = trimmedLine.charAt(end);
			if (c == ':' || c == '=' || Character.isWhitespace(c))
				break;
			end++;
		}
		return trimmedLine.substring(0, end);
	}

	/** True when the line ends with an odd number of backslashes (property line continuation). */
	private static boolean endsWithContinuation(String line) {
		int end = line.length();
		while (end > 0 && (line.charAt(end - 1) == '\r' || line.charAt(end - 1) == '\n'))
			end--;
		int backslashes = 0;
		for (int i = end - 1; i >= 0 && line.charAt(i) == '\\'; i--)
			backslashes++;
		return (backslashes & 1) == 1;
	}

	/** Line of the second occurrence of the duplicated key within its file, else 1. */
	private static int findDuplicateLine(InFileDuplicate d) {
		try {
			List<Integer> occurrences = new ArrayList<>();
			scanLogicalKeys(IO.collect(d.file()), (key, line) -> {
				if (key.equals(d.key()))
					occurrences.add(line);
			});
			if (occurrences.size() > 1)
				return occurrences.get(1);
			if (!occurrences.isEmpty())
				return occurrences.get(0);
		} catch (Exception e) {
			// fall through
		}
		return 1;
	}

	/** Line of the key in the edited file, else the first -include line, else 1. */
	private static int findLineNumber(IResource resource, String key) {
		try {
			String content = IO.collect(resource.getLocation()
				.toFile());
			String[] lines = content.split("\r?\n", -1);
			int includeLine = 1;
			boolean includeSeen = false;
			for (int i = 0; i < lines.length; i++) {
				String t = lines[i].trim();
				if (matchesKey(t, key))
					return i + 1;
				if (!includeSeen && matchesKey(t, Constants.INCLUDE)) {
					includeLine = i + 1;
					includeSeen = true;
				}
			}
			return includeLine;
		} catch (Exception e) {
			return 1;
		}
	}

	private static boolean matchesKey(String line, String key) {
		if (!line.startsWith(key))
			return false;
		String rest = line.substring(key.length());
		return rest.isEmpty() || rest.charAt(0) == ':' || rest.charAt(0) == '='
			|| Character.isWhitespace(rest.charAt(0));
	}

	/**
	 * Suffix for renaming a plain key in the given file: the sanitized file
	 * name without extension, made unique against keys already present in the
	 * file.
	 */
	static String suggestSuffixForFile(File file, String key) {
		String name = suffixBase(file);
		try {
			UTF8Properties p = new UTF8Properties();
			p.load(IO.collect(file), file, null);
			String suffix = name;
			int n = 2;
			while (p.containsKey(key + "." + suffix))
				suffix = name + "-" + n++;
			return suffix;
		} catch (Exception e) {
			return name;
		}
	}

	/** Sanitized file name without .bnd/.bndrun extension, for use as a merge-key suffix. */
	private static String suffixBase(File file) {
		String name = file.getName();
		if (name.endsWith(".bndrun"))
			name = name.substring(0, name.length() - ".bndrun".length());
		else if (name.endsWith(".bnd"))
			name = name.substring(0, name.length() - ".bnd".length());
		name = name.replaceAll("[^A-Za-z0-9._-]", "-");
		return name.isEmpty() ? "local" : name;
	}

	/** Renames the first occurrence of the key at line start, preserving indentation and separator. */
	static void renameKeyInFile(File file, String key, String newKey) throws Exception {
		String content = IO.collect(file);
		Pattern pattern = Pattern.compile("(?m)^([ \\t]*)" + Pattern.quote(key) + "(?=\\s*[:=\\s])");
		Matcher matcher = pattern.matcher(content);
		if (!matcher.find())
			return;
		String updated = new StringBuilder(content).replace(matcher.start(), matcher.end(),
			matcher.group(1) + newKey)
			.toString();
		storeContent(file, updated);
	}

	/**
	 * Renames the second and later occurrences of the key within the file to
	 * unique merged-syntax keys (filename-based suffix), keeping the first
	 * occurrence unchanged. Preserves line endings and layout.
	 */
	static void renameDuplicateKeysInFile(File file, String key) throws Exception {
		String content = IO.collect(file);
		UTF8Properties existing = new UTF8Properties();
		existing.load(content, file, null);
		String base = suffixBase(file);
		Set<String> planned = new LinkedHashSet<>();
		// split keeping the EOL attached to each line so layout is preserved
		String[] lines = content.split("(?<=\n)", -1);
		StringBuilder out = new StringBuilder(content.length() + 64);
		boolean continuation = false;
		int seen = 0;
		for (String line : lines) {
			String emit = line;
			if (continuation) {
				continuation = endsWithContinuation(line);
			} else {
				String t = line.stripLeading();
				boolean comment = t.isEmpty() || t.charAt(0) == '#' || t.charAt(0) == '!';
				if (!comment && keyToken(t).equals(key)) {
					seen++;
					if (seen > 1) {
						String newKey = key + "." + base;
						int n = 2;
						while (existing.containsKey(newKey) || planned.contains(newKey))
							newKey = key + "." + base + "-" + n++;
						planned.add(newKey);
						int idx = line.indexOf(key);
						emit = line.substring(0, idx) + newKey + line.substring(idx + key.length());
					}
				}
				continuation = !comment && endsWithContinuation(line);
			}
			out.append(emit);
		}
		storeContent(file, out.toString());
	}

	/** Writes updated content through the workspace file when possible, else directly to disk. */
	private static void storeContent(File file, String updated) throws Exception {
		IFile iFile = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getFileForLocation(new Path(file.getAbsolutePath()));
		if (iFile != null && iFile.exists()) {
			iFile.setContents(new ByteArrayInputStream(updated.getBytes(StandardCharsets.UTF_8)), true, true, null);
		} else {
			IO.store(updated, file);
		}
	}

	private IncludeConflictDetector() {}
}
