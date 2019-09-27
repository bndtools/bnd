package biz.aQute.bnd.reporter.helpers;

import java.util.Locale;
import java.util.Objects;
import java.util.jar.Manifest;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

/**
 * An helper to extract manifest headers taking into account the localization.
 */
public class ManifestHelper {

	final private static String	DEFAULT_LOCALIZATION_BASE	= "OSGI-INF/l10n/bundle";

	private LocaleHelper		_loHelper;
	private final Manifest		_manifest;

	private ManifestHelper(final Jar jar, final Manifest manifest, final Locale locale) {

		_manifest = manifest;

		String path = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_LOCALIZATION);
		if (path == null || path.isEmpty()) {
			path = DEFAULT_LOCALIZATION_BASE;
		}

		_loHelper = LocaleHelper.createIfPresent(jar, locale, path);

		if (_loHelper == null) {
			_loHelper = LocaleHelper.empty();
		}
	}

	/**
	 * Create a {@link ManifestHelper} if the specified jar contains a manifest,
	 * otherwise return {@code null}.
	 *
	 * @param jar the jar, must not be {@code null}
	 * @param locale the locale, must not be {@code null}
	 * @return the helper or {@code null} if the jar does not contain a manifest
	 */
	public static ManifestHelper createIfPresent(final Jar jar, final Locale locale) {
		Objects.requireNonNull(jar, "jar");
		Objects.requireNonNull(locale, "locale");

		Manifest manifest = null;

		try {
			manifest = jar.getManifest();
		} catch (final Exception exception) {
			throw new RuntimeException("Unable to read the manifest of Jar " + jar.getName(), exception);
		}

		if (manifest != null) {
			return new ManifestHelper(jar, manifest, locale);
		} else {
			return null;
		}
	}

	/**
	 * Gets the header value for the locale of this helper instance.
	 * <p>
	 * Values will be search from the most specific to the less specific locale
	 * including unlocalized (empty locale) value.
	 *
	 * @param headerName the header to extract, must not be {@code null}
	 * @param allowDuplicateAttributes true if the header allow duplicate
	 *            attributes
	 * @return the header value, never {@code null}
	 */
	public Parameters getHeader(final String headerName, final boolean allowDuplicateAttributes) {
		Objects.requireNonNull(headerName, "headerName");

		final String headerString = _loHelper.get(_manifest.getMainAttributes()
			.getValue(headerName));
		if (headerString != null) {
			final Parameters result = new Parameters(allowDuplicateAttributes);
			OSGiHeader.parseHeader(headerString, null, result);

			return result;
		} else {
			return new Parameters();
		}
	}

	/**
	 * Gets the header value for the locale of this helper instance.
	 * <p>
	 * Values will be search from the most specific to the less specific locale
	 * including unlocalized (empty locale) value.
	 *
	 * @param headerName the header to extract, must not be {@code null}
	 * @return the header value, never {@code null}
	 */
	public String getHeaderAsString(final String headerName) {
		Objects.requireNonNull(headerName, "headerName");

		final String headerString = _loHelper.get(_manifest.getMainAttributes()
			.getValue(headerName));
		if (headerString != null) {
			return headerString;
		} else {
			return "";
		}
	}
}
