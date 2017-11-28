package biz.aQute.bnd.reporter.plugins;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.WorkspaceEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ProjectReportGenerator;

public class WorkspaceContentsEntryPlugin implements WorkspaceEntryPlugin, Plugin {

	private final Set<String> excludes = new HashSet<>();

	@Override
	public Object extract(final Workspace workspace, final Processor reporter) throws Exception {
		Objects.requireNonNull(workspace, "workspace");
		Objects.requireNonNull(reporter, "reporter");

		final List<Map<String, Object>> projectReports = new LinkedList<>();
		for (final Project project : workspace.getAllProjects()) {
			if (!excludes.contains(project.toString())) {
				try (ProjectReportGenerator gen = new ProjectReportGenerator(project)) {
					projectReports.add(gen.getMetadata());
					reporter.getInfo(gen, gen.toString() + ": ");
				}
			}
		}

		if (!projectReports.isEmpty()) {
			return projectReports;
		} else {
			return null;
		}
	}

	@Override
	public String getEntryName() {
		return "contents";
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		if (map.containsKey("excludes")) {
			for (final String exclude : map.get("excludes").split(",")) {
				excludes.add(exclude.trim());
			}
		}
	}

	@Override
	public void setReporter(final Reporter processor) {
		// Nothing to do
	}
}
