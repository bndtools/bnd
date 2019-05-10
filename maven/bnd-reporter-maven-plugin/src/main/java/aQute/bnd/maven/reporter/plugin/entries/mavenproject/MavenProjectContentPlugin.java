package aQute.bnd.maven.reporter.plugin.entries.mavenproject;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.maven.reporter.plugin.MavenProjectWrapper;
import aQute.bnd.maven.reporter.plugin.ReportGeneratorFactory;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.bnd.service.reporter.ReportGeneratorService;
import aQute.lib.strings.Strings;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.generator.ReportGeneratorBuilder;
import biz.aQute.bnd.reporter.generator.ReportGeneratorConstants;
import biz.aQute.bnd.reporter.plugins.entries.bndproject.BndProjectContentsPlugin;

/**
 * This plugins extracts bundle data built by a maven project. The user can set
 * the {@link BndProjectContentsPlugin#USE_CONFIG_PROPERTY} to the desired
 * configuration name that will be used to generate the report of the bundle.
 */
@BndPlugin(name = "entry." + EntryNamesReference.BUNDLES)
public class MavenProjectContentPlugin implements ReportEntryPlugin<MavenProjectWrapper>, Plugin {

	private static final String				USE_CONFIG_PROPERTY	= "useConfig";
	private static final String				PACKAGING_JAR		= "jar";

	private Reporter						_reporter;
	private final Map<String, String>		_properties			= new HashMap<>();
	private final ReportGeneratorBuilder	_generatorBuilder;

	public MavenProjectContentPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.BUNDLES);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, MavenProjectWrapper.class.getCanonicalName());

		_generatorBuilder = ReportGeneratorFactory.create()
			.useCustomConfig()
			.withBundleDefaultPlugins();
	}

	@Override
	public void setReporter(final Reporter processor) {
		_reporter = processor;
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		_properties.putAll(map);
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(_properties);
	}

	private String getConfigName() {
		return _properties.get(USE_CONFIG_PROPERTY);
	}

	@Override
	public Object extract(final MavenProjectWrapper obj, final Locale locale) throws Exception {
		Objects.requireNonNull(obj, "obj");

		final List<Map<String, Object>> bundlesReports = new LinkedList<>();

		File bundleFile = new File(obj.getProject()
			.getBuild()
			.getDirectory(),
			obj.getProject()
				.getBuild()
				.getFinalName() + ".jar");

		if (PACKAGING_JAR.equals(obj.getProject()
			.getPackaging()) && bundleFile.exists()) {
			Processor processor = new Processor(obj.getReportConfig());
			Jar bundleJar = new Jar(bundleFile);

			final ReportGeneratorService generator = _generatorBuilder.setProcessor(processor)
				.build();
			if (getConfigName() != null) {
				bundlesReports.add(generator.generateReportOf(bundleJar, locale,
					"(" + ReportGeneratorConstants.CONFIG_NAME_PROPERTY + "=" + getConfigName() + ")"));
			} else {
				bundlesReports.add(generator.generateReportOf(bundleJar, locale));
			}
			if (!processor.isOk()) {
				if (!processor.getErrors()
					.isEmpty()) {
					_reporter.error("Creating report of Jar %s generates errors: %s", bundleJar.getBsn(),
						Strings.join(",", processor.getErrors()));
				}
				if (!processor.getWarnings()
					.isEmpty()) {
					_reporter.error("Creating report of Jar %s generates warnings: %s", bundleJar.getBsn(),
						Strings.join(",", processor.getWarnings()));
				}
			}
		}

		if (!bundlesReports.isEmpty()) {
			return bundlesReports;
		} else {
			return null;
		}
	}
}
