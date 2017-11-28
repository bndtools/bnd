package biz.aQute.bnd.reporter.plugins;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.WorkspaceEntryPlugin;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class WorkspaceSettingsEntryPlugin implements WorkspaceEntryPlugin {
	
	@Override
	public Object extract(final Workspace workspace, final Processor reporter)
	throws Exception {
		Objects.requireNonNull(workspace, "workspace");
		Objects.requireNonNull(reporter, "reporter");
		
		final Map<String, Object> settings = new LinkedHashMap<>();
		
		settings.put("folderName", workspace.getBase().getName());
		// TODO add Project.report output
		
		return settings;
	}
	
	@Override
	public String getEntryName() {
		return "settings";
	}
}
