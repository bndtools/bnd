package aQute.bnd.service.reporter;

import java.io.OutputStream;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This plugin serialize the extracted metadata DTO into a specific format.
 */
@ProviderType
public interface ReportSerializerPlugin {

	/**
	 * Get the set of file extension names corresponding to the format that this
	 * plugin can serialize into.
	 * 
	 * @return one or multiple extensions name, never {@code null}
	 */
	public String[] getHandledExtensions();

	/**
	 * Serialize the metadata DTO into the output stream.
	 * 
	 * @param metadata the metadata to serialize, must not be {@code null}
	 * @param output the output stream to write the serialization result, must
	 *            not be {@code null}
	 * @throws Exception if any errors occur during the serialization process
	 */
	public void serialize(Map<String,Object> metadata, OutputStream output) throws Exception;
}
