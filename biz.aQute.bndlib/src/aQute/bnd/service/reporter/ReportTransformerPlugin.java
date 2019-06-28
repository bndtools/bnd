package aQute.bnd.service.reporter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This plugin transform an input model by applying a template on it.
 */
@ProviderType
public interface ReportTransformerPlugin {

	/**
	 * Get the set of file extension names corresponding to the template format
	 * that this plugin support.
	 *
	 * @return one or multiple extensions name, never {@code null}
	 */
	String[] getHandledTemplateExtensions();

	/**
	 * Get the set of file extension names corresponding to the model format
	 * that this plugin support.
	 *
	 * @return one or multiple extensions name, never {@code null}
	 */
	String[] getHandledModelExtensions();

	/**
	 * Transform the model by applying the template on it and write the result
	 * to the output stream.
	 *
	 * @param model an input stream that contains the model, must not be
	 *            {@code null}
	 * @param template an input stream that contains the template, must not be
	 *            {@code null}
	 * @param output the output stream to write the transformation result, must
	 *            not be {@code null}
	 * @param parameters a map of parameters and their value that must be
	 *            provided to the template engine, must not be {@code null}
	 * @throws Exception if any errors occur during the transformation process
	 */
	void transform(InputStream model, InputStream template, OutputStream output, Map<String, String> parameters)
		throws Exception;
}
