package aQute.bnd.maven.reporter.plugin.entries.mavenproject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.project.MavenProject;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.maven.reporter.plugin.MavenProjectWrapper;
import aQute.bnd.maven.reporter.plugin.ReportGeneratorFactory;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.bnd.service.reporter.ReportGeneratorService;
import aQute.lib.strings.Strings;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.generator.ReportGeneratorBuilder;
import biz.aQute.bnd.reporter.generator.ReportGeneratorConstants;
import biz.aQute.bnd.reporter.plugins.entries.bndworkspace.BndWorkspaceContentsPlugin;

/**
 * This plugins extracts the child project data of a maven project. The user can
 * set the {@link BndWorkspaceContentsPlugin#EXCLUDES_PROPERTY} to skip some
 * projects and the {@link BndWorkspaceContentsPlugin#USE_CONFIG_PROPERTY} to
 * the desired configuration name that will be used to generate the report of
 * the projects.
 */
@BndPlugin(name = "entry." + EntryNamesReference.PROJECTS)
public class MavenAggregatorConcentPlugin implements ReportEntryPlugin<MavenProjectWrapper>, Plugin {

	final public static String				USE_CONFIG_PROPERTY	= "useConfig";
	final public static String				EXCLUDES_PROPERTY	= "excludes";

	private Reporter						_reporter;
	private final Map<String, String>		_properties			= new HashMap<>();
	private final ReportGeneratorBuilder	_projectGeneratorBuilder;
	private final ReportGeneratorBuilder	_aggregatorGeneratorBuilder;

	public MavenAggregatorConcentPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.PROJECTS);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, MavenProjectWrapper.class.getCanonicalName());

		_projectGeneratorBuilder = ReportGeneratorFactory.create()
			.useCustomConfig()
			.withProjectDefaultPlugins()
			.addPlugin(EntryNamesReference.COMMON_INFO);

		_aggregatorGeneratorBuilder = ReportGeneratorFactory.create()
			.useCustomConfig()
			.withAggregatorProjectDefaultPlugins();
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
	public Object extract(final MavenProjectWrapper obj, final Locale locale) throws Exception {
		Objects.requireNonNull(obj, "obj");

		final List<Map<String, Object>> projectReports = new LinkedList<>();
		for (final MavenProject project : obj.getSubProjects()) {
			if (!getExcludes().contains(project.getBasedir()
				.getName())) {
				try (Processor processor = new Processor()) {
					processor.setBase(project.getBasedir());
					processor.addProperties(obj.getReportConfig());

					MavenProjectWrapper toAnalyze = new MavenProjectWrapper(obj.getProjects(), project);

					ReportGeneratorService generator = null;
					if (toAnalyze.isAggregator()) {
						generator = _aggregatorGeneratorBuilder.setProcessor(processor)
							.build();
					} else {
						generator = _projectGeneratorBuilder.setProcessor(processor)
							.build();
					}

					if (getConfigName() != null) {
						projectReports.add(generator.generateReportOf(toAnalyze, locale,
							"(" + ReportGeneratorConstants.CONFIG_NAME_PROPERTY + "=" + getConfigName() + ")"));
					} else {
						projectReports.add(generator.generateReportOf(toAnalyze, locale));
					}

					if (!processor.isOk()) {
						if (!processor.getErrors()
							.isEmpty()) {
							_reporter.error("Creating report of Project %s generates errors: %s", project.getName(),
								Strings.join(",", processor.getErrors()));
						}
						if (!processor.getWarnings()
							.isEmpty()) {
							_reporter.error("Creating report of Project %s generates warnings: %s", project.getName(),
								Strings.join(",", processor.getWarnings()));
						}
					}
				}
			}
		}

		if (!projectReports.isEmpty()) {
			return projectReports;
		} else {
			return null;
		}
	}
}
