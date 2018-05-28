package aQute.bnd.service.reporter;

import java.io.InputStream;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This plugin deserialize a stream of a specific format into a DTO
 * representation.
 */
@ProviderType
public interface ReportImportDeserializerPlugin {

	/**
	 * Get the set of file extension names corresponding to the format that this
	 * plugin can deserialize.
	 * 
	 * @return one or multiple extensions name, never {@code null}
	 */
	public String[] getHandledExtensions();

	/**
	 * Deserialize the input stream into a DTO representation.
	 * 
	 * @param input the stream to deserialize, must not be {@code null}
	 * @return a DTO representation of the input stream content or {@code null}
	 *         if the stream is empty
	 * @throws Exception if any errors occur during the deserialization process
	 */
	public Object deserialyze(InputStream input) throws Exception;
}
