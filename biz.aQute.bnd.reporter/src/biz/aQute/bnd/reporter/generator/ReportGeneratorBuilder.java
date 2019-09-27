package biz.aQute.bnd.reporter.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ReportGeneratorService;
import biz.aQute.bnd.reporter.plugins.entries.any.AnyEntryPlugin;
import biz.aQute.bnd.reporter.plugins.entries.any.ImportResourcePlugin;
import biz.aQute.bnd.reporter.plugins.entries.bndproject.BndProjectContentsPlugin;
import biz.aQute.bnd.reporter.plugins.entries.bndproject.CodeSnippetPlugin;
import biz.aQute.bnd.reporter.plugins.entries.bndproject.CommonInfoProjectPlugin;
import biz.aQute.bnd.reporter.plugins.entries.bndworkspace.BndWorkspaceContentsPlugin;
import biz.aQute.bnd.reporter.plugins.entries.bndworkspace.CommonInfoPlugin;
import biz.aQute.bnd.reporter.plugins.entries.bundle.ComponentsPlugin;
import biz.aQute.bnd.reporter.plugins.entries.bundle.ImportJarResourcePlugin;
import biz.aQute.bnd.reporter.plugins.entries.bundle.ManifestPlugin;
import biz.aQute.bnd.reporter.plugins.entries.bundle.MavenCoordinatePlugin;
import biz.aQute.bnd.reporter.plugins.entries.bundle.MetatypesPlugin;
import biz.aQute.bnd.reporter.plugins.entries.processor.FileNamePlugin;

/**
 * Build the {@link ReportGeneratorService}.
 */
public class ReportGeneratorBuilder {

	private Processor					_processor			= null;
	private final Map<String, String>	_pluginNames		= new HashMap<>();
	private final List<String>			_plugins			= new ArrayList<>();
	private boolean						_useCustomConfig	= false;

	private ReportGeneratorBuilder() {
		registerDefaultPlugins();
	}

	/**
	 * Create a new builder.
	 *
	 * @return the builder
	 */
	public static ReportGeneratorBuilder create() {
		return new ReportGeneratorBuilder();
	}

	/**
	 * Set the processor from which plugins are loaded as well as properties and
	 * to which errors are reported.
	 *
	 * @param processor the processor to use.
	 * @return the builder
	 */
	public ReportGeneratorBuilder setProcessor(final Processor processor) {
		_processor = processor;

		return this;
	}

	/**
	 * If set, the {@link ReportGeneratorConstants#REPORT_CONFIG_INSTRUCTION}
	 * instruction will be processed to customize the report generation.
	 *
	 * @return the builder
	 */
	public ReportGeneratorBuilder useCustomConfig() {
		_useCustomConfig = true;

		return this;
	}

	/**
	 * Register a plugin to be used by the
	 * {@link ReportGeneratorConstants#REPORT_CONFIG_INSTRUCTION} and to be able
	 * to use plugin short names.
	 *
	 * @param className the class name of the plugin.
	 * @return the builder
	 */
	public ReportGeneratorBuilder registerPlugin(final String className) {
		Objects.requireNonNull("className", className);

		final String shortName = findPluginName(className);
		if (!shortName.equals("")) {
			_pluginNames.put(shortName, className);
		}

		return this;
	}

	private ReportGeneratorBuilder registerPluginIfAbsent(final String className) {
		Objects.requireNonNull("className", className);

		final String shortName = findPluginName(className);
		if (!shortName.equals("") && !_pluginNames.containsKey(shortName)) {
			_pluginNames.put(shortName, className);
		}

		return this;
	}

	/**
	 * Add a plugin to be used as default plugins for custom configuration or in
	 * addition to plugins defined by the processor.
	 *
	 * @param classNameOrPluginName a class name or the short name of a plugin.
	 * @return the builder
	 */
	public ReportGeneratorBuilder addPlugin(final String classNameOrPluginName) {
		Objects.requireNonNull("classNameOrPluginName", classNameOrPluginName);

		registerPlugin(classNameOrPluginName);
		_plugins.add(classNameOrPluginName);

		return this;
	}

	/**
	 * Add the default plugins used to extract bundle data.
	 *
	 * @return the builder
	 */
	public ReportGeneratorBuilder withBundleDefaultPlugins() {

		addPlugin(EntryNamesReference.COMPONENTS);
		addPlugin(EntryNamesReference.MANIFEST);
		addPlugin(EntryNamesReference.MAVEN_COORDINATE);
		addPlugin(EntryNamesReference.METATYPES);

		return this;
	}

	/**
	 * Add the default plugins used to extract project data.
	 *
	 * @return the builder
	 */
	public ReportGeneratorBuilder withProjectDefaultPlugins() {

		addPlugin(EntryNamesReference.FILE_NAME);
		addPlugin(EntryNamesReference.BUNDLES);
		addPlugin(EntryNamesReference.CODE_SNIPPETS);

		/*
		 * Instead of this maybe we should allow multiple plugin impl to be
		 * registered.
		 */
		registerPluginIfAbsent(CommonInfoProjectPlugin.class.getCanonicalName());
		addPlugin(EntryNamesReference.COMMON_INFO);

		return this;
	}

	/**
	 * Add the default plugins used to extract aggregator project or workspace
	 * data.
	 *
	 * @return the builder
	 */
	public ReportGeneratorBuilder withAggregatorProjectDefaultPlugins() {

		addPlugin(EntryNamesReference.FILE_NAME);
		addPlugin(EntryNamesReference.PROJECTS);

		/*
		 * Instead of this maybe we should allow multiple plugin impl to be
		 * registered.
		 */
		registerPluginIfAbsent(CommonInfoPlugin.class.getCanonicalName());
		addPlugin(EntryNamesReference.COMMON_INFO);

		return this;
	}

	/**
	 * Build the service.
	 *
	 * @return the service
	 */
	public ReportGeneratorService build() {
		final Processor processor = _processor != null ? _processor : new Processor();

		return new ReportGenerator(configureProcessor(processor), processor);
	}

	private String findPluginName(final String className) {
		try {
			final Class<?> pluginClass = Class.forName(className);
			if (pluginClass != null) {
				final BndPlugin annotation = pluginClass.getAnnotation(BndPlugin.class);
				if (annotation != null) {
					final String name = annotation.name() != null ? annotation.name() : "";
					if (name.startsWith("entry.")) {
						return name.substring(6);
					} else {
						return name;
					}
				}
			}
			return "";
		} catch (@SuppressWarnings("unused")
		final ClassNotFoundException exception) {
			/* this exception will be reported later by the processor */
			return "";
		}
	}

	private void registerDefaultPlugins() {
		registerPlugin(AnyEntryPlugin.class.getCanonicalName());
		registerPlugin(ImportResourcePlugin.class.getCanonicalName());
		registerPlugin(ImportJarResourcePlugin.class.getCanonicalName());
		registerPlugin(BndProjectContentsPlugin.class.getCanonicalName());
		registerPlugin(FileNamePlugin.class.getCanonicalName());
		registerPlugin(BndWorkspaceContentsPlugin.class.getCanonicalName());
		registerPlugin(MavenCoordinatePlugin.class.getCanonicalName());
		registerPlugin(ComponentsPlugin.class.getCanonicalName());
		registerPlugin(ImportJarResourcePlugin.class.getCanonicalName());
		registerPlugin(ManifestPlugin.class.getCanonicalName());
		registerPlugin(MetatypesPlugin.class.getCanonicalName());
		registerPlugin(CodeSnippetPlugin.class.getCanonicalName());
	}

	private Processor configureProcessor(final Processor processor) {
		if (!_useCustomConfig) {
			if (!_plugins.isEmpty()) {
				final Processor configuredProcessor = new Processor(processor);

				configuredProcessor.setProperty("-plugin.AutoDefaultReportConfig", generateDefaultPlugins().toString());
				configuredProcessor.getPlugins();
				processor.getInfo(configuredProcessor);

				return configuredProcessor;
			} else {
				return processor;
			}
		} else {
			final Processor configuredProcessor = new Processor(processor);

			configuredProcessor.setProperty("-plugin.AutoReportConfig", readPluginsConfig(processor).toString());
			configuredProcessor.setProperty("-plugin.AutoDefaultReportConfig", generateDefaultPlugins().toString());
			configuredProcessor.getPlugins();
			processor.getInfo(configuredProcessor);

			return configuredProcessor;
		}
	}

	private Parameters generateDefaultPlugins() {
		final Parameters configuration = new Parameters(true);

		_plugins.forEach(s -> {
			final String key = removeDuplicateMarker(s);
			configuration.add(_pluginNames.getOrDefault(key, key), new Attrs());
		});

		return configuration;
	}

	private Parameters readPluginsConfig(final Processor processor) {
		final Parameters configuration = new Parameters(true);

		/*
		 * we iterate over all the reportconfig instructions to setup each
		 * configuration they describe
		 */
		processor.getPropertyKeys(true)
			.stream()
			.filter(k -> k.toString()
				.startsWith(ReportGeneratorConstants.REPORT_CONFIG_INSTRUCTION + "."))
			.forEach(k -> {
				/*
				 * the name of the configuration is the remaining part after the
				 * reportconfig instructions. Eg; 'reportconfig.myname' create a
				 * configuration named 'myname'
				 */
				final String configName = k.substring(ReportGeneratorConstants.REPORT_CONFIG_INSTRUCTION.length() + 1);
				final Parameters p = processor
					.getParameters(ReportGeneratorConstants.REPORT_CONFIG_INSTRUCTION + "." + configName);

				final List<String> defaultPlugins = new ArrayList<>(_plugins);
				/*
				 * if the clearDefaults property is present we do not add them
				 * to the current configuration
				 */
				if (p.containsKey(ReportGeneratorConstants.CLEAR_DEFAULTS_PROPERTY)) {
					p.remove(ReportGeneratorConstants.CLEAR_DEFAULTS_PROPERTY);
					defaultPlugins.clear();
				}

				/* we setup the plugins from the current instruction */
				p.forEach((q, v) -> {
					String key = removeDuplicateMarker(q);

					/* default plugins are overridden */
					defaultPlugins.remove(key);
					key = _pluginNames.getOrDefault(key, key);
					defaultPlugins.remove(key);
					v.put(ReportGeneratorConstants.CONFIG_NAME_PROPERTY, configName);
					configuration.add(key, v);
				});

				/*
				 * we add the remaining defaults plugins to the current
				 * configuration
				 */
				defaultPlugins.forEach(s -> {
					final Attrs attr = new Attrs();
					attr.put(ReportGeneratorConstants.CONFIG_NAME_PROPERTY, configName);
					configuration.add(_pluginNames.getOrDefault(s, s), attr);
				});
			});

		return configuration;
	}

	private String removeDuplicateMarker(final String key) {
		int i = key.length() - 1;
		while (i >= 0 && key.charAt(i) == '~') {
			--i;
		}

		return key.substring(0, i + 1);
	}
}
