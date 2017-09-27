package aQute.bnd.metadata;

import java.util.Objects;

import aQute.bnd.metadata.dto.BundleMetadataDTO;
import aQute.bnd.osgi.Jar;

/**
 * A {@code MetadataAnalizer} can analyze the metadata contained in a jar and
 * create a DTO representation of it.
 */
final public class MetadataAnalizer {

	private MetadataAnalizer() {

	}

	/**
	 * Analyze the metadata of the jar in argument and create a DTO representation
	 * of it.
	 * 
	 * @param jar the jar to analyze, must not be {@code null}
	 * @return the DTO representation of the metadata, never {@code null}
	 * @throws Exception if the DTO representation is not valid
	 */
	public static BundleMetadataDTO analyze(Jar jar) throws Exception {

		Objects.requireNonNull(jar, "jar");

		BundleMetadataDTO dto = new BundleMetadataDTO();

		for (MetadataExtractor e : Extractors.METADATA_EXTRACTORS) {

			e.extract(dto, jar);
			e.verify(dto);
		}

		return dto;
	}
}
