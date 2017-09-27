package aQute.bnd.metadata;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import aQute.bnd.metadata.dto.BundleMetadataDTO;
import aQute.lib.json.Decoder;
import aQute.lib.json.Encoder;
import aQute.lib.json.JSONCodec;

/**
 * A {@code MetadataJsonIO} can serialize and deserialize a bundle metadata DTO
 * in the Json format.
 */
final public class MetadataJsonIO {

	private MetadataJsonIO() {

	}

	/**
	 * Deserialize a bundle metadata Json representation from the input stream in
	 * argument using the UTF-8 charset and verify that the representation is valid.
	 * 
	 * @param inputStream the input stream which contain the serialization, must not
	 *            be {@code null}
	 * @return the deserialized bundle metadata DTO, never {@code null}
	 * @throws Exception if any error occurs in the input stream or the input stream
	 *             contains an invalid metadata bundle representation
	 */
	static public BundleMetadataDTO fromJson(final InputStream inputStream) throws Exception {

		return fromJson(inputStream, null);
	}

	/**
	 * Deserialize a bundle metadata Json representation from the input stream in
	 * argument using the specified charset and verify that the representation is
	 * valid.
	 * 
	 * @param inputStream the input stream which contain the serialization, must not
	 *            be {@code null}
	 * @param charset the charset to use, can be {@code null}
	 * @return the deserialized bundle metadata DTO, never {@code null}
	 * @throws Exception if any error occurs in the input stream or the input stream
	 *             contains an invalid metadata bundle representation
	 */
	static public BundleMetadataDTO fromJson(final InputStream inputStream, final Charset charset) throws Exception {

		Objects.requireNonNull(inputStream, "stream");

		JSONCodec codec = new JSONCodec();
		Charset cs = charset != null ? charset : StandardCharsets.UTF_8;

		BundleMetadataDTO result = null;

		try (Decoder decoder = codec.dec().charset(cs).from(inputStream)) {

			result = decoder.get(BundleMetadataDTO.class);
		}

		for (MetadataExtractor e : Extractors.METADATA_EXTRACTORS) {

			e.verify(result);
		}

		return result;
	}

	/**
	 * Serialize the bundle metadata DTO to the output stream in arguments in the
	 * Json format using the UTF-8 charset.
	 * 
	 * @param bundleDto the bundle metadata DTO to serialize, must not be
	 *            {@code null}
	 * @param outputStream the output stream which must contain the serialization,
	 *            must not be {@code null}
	 * @throws Exception if any error occurs in the output stream
	 */
	static public void toJson(final BundleMetadataDTO bundleDto, final OutputStream outputStream)
			throws Exception {

		toJson(bundleDto, outputStream, null);
	}

	/**
	 * Serialize the bundle metadata DTO to the output stream in arguments in the
	 * Json format using the specified charset.
	 * 
	 * @param bundleDto the bundle metadata DTO to serialize, must not be
	 *            {@code null}
	 * @param outputStream the output stream which must contain the serialization,
	 *            must not be {@code null}
	 * @param charset the charset to use, can be {@code null}
	 * @throws Exception if any error occurs in the output stream
	 */
	static public void toJson(final BundleMetadataDTO bundleDto, final OutputStream outputStream, final Charset charset)
			throws Exception {

		Objects.requireNonNull(bundleDto, "bundleDto");
		Objects.requireNonNull(outputStream, "outputStream");

		JSONCodec codec = new JSONCodec();
		Charset cs = charset != null ? charset : StandardCharsets.UTF_8;

		try (Encoder encoder = codec.enc().charset(cs).indent("\t").to(outputStream)) {

			encoder.put(bundleDto).flush();
		}
	}
}
