package aQute.bnd.exporter.subsystem;

public enum EsaArchiveType {
	NONE,
	CONTENT,
	ALL;

	public static EsaArchiveType byParameter(String archiveContent) {
		if (archiveContent == null) {
			return CONTENT; // default
		}
		try {
			return valueOf(archiveContent.toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
