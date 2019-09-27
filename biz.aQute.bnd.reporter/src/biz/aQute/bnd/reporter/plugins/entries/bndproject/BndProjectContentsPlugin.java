package biz.aQute.bnd.reporter.plugins.entries.bndproject;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.bnd.service.reporter.ReportGeneratorService;
import aQute.lib.strings.Strings;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.generator.ReportGeneratorBuilder;
import biz.aQute.bnd.reporter.generator.ReportGeneratorConstants;

/**
 * This plugins allows to extract all the bundles built by a bnd project. The
 * user can set the {@link BndProjectContentsPlugin#EXCLUDES_PROPERTY} to skip
 * some bundles and the {@link BndProjectContentsPlugin#USE_CONFIG_PROPERTY} to
 * the desired configuration name that will be used to generate the report of
 * the bundles.
 */
@BndPlugin(name = "entry." + EntryNamesReference.BUNDLES)
public class BndProjectContentsPlugin implements ReportEntryPlugin<Project>, Plugin {

	final public static String				USE_CONFIG_PROPERTY	= "useConfig";
	final public static String				EXCLUDES_PROPERTY	= "excludes";

	private Reporter						_reporter;
	private final Map<String, String>		_properties			= new HashMap<>();
	private final ReportGeneratorBuilder	_generatorBuilder;

	public BndProjectContentsPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.BUNDLES);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Project.class.getCanonicalName());

		_generatorBuilder = ReportGeneratorBuilder.create()
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

	private Set<String> getExcludes() {
		final Set<String> excludes = new HashSet<>();
		if (_properties.containsKey(EXCLUDES_PROPERTY)) {
			for (final String exclude : _properties.get(EXCLUDES_PROPERTY)
				.split(",")) {
				excludes.add(exclude.trim());
			}
		}
		return excludes;
	}

	private String getConfigName() {
		return _properties.get(USE_CONFIG_PROPERTY);
	}

	@Override
	public Object extract(final Project project, final Locale locale) throws Exception {
		Objects.requireNonNull(project, "project");

		final List<Map<String, Object>> bundlesReports = new LinkedList<>();

		try (final ProjectBuilder pb = project.getBuilder(null)) {
			final File[] jarFiles = project.getBuildFiles(false);
			if (jarFiles != null) {
				final List<Builder> builders = pb.getSubBuilders();

				for (final File jarFile : jarFiles) {
					try (final Jar jar = new Jar(jarFile)) {
						final Optional<Builder> opt = builders.stream()
							.filter(b -> {
								try {
									return b.getBsn()
										.equals(jar.getBsn());
								} catch (final Exception exception) {
									throw new RuntimeException(exception);
								}
							})
							.findAny();
						if (opt.isPresent()) {
							final Builder builder = opt.get();
							if (!getExcludes().contains(jar.getBsn())) {
								final ReportGeneratorService generator = _generatorBuilder.setProcessor(builder)
									.build();
								if (getConfigName() != null) {
									bundlesReports.add(generator.generateReportOf(jar, locale, "("
										+ ReportGeneratorConstants.CONFIG_NAME_PROPERTY + "=" + getConfigName() + ")"));
								} else {
									bundlesReports.add(generator.generateReportOf(jar, locale));
								}
								if (!builder.isOk()) {
									if (!builder.getErrors()
										.isEmpty()) {
										_reporter.error("Creating report of Jar %s generates errors: %s", jar.getBsn(),
											Strings.join(",", builder.getErrors()));
									}
									if (!builder.getWarnings()
										.isEmpty()) {
										_reporter.error("Creating report of Jar %s generates warnings: %s",
											jar.getBsn(), Strings.join(",", builder.getWarnings()));
									}
								}
							} else {
								builder.close();
							}
						}
					}
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
