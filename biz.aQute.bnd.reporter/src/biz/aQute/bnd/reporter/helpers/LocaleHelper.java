package biz.aQute.bnd.reporter.helpers;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import aQute.bnd.osgi.Jar;

/**
 * An helper to extract localization data from a Jar.
 */
public class LocaleHelper {

	private final Locale							_locale;
	private final Map<String, Map<String, String>>	_localizations;

	private LocaleHelper(final Jar jar, final Locale locale, final String basePath) {
		_locale = locale;
		_localizations = extractLocalizations(jar, basePath);
	}

	private LocaleHelper() {
		_locale = Locale.forLanguageTag("und");
		_localizations = new HashMap<>();
	}

	/**
	 * Create a {@link LocaleHelper} if localization data are found at the
	 * specified path, otherwise return {@code null}.
	 *
	 * @param jar the jar containing the localization file, must not be
	 *            {@code null}
	 * @param locale the default locale for this helper, must not be
	 *            {@code null}
	 * @param basePath a base path in the Jar to the localization property files
	 *            without its extension and its locale suffix, must not be
	 *            {@code null}
	 * @return a {@code LocaleHelper} which contains localization data or
	 *         {@code null} if there is no localization data at the specified
	 *         path.
	 */
	public static LocaleHelper createIfPresent(final Jar jar, final Locale locale, final String basePath) {
		Objects.requireNonNull(jar, "jar");
		Objects.requireNonNull(locale, "locale");
		Objects.requireNonNull(basePath, "basePath");

		if (hasLocalization(jar, basePath)) {
			return new LocaleHelper(jar, locale, basePath);
		} else {
			return null;
		}
	}

	/**
	 * @return a {@code LocaleHelper} without localization data, never
	 *         {@code null}
	 */
	public static LocaleHelper empty() {
		return new LocaleHelper();
	}

	/**
	 * If the argument is a variable, its corresponding value will be returned
	 * for the default locale of this helper instance. Otherwise, the argument
	 * is returned.
	 * <p>
	 * Values will be search from the most specific to the less specific locale
	 * including unlocalized (empty locale) value.
	 *
	 * @param variableOrValue a variable (starting with '%') or a value, can be
	 *            {@code null}
	 * @return the localized value, can be {@code null}
	 */
	public String get(final String variableOrValue) {
		return get(variableOrValue, _locale);
	}

	/**
	 * If the argument is a variable, its corresponding value will be returned
	 * for the specified locale. Otherwise, the argument is returned.
	 * <p>
	 * Values will be search from the most specific to the less specific locale
	 * including unlocalized (empty locale) value.
	 *
	 * @param variableOrValue a variable (starting with '%') or a value, can be
	 *            {@code null}
	 * @param locale the locale, must not be {@code null}
	 * @return the localized value, can be {@code null}
	 */
	public String get(final String variableOrValue, final Locale locale) {
		Objects.requireNonNull(locale, "locale");

		if (variableOrValue != null) {
			if (variableOrValue.startsWith("%")) {
				Locale nextLocale = locale;
				final String variable = variableOrValue.substring(1);
				String result = null;

				do {
					if (_localizations.get(nextLocale.toString()) != null) {
						result = _localizations.get(nextLocale.toString())
							.get(variable);
					}
					nextLocale = computeNextLocale(nextLocale);
				} while (nextLocale != null && result == null);

				return result;
			} else {
				return variableOrValue;
			}
		} else {
			return null;
		}
	}

	private Locale computeNextLocale(final Locale nextLocale) {
		if (nextLocale.getVariant() != "") {
			return new Locale(nextLocale.getLanguage(), nextLocale.getCountry());
		}
		if (nextLocale.getCountry() != "") {
			return new Locale(nextLocale.getLanguage());
		}
		if (nextLocale.getLanguage() != "") {
			return new Locale("");
		}
		return null;
	}

	private static boolean hasLocalization(final Jar jar, final String path) {
		return jar.getResources()
			.keySet()
			.stream()
			.anyMatch(k -> k.startsWith(path));
	}

	private Map<String, Map<String, String>> extractLocalizations(final Jar jar, final String path) {
		final Map<String, Map<String, String>> result = new HashMap<>();

		jar.getResources()
			.entrySet()
			.stream()
			.filter(e -> e.getKey()
				.startsWith(path))
			.forEach(entry -> {
				String lang = entry.getKey()
					.substring(path.length())
					.replaceFirst("\\..*", "");
				if (lang.startsWith("_")) {
					lang = lang.substring(1);
				}

				try (InputStream inProp = entry.getValue()
					.openInputStream()) {
					final Properties prop = new Properties();
					prop.load(inProp);

					final Map<String, String> properties = new HashMap<>();
					for (final String key : prop.stringPropertyNames()) {
						properties.put(key, prop.getProperty(key));
					}

					result.put(lang, properties);
				} catch (final Exception e) {
					throw new RuntimeException("Unable to read localization data at path " + entry.getKey(), e);
				}
			});

		return result;
	}
}
