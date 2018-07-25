package aQute.bnd.exporter.subsystem;

import java.util.stream.Stream;

public enum EsaArchivType {
	NONE,
	CONTENT,
	ALL;

	public static EsaArchivType byParameter(String archiveContent) {

		if (archiveContent == null) {
			return CONTENT;/// default
		}

		return Stream.of(EsaArchivType.values())
			.filter(t -> archiveContent.toUpperCase()
				.equals(t.name()))
			.findFirst()
			.orElse(null);
	}
}