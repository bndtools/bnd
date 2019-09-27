package biz.aQute.bnd.reporter.exporter;

import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ReportExporterService;
import aQute.bnd.service.reporter.ReportGeneratorService;
import biz.aQute.bnd.reporter.generator.ReportGeneratorBuilder;
import biz.aQute.bnd.reporter.plugins.resource.converter.JsonConverterPlugin;
import biz.aQute.bnd.reporter.plugins.resource.converter.ManifestConverterPlugin;
import biz.aQute.bnd.reporter.plugins.resource.converter.PropertiesConverterPlugin;
import biz.aQute.bnd.reporter.plugins.resource.converter.XmlConverterPlugin;
import biz.aQute.bnd.reporter.plugins.serializer.JsonReportSerializerPlugin;
import biz.aQute.bnd.reporter.plugins.serializer.XmlReportSerializerPlugin;
import biz.aQute.bnd.reporter.plugins.transformer.JtwigTransformerPlugin;
import biz.aQute.bnd.reporter.plugins.transformer.XsltTransformerPlugin;

/**
 * Build the {@link ReportExporterService}.
 */
public class ReportExporterBuilder {
	private String					_scope		= null;
	private Processor				_processor	= null;
	private ReportGeneratorService	_generator	= null;

	private ReportExporterBuilder() {}

	/**
	 * Create a new builder.
	 *
	 * @return the builder
	 */
	public static ReportExporterBuilder create() {
		return new ReportExporterBuilder();
	}

	/**
	 * Set a scope for the exporter service. If set, the exporter will ignore
	 * report instructions with a different scope.
	 *
	 * @param scopeName the scope name, may be {@code null}
	 * @return the builder
	 */
	public ReportExporterBuilder setScope(final String scopeName) {
		_scope = scopeName;

		return this;
	}

	/**
	 * Set the processor from which plugins are loaded as well as properties and
	 * to which errors are reported.
	 * <p>
	 * If not set, a default processor is used but the created service will be
	 * useless because no instructions will be provided.
	 *
	 * @param processor the processor to use.
	 * @return the builder
	 */
	public ReportExporterBuilder setProcessor(final Processor processor) {
		_processor = processor;

		return this;
	}

	/**
	 * Set the generator which will be used to generate the reports.
	 * <p>
	 * If not set a default generator will be used.
	 *
	 * @param generator the generator service to use
	 * @return the builder
	 */
	public ReportExporterBuilder setGenerator(final ReportGeneratorService generator) {
		_generator = generator;

		return this;
	}

	/**
	 * Build the service.
	 *
	 * @return the service
	 */
	public ReportExporterService build() {
		final String scope = _scope != null ? _scope : "";
		final Processor processor = _processor != null ? _processor : new Processor();
		final ReportGeneratorService generator = _generator != null ? _generator
			: ReportGeneratorBuilder.create()
				.setProcessor(processor)
				.build();

		setupDefaultPlugins(processor);

		return new ReportExporter(scope, processor, generator);
	}

	private static void setupDefaultPlugins(final Processor processor) {
		if (processor.getPlugin(ManifestConverterPlugin.class) == null) {
			processor.addBasicPlugin(new ManifestConverterPlugin());
		}
		if (processor.getPlugin(PropertiesConverterPlugin.class) == null) {
			processor.addBasicPlugin(new PropertiesConverterPlugin());
		}
		if (processor.getPlugin(JsonConverterPlugin.class) == null) {
			processor.addBasicPlugin(new JsonConverterPlugin());
		}
		if (processor.getPlugin(XmlConverterPlugin.class) == null) {
			processor.addBasicPlugin(new XmlConverterPlugin());
		}
		if (processor.getPlugin(JsonReportSerializerPlugin.class) == null) {
			processor.addBasicPlugin(new JsonReportSerializerPlugin());
		}
		if (processor.getPlugin(XmlReportSerializerPlugin.class) == null) {
			processor.addBasicPlugin(new XmlReportSerializerPlugin());
		}
		if (processor.getPlugin(JtwigTransformerPlugin.class) == null) {
			processor.addBasicPlugin(new JtwigTransformerPlugin());
		}
		if (processor.getPlugin(XsltTransformerPlugin.class) == null) {
			processor.addBasicPlugin(new XsltTransformerPlugin());
		}
	}
}
