package biz.aQute.bnd.reporter.generator;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import aQute.bnd.service.Registry;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.bnd.service.reporter.ReportGeneratorService;
import aQute.lib.filter.Filter;
import aQute.service.reporter.Reporter;

class ReportGenerator implements ReportGeneratorService {

	private final Registry	_registry;

	private final Reporter	_reporter;

	ReportGenerator(final Registry registry, final Reporter reporter) {
		_registry = registry;
		_reporter = reporter;
	}

	@Override
	public Map<String, Object> generateReportOf(final Object source) {
		return generateReportOf(source, null, null);
	}

	@Override
	public Map<String, Object> generateReportOf(final Object source, final String filter) {
		return generateReportOf(source, null, filter);
	}

	@Override
	public Map<String, Object> generateReportOf(final Object source, final Locale locale) {
		return generateReportOf(source, locale, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> generateReportOf(final Object source, final Locale locale, final String filter) {

		final Map<String, Object> report = new HashMap<>();
		if (source != null) {
			try {
				final Locale myLocale = locale != null ? locale : Locale.forLanguageTag("und");
				final Filter myFilter = filter != null && !filter.isEmpty() ? new Filter(filter, true) : null;

				/*
				 * we filter ReportEntryPlugins that can handle the source
				 * object, that defines a non empty value for the
				 * ENTRY_NAME_PROPERTY property (silently ignored if not) and
				 * then that matches the optional filter
				 */
				_registry.getPlugins(ReportEntryPlugin.class)
					.stream()
					.filter(p -> checkSourceType(p, source))
					.filter(this::checkEntryName)
					.filter(p -> checkFilters(p, myFilter))
					.forEach(p -> {
						try {
							/*
							 * extracted DTO is added to the report if not null
							 */
							final Object extracted = p.extract(source, myLocale);
							if (extracted != null) {
								report.put((String) p.getProperties()
									.get(ReportEntryPlugin.ENTRY_NAME_PROPERTY), extracted);
							}
						} catch (final Exception e) {
							_reporter.exception(e, "Failed to report %s  entry for the source: %s", p.getProperties()
								.get(ReportEntryPlugin.ENTRY_NAME_PROPERTY), source);
						}
					});
			} catch (final Exception e) {
				_reporter.exception(e, "Failed to report the source: %s", source);
			}
		}
		return report;
	}

	/*
	 * check if the plugin properties match the filter
	 */
	private boolean checkFilters(final ReportEntryPlugin<?> p, final Filter filter) {
		try {
			return filter != null ? filter.matchMap(p.getProperties()) : true;
		} catch (final Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	/*
	 * check if the current plugin defines the SOURCE_CLASS_PROPERTY property
	 * with a non empty value and that the source object can be cast to this
	 * type
	 */
	private boolean checkSourceType(final ReportEntryPlugin<?> plugin, final Object source) {
		final String className = plugin.getProperties()
			.get(ReportEntryPlugin.SOURCE_CLASS_PROPERTY);
		if (className != null && !className.isEmpty()) {
			try {
				return Class.forName(className)
					.isInstance(source);
			} catch (final ClassNotFoundException exception) {
				throw new RuntimeException(exception);
			}
		} else {
			return false;
		}
	}

	/*
	 * check if the plugin define a non empty value for the ENTRY_NAME_PROPERTY
	 * property
	 */
	private boolean checkEntryName(final ReportEntryPlugin<?> plugin) {
		final String entryName = plugin.getProperties()
			.get(ReportEntryPlugin.ENTRY_NAME_PROPERTY);
		if (entryName != null && !entryName.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}
}
