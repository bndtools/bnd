package aQute.bnd.service.reporter;

import java.util.Locale;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This plugin extracts a piece of information (potentially localized) from a
 * source object and converts it into a DTO representation.
 * <p>
 * The {@link ReportGeneratorService} will select a collection of those plugins
 * and apply them on a source object. Each result will be put into a {@link Map}
 * under the corresponding {@link ReportEntryPlugin#ENTRY_NAME_PROPERTY}
 * property value. This final {@link Map} will constitute a report of the source
 * object. Plugins will be selected thanks to their
 * {@link ReportEntryPlugin#SOURCE_CLASS_PROPERTY} and optionally by any
 * provided properties.
 * <p>
 * Implementers: Implementers must define the
 * {@link ReportEntryPlugin#ENTRY_NAME_PROPERTY} and the
 * {@link ReportEntryPlugin#SOURCE_CLASS_PROPERTY}
 */
@ProviderType
public interface ReportEntryPlugin<T> {

	/**
	 * The entry name property under which the DTO value extracted by the
	 * {@link ReportEntryPlugin#extract(Object, Locale)} method is added to a
	 * report.
	 */
	String	ENTRY_NAME_PROPERTY		= "entryName";

	/**
	 * The class name of the source object that a {@link ReportEntryPlugin} can
	 * extract.
	 */
	String	SOURCE_CLASS_PROPERTY	= "sourceClass";

	/**
	 * Extracts a piece of information from the source in arguments.
	 * <p>
	 * If the source contains localized data, it will be extracted for the
	 * specified locale or a less specific if not found.
	 *
	 * @param source the source to inspect, must not be {@code null}
	 * @param locale the {@code String} representation of a {@code Locale}, must
	 *            not be {@code null}
	 * @return a DTO representation or {@code null} if no data is available
	 */
	Object extract(T source, Locale locale) throws Exception;

	/**
	 * @return a map of properties, never {@code null}
	 */
	Map<String, String> getProperties();
}
