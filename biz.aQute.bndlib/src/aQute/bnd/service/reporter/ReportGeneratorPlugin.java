package aQute.bnd.service.reporter;

import java.util.Locale;

import aQute.bnd.osgi.Jar;
import aQute.service.reporter.Reporter;

/**
 * This plugin extracts a piece information from a Jar and converts it into an
 * XML representation.
 * <p>
 * This plugin is called on a completely built Jar, and its result contributes
 * to the formation of an XML report of the Jar.
 */
public interface ReportGeneratorPlugin {

	/**
	 * Extracts a piece of information from the Jar in arguments.
	 * <p>
	 * If the Jar contains localized data, it will be extracted for the specified
	 * locale or a less specific if not found. The syntax of the locale must be as
	 * specified in {@link Locale}, an empty {@code String} means
	 * {@code unlocalized}.
	 * 
	 * @param jar the Jar to inspect, must not be {@code null}
	 * @param locale the {@code String} representation of a {@code Locale}, must not
	 *            be {@code null}
	 * @return the XML representations of the extracted data, must not be
	 *         {@code null}
	 */
	public XmlReportPart report(final Jar jar, final String locale, Reporter reporter);
}
