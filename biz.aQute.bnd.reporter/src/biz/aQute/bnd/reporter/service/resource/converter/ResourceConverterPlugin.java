package biz.aQute.bnd.reporter.service.resource.converter;

import java.io.InputStream;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This plugin extract data form an InputStream of a specific format into a DTO
 * representation.
 */
@ProviderType
public interface ResourceConverterPlugin {

	/**
	 * Get the set of file extension names corresponding to the format that this
	 * plugin can handle.
	 *
	 * @return one or multiple extensions name, never {@code null}
	 */
	String[] getHandledExtensions();

	/**
	 * Extract data from the input stream into a DTO representation.
	 *
	 * @param input the stream to extract, must not be {@code null}
	 * @return a DTO representation of the input stream content or {@code null}
	 *         if the stream is empty
	 * @throws Exception if any errors occur during the extraction process
	 */
	Object extract(InputStream input) throws Exception;
}
