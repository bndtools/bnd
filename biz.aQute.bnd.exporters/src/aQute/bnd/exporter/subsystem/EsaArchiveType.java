package aQute.bnd.exporter.subsystem;

import java.util.Locale;

public enum EsaArchiveType {
	NONE,
	CONTENT,
	ALL;

	public static EsaArchiveType byParameter(String archiveContent) {
		if (archiveContent == null) {
			return CONTENT; // default
		}
		try {
			return valueOf(archiveContent.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
