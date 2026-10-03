package aQute.bnd.osgi;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/** A property definition lost during parsing or include resolution. */
public record PropertyConflict(Kind kind, String key, List<Occurrence> occurrences, Occurrence winner) {
	/** A declaration, with a zero-based line and half-open key offsets; -1 means unknown offsets. */
	public record Occurrence(String key, String source, int line, int start, int end) {}

	/** All collected conflicts, independent of configured severity. */
	public static List<PropertyConflict> getConflicts(Processor processor) {
		return processor.getPropertyConflicts();
	}

	/**
	 * Creates an owned processor for an editor snapshot. The caller must close it.
	 * Includes resolve relative to the file; the parent supplies inherited settings.
	 */
	public static Processor analyze(Processor parent, File file, String content) throws IOException {
		Processor processor = parent == null ? new Processor() : new Processor(parent);
		try {
			processor.setPropertiesFile(file.getAbsoluteFile());
			processor.setBase(file.getAbsoluteFile().getParentFile());
			processor.setProperties(new StringReader(content));
			return processor;
		} catch (IOException | RuntimeException exception) {
			processor.close();
			throw exception;
		}
	}
	public enum Kind {
		DUPLICATE, INCLUDE
	}

	public PropertyConflict {
		occurrences = List.copyOf(occurrences);
	}

	/**
	 * Returns true if the key is a plain merged header stem or a suffixed
	 * form of a merged header.
	 */
	public static boolean isMergedHeader(String key) {
		if (key == null)
			return false;
		if (Constants.MERGED_HEADERS.contains(key))
			return true;
		int dot = key.indexOf('.');
		return dot > 0 && Constants.MERGED_HEADERS.contains(key.substring(0, dot));
	}

	/** Whether unique suffixed keys can be consumed by bnd's merged-property API. */
	public boolean mergeable() {
		return isMergedHeader(key);
	}
}
