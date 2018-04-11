package aQute.bnd.exporter.subsystem;

public enum EasArchivType {
	NONE,
	CONTENT,
	ALL;

	public static EasArchivType byParameter(String archiveContent) {

		if (archiveContent == null) {
			return CONTENT;/// default
		}
		return valueOf(archiveContent.toUpperCase());
	}
}
