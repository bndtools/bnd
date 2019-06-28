package aQute.bnd.service.reporter;

import java.io.OutputStream;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This plugin serialize the extracted DTO report into a specific format.
 */
@ProviderType
public interface ReportSerializerPlugin {

	/**
	 * Get the set of file extension names corresponding to the format that this
	 * plugin can serialize to.
	 *
	 * @return one or multiple extensions name, never {@code null}
	 */
	String[] getHandledExtensions();

	/**
	 * Serialize the DTO report into the output stream.
	 *
	 * @param reportDTO the DTO report to serialize, must not be {@code null}
	 * @param output the output stream to write the serialization result, must
	 *            not be {@code null}
	 * @throws Exception if any errors occur during the serialization process
	 */
	void serialize(Map<String, Object> reportDTO, OutputStream output) throws Exception;
}
