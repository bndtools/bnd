package biz.aQute.bnd.reporter.plugins.entries.bndworkspace;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.bnd.service.reporter.ReportGeneratorService;
import aQute.lib.strings.Strings;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.generator.ReportGeneratorBuilder;
import biz.aQute.bnd.reporter.generator.ReportGeneratorConstants;

/**
 * This plugins allows to extract all the bnd projects built by a bnd workspace.
 * The user can set the {@link BndWorkspaceContentsPlugin#EXCLUDES_PROPERTY} to
 * skip some projects and the
 * {@link BndWorkspaceContentsPlugin#USE_CONFIG_PROPERTY} to the desired
 * configuration name that will be used to generate the report of the projects.
 */
@BndPlugin(name = "entry." + EntryNamesReference.PROJECTS)
public class BndWorkspaceContentsPlugin implements ReportEntryPlugin<Workspace>, Plugin {

	final public static String				USE_CONFIG_PROPERTY	= "useConfig";
	final public static String				EXCLUDES_PROPERTY	= "excludes";

	private Reporter						_reporter;
	private final Map<String, String>		_properties			= new HashMap<>();
	private final ReportGeneratorBuilder	_generatorBuilder;

	public BndWorkspaceContentsPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.PROJECTS);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Workspace.class.getCanonicalName());

		_generatorBuilder = ReportGeneratorBuilder.create()
			.useCustomConfig()
			.withProjectDefaultPlugins();
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
	public Object extract(final Workspace workspace, final Locale locale) throws Exception {
		Objects.requireNonNull(workspace, "workspace");

		final List<Map<String, Object>> projectReports = new LinkedList<>();
		for (final Project project : workspace.getAllProjects()) {
			if (!getExcludes().contains(project.toString())) {
				final ReportGeneratorService generator = _generatorBuilder.setProcessor(project)
					.build();
				if (getConfigName() != null) {
					projectReports.add(generator.generateReportOf(project, locale,
						"(" + ReportGeneratorConstants.CONFIG_NAME_PROPERTY + "=" + getConfigName() + ")"));
				} else {
					projectReports.add(generator.generateReportOf(project, locale));
				}
				if (!project.isOk()) {
					if (!project.getErrors()
						.isEmpty()) {
						_reporter.error("Creating report of Project %s generates errors: %s", project.getName(),
							Strings.join(",", project.getErrors()));
					}
					if (!project.getWarnings()
						.isEmpty()) {
						_reporter.error("Creating report of Project %s generates warnings: %s", project.getName(),
							Strings.join(",", project.getWarnings()));
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
