package aQute.bnd.service.reporter;

import java.util.Locale;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This plugin can generate a DTO report of an arbitrary object. One can
 * implements the {@link ReportEntryPlugin} plugin for a specific object type to
 * contribute to a report.
 */
@ProviderType
public interface ReportGeneratorService {

	/**
	 * Generate a DTO report of the source object.
	 *
	 * @param source the source object from which the report is generated, if
	 *            {@code null} an empty report is returned.
	 * @return the report, never {@code null}
	 */
	Map<String, Object> generateReportOf(Object source);

	/**
	 * Generate a DTO report of the source object.
	 *
	 * @param source the source object from which the report is generated, if
	 *            {@code null} an empty report is returned.
	 * @param filter a LDAP filter used to filter the {@link ReportEntryPlugin}
	 *            plugins which will contribute to the report, if not specified
	 *            all configured {@link ReportEntryPlugin} plugins that handle
	 *            the source object type will contribute.
	 * @return the report, never {@code null}
	 */
	Map<String, Object> generateReportOf(Object source, String filter);

	/**
	 * Generate a DTO report of the source object, data will be localized for
	 * the specified locale if any.
	 *
	 * @param source the source object from which the report is generated, if
	 *            {@code null} an empty report is returned.
	 * @param locale a locale to localized extracted data, if not specified data
	 *            will be unlocalized.
	 * @return the report, never {@code null}
	 */
	Map<String, Object> generateReportOf(Object source, Locale locale);

	/**
	 * Generate a DTO report of the source object, data will be localized for
	 * the specified locale if any.
	 *
	 * @param source the source object from which the report is generated, if
	 *            {@code null} an empty report is returned.
	 * @param locale a locale to localized extracted data, if not specified data
	 *            will be unlocalized.
	 * @param filter a LDAP filter used to filter the {@link ReportEntryPlugin}
	 *            plugins which will contribute to the report, if not specified
	 *            all configured {@link ReportEntryPlugin} plugins that handle
	 *            the source object type will contribute.
	 * @return the report, never {@code null}
	 */
	Map<String, Object> generateReportOf(Object source, Locale locale, String filter);
}
