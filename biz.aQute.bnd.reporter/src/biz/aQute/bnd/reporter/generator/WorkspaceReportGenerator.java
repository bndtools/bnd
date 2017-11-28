package biz.aQute.bnd.reporter.generator;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.WorkspaceEntryPlugin;
import biz.aQute.bnd.reporter.plugins.WorkspaceSettingsEntryPlugin;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tool to create reports of workspaces.
 * <p>
 * This tool used the {@link WorkspaceEntryPlugin} plugins to generate the content of the report.
 */
public final class WorkspaceReportGenerator extends ReportGenerator {
	
	private final Workspace _workspace;
	
	public WorkspaceReportGenerator(final Workspace workspace) {
		super(Objects.requireNonNull(workspace, "workspace"));
		
		_workspace = workspace;
		addDefaultPlugins();
	}
	
	private void addDefaultPlugins() {
		addBasicPlugin(new WorkspaceSettingsEntryPlugin());
	}
	
	@Override
	protected void extractMetadataFromPlugins(final Map<String, Object> metadata, final List<String> includes, final List<String> locales) {
		final List<WorkspaceEntryPlugin> plugins = getPlugins(WorkspaceEntryPlugin.class);
		for (final WorkspaceEntryPlugin rp : plugins) {
			if (includes.contains(rp.getEntryName())) {
				try {
					final Processor r = new Processor();
					r.use(this);
					r.setBase(getBase());
					metadata.put(rp.getEntryName(), rp.extract(_workspace, this));
					getInfo(r, rp.getEntryName() + " workspace entry: ");
				} catch (final Exception e) {
					exception(e, "failed to report workspace entry: %s", rp.getEntryName());
				}
			}
		}
	}
	
	@Override
	protected Set<String> getAllAvailableEntries() {
		return getPlugins(WorkspaceEntryPlugin.class).stream().map(p -> p.getEntryName()).collect(Collectors.toSet());
	}
	
	@Override
	protected List<ReportGenerator> getSubGenerator() {
		final List<ReportGenerator> result = new LinkedList<>();
		Collection<Project> projects;
		
		try {
			projects = _workspace.getAllProjects();
		} catch (final Exception e1) {
			exception(e1, "failed to open projects of workspace %s", _workspace);
			projects = new LinkedList<>();
		}
		
		for (final Project project : projects) {
			result.add(new ProjectReportGenerator(project));
		}
		
		return result;
	}
	
	@Override
	protected String getReporterTypeName() {
		return "workspace";
	}
	
	@Override
	public String toString() {
		return _workspace + " workspace reporter";
	}
}
