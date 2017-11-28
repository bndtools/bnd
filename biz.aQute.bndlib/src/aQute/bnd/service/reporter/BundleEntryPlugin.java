package aQute.bnd.service.reporter;

import java.util.Locale;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;

/**
 * This plugin extracts a piece of information from a Jar and converts it into a
 * DTO representation. Its result will contributes to the formation of a Jar
 * metadata report.
 */
@ProviderType
public interface BundleEntryPlugin {

	/**
	 * Extracts a piece of information from the Jar in arguments.
	 * <p>
	 * If the Jar contains localized data, it will be extracted for the
	 * specified locale or a less specific if not found. The syntax of the
	 * locale must be as specified in {@link Locale}, an empty {@code String}
	 * means {@code unlocalized}.
	 * 
	 * @param jar the Jar to inspect, must not be {@code null}
	 * @param locale the {@code String} representation of a {@code Locale}, must
	 *            not be {@code null}
	 * @param reporter used to report error, must not be {@code null}
	 * @return a DTO representation or {@code null} if no data is available
	 */
	public Object extract(final Jar jar, final String locale, final Processor reporter) throws Exception;

	/**
	 * Get the entry name under which this plugin will contribute to the main
	 * report.
	 * 
	 * @return the entry name, never {@code null}
	 */
	public String getEntryName();
}
