package biz.aQute.bnd.reporter.plugins;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ProjectEntryPlugin;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ProjectSettingsEntryPlugin implements ProjectEntryPlugin {
	
	@Override
	public Object extract(final Project project, final Processor reporter)
	throws Exception {
		Objects.requireNonNull(project, "project");
		Objects.requireNonNull(reporter, "reporter");
		
		final Map<String, Object> settings = new LinkedHashMap<>();
		
		settings.put("folderName", project.getBase().getName());
		// TODO add Project.report output
		
		if (!settings.isEmpty()) {
			return settings;
		} else {
			return null;
		}
	}
	
	@Override
	public String getEntryName() {
		return "settings";
	}
}
