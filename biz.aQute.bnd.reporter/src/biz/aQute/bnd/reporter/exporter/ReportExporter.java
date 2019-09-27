package biz.aQute.bnd.reporter.exporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import aQute.bnd.header.Attrs;
import aQute.bnd.help.SyntaxAnnotation;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.reporter.ReportExporterService;
import aQute.bnd.service.reporter.ReportGeneratorService;
import aQute.bnd.service.reporter.ReportSerializerPlugin;
import aQute.bnd.service.reporter.ReportTransformerPlugin;
import aQute.lib.io.IO;
import biz.aQute.bnd.reporter.exporter.ReportExporter.ReportExporterInstructions.ExportReportInstruction;
import biz.aQute.bnd.reporter.generator.ReportGeneratorConstants;
import biz.aQute.bnd.reporter.helpers.ArrayHelper;
import biz.aQute.bnd.reporter.helpers.FileHelper;

/**
 * Provide the ReportExporterService service. This implementation is configured
 * by the user with the
 * {@link ReportExporterConstants#EXPORT_REPORT_INSTRUCTION} instruction.
 */
class ReportExporter implements ReportExporterService {

	private final Processor					_processor;

	private final String					_scope;

	private final ReportGeneratorService	_generator;

	private final Cache						_cache	= new Cache();

	ReportExporter(final String scope, final Processor processor, final ReportGeneratorService generator) {
		_scope = scope;
		_processor = processor;
		_generator = generator;
	}

	@Override
	public List<String> getAvailableReportsOf(final Object source) {
		return getScopedExportInstruction().keySet()
			.stream()
			.map(f -> IO.getFile(_processor.getBase(), f)
				.getPath())
			.collect(Collectors.toList());
	}

	@Override
	public Map<String, Resource> exportReportsOf(final Object source) {
		final Map<String, Resource> reportResources = new HashMap<>();

		final Map<String, ExportReportInstruction> instructions = getScopedExportInstruction();
		instructions.entrySet()
			.forEach(config -> {
				final File destination = IO.getFile(_processor.getBase(),
					Processor.removeDuplicateMarker(config.getKey()));
				final ExportReportInstruction instruction = config.getValue();
				final Map.Entry<String, Resource> templateResource = getTemplateResource(instruction, destination,
					_processor.getBase());

				if (templateResource != null) {
					reportResources.put(destination.getPath(),
						new EmbeddedResource(transform(source, instruction.locale()
							.orElse(""),
							config.getValue()
								.configName()
								.orElse(""),
							templateResource.getValue(), templateResource.getKey(), instruction.parameters()), 0L));
				} else {
					reportResources.put(destination.getPath(),
						new EmbeddedResource(getGeneratedReport(source, config.getValue()
							.locale()
							.orElse(""),
							config.getValue()
								.configName()
								.orElse(""),
							FileHelper.getExtension(destination)), 0L));
				}
			});
		return reportResources;
	}

	/*
	 * decide, if any, the template file to use for a transformation, look first
	 * for a file with the same name of the destination file in the destination
	 * file folder but with a different extension (this allow user to simply add
	 * a ./readme.twig if they want to generate a readme.md), then any template
	 * file declared in the instruction, else return null.
	 */
	private Map.Entry<String, Resource> getTemplateResource(final ExportReportInstruction instruction,
		final File destination, final File base) {
		File templateFile = null;
		Resource templateResource = null;
		String templateExtension = null;

		templateFile = FileHelper.searchSiblingWithDifferentExtension(destination, getAvailableTransformerExtensions());
		if (templateFile == null && instruction.template()
			.isPresent()) {
			templateFile = IO.getFile(base, instruction.template()
				.get());
		}

		if (templateFile != null) {
			if (templateFile.isFile()) {
				try {
					templateResource = new FileResource(templateFile);
					templateExtension = FileHelper.getExtension(templateFile);
					templateExtension = instruction.templateType()
						.orElse(templateExtension);
					return new AbstractMap.SimpleEntry<>(templateExtension, templateResource);
				} catch (final IOException e) {
					final String tPpath = templateFile.getPath();
					_processor.exception(e, "Failed to read the template file at %s", tPpath);
					return null;
				}
			} else if (instruction.template()
				.isPresent()
				&& instruction.template()
					.get()
					.startsWith("default:")) {
				try {
					InputStream embeddedTemplate = this.getClass()
						.getClassLoader()
						.getResourceAsStream(
							"biz/aQute/bnd/reporter/plugins/transformer/templates/" + instruction.template()
							.get()
							.substring(8));

					if (embeddedTemplate == null) {
						throw new IOException("Resource " + instruction.template()
							.get() + " not found.");
					}

					templateResource = new EmbeddedResource(IO.read(embeddedTemplate),
						0L);
					templateExtension = FileHelper.getExtension(templateFile);
					templateExtension = instruction.templateType()
						.orElse(templateExtension);
					return new AbstractMap.SimpleEntry<>(templateExtension, templateResource);
				} catch (final IOException e) {
					_processor.exception(e, "Failed to read the embedded template file at %s", instruction.template()
						.get());
					return null;
				}
			} else {
				try {
					final URL url;
					if (instruction.template()
						.isPresent()) {
						url = new URL(instruction.template()
							.get());
					} else {
						url = templateFile.toURI()
							.toURL();
					}
					templateResource = Resource.fromURL(url);
					templateExtension = FileHelper.getExtension(templateFile);
					templateExtension = instruction.templateType()
						.orElse(templateExtension);
					return new AbstractMap.SimpleEntry<>(templateExtension, templateResource);
				} catch (final IOException e) {
					final String tPpath = templateFile.getPath();
					_processor.exception(e, "Failed to read the template file at %s", tPpath);
					return null;
				}
			}
		} else {
			return null;
		}
	}

	/* transform a report using a template engine */
	private byte[] transform(final Object source, final String locale, final String configName,
		final Resource templateResource, final String templateExtension, final Attrs parameters) {
		final String[] serializerExtensions = getAvailableSerializerExtensions();

		final ReportTransformerPlugin plugin = _processor.getPlugins(ReportTransformerPlugin.class)
			.stream()
			.filter(p -> ArrayHelper.containsIgnoreCase(p.getHandledTemplateExtensions(), templateExtension)
				&& ArrayHelper.oneInBoth(p.getHandledModelExtensions(), serializerExtensions) != null)
			.findFirst()
			.orElse(null);

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		if (plugin != null) {
			final String serializationExtension = ArrayHelper.oneInBoth(plugin.getHandledModelExtensions(),
				serializerExtensions);

			try (InputStream isT = templateResource.openInputStream()) {
				try {
					plugin.transform(
						new ByteArrayInputStream(
							getGeneratedReport(source, locale, configName, serializationExtension)),
						isT, bos, parameters);
				} catch (final Exception e) {
					_processor.exception(e, "Failed to transform the report");
				}
			} catch (final Exception e1) {
				_processor.exception(e1, "Failed to read to the template file");
			}
		} else {
			_processor.error("Unable to find a template processor of type %s, available template types are: %s",
				templateExtension, getAvailableTransformerExtensions());
		}
		return bos.toByteArray();
	}

	/*
	 * for optimization purpose, return a cached DTO report or compute a new one
	 */
	private Map<String, Object> getGeneratedReport(final Object source, final String locale, final String configName) {
		if (configName.equals("")) {
			return _cache.reportsDTOCache.computeIfAbsent(locale + configName,
				l -> _generator.generateReportOf(source, Locale.forLanguageTag(l)));
		} else {
			return _cache.reportsDTOCache.computeIfAbsent(locale + configName,
				l -> _generator.generateReportOf(source, Locale.forLanguageTag(l),
					"(" + ReportGeneratorConstants.CONFIG_NAME_PROPERTY + "=" + configName + ")"));
		}
	}

	/*
	 * for optimization purpose, return a cached serialized report or compute a
	 * new one
	 */
	private byte[] getGeneratedReport(final Object source, final String locale, final String configName,
		final String extension) {
		return _cache.reportsByteCache.computeIfAbsent(locale + configName + extension,
			k -> serializeReport(getGeneratedReport(source, locale, configName), extension));
	}

	/*
	 * perform the serialization of a report given the wanted resulting file
	 * extension (which specify the serialization format)
	 */
	private byte[] serializeReport(final Map<String, Object> reportDTO, final String extension) {
		final ReportSerializerPlugin plugin = _processor.getPlugins(ReportSerializerPlugin.class)
			.stream()
			.filter(p -> ArrayHelper.containsIgnoreCase(p.getHandledExtensions(), extension))
			.findFirst()
			.orElse(null);

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		if (plugin != null) {
			try {
				plugin.serialize(reportDTO, bos);
			} catch (final Exception e) {
				_processor.exception(e, "Failed to serialize the report to %s", extension);
			}
		} else {
			_processor.error("Unable to serialize the report to %s, available types are: %s", extension,
				getAvailableSerializerExtensions());
		}
		return bos.toByteArray();
	}

	/*
	 * return all the file extensions to which a report can actually be
	 * serialize into the corresponding format.
	 */
	private String[] getAvailableSerializerExtensions() {
		return _processor.getPlugins(ReportSerializerPlugin.class)
			.stream()
			.flatMap(r -> Arrays.stream(r.getHandledExtensions()))
			.collect(Collectors.toList())
			.toArray(new String[0]);
	}

	/*
	 * return all the template file extensions corresponding to the currently
	 * available template engine and accepted template file format.
	 */
	private String[] getAvailableTransformerExtensions() {
		return _processor.getPlugins(ReportTransformerPlugin.class)
			.stream()
			.flatMap(r -> Arrays.stream(r.getHandledTemplateExtensions()))
			.collect(Collectors.toList())
			.toArray(new String[0]);
	}

	/*
	 * if this export has a scope, return all the exportreport instructions that
	 * do not define a scope or the same scope as the exporter scope.
	 */
	private Map<String, ExportReportInstruction> getScopedExportInstruction() {
		if (_scope.isEmpty()) {
			return _processor.getInstructions(ReportExporterInstructions.class)
				.exportreport();
		} else {
			final Map<String, ExportReportInstruction> instructions = new HashMap<>();
			_processor.getInstructions(ReportExporterInstructions.class)
				.exportreport()
				.entrySet()
				.stream()
				.filter(e -> e.getValue()
					.scope()
					.orElse(_scope)
					.equals(_scope))
				.forEach(e -> instructions.put(e.getKey(), e.getValue()));
			return instructions;
		}
	}

	interface ReportExporterInstructions {

		interface ExportReportInstruction {

			Optional<String> locale();

			Optional<String> template();

			Optional<String> templateType();

			Attrs parameters();

			Optional<String> configName();

			Optional<String> scope();
		}

		@SyntaxAnnotation
		Map<String, ExportReportInstruction> exportreport();
	}

	class Cache {
		public Map<String, Map<String, Object>>	reportsDTOCache		= new HashMap<>();
		public Map<String, byte[]>				reportsByteCache	= new HashMap<>();
	}
}
