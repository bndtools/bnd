package aQute.bnd.service;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.service.reporter.Reporter;

/**
 * For instance, use with a bnd.bnd file
 * -nobundles
 * -dependson:*
 * -plugin: aQute.bnd.service.AllProjectsDependencyContributor;projectFilter=org.foo.[^_]* 
 * 
 * where you want to exclude projects named org.foo.bar_itests
 *
 */

public class AllProjectsDependencyContributor implements DependencyContributor, Plugin {

	private Reporter reporter;
	
	private Pattern filter;
	
	@Override
	public void addDependencies(Project project, Set<String> dependencies) {
		if (dependencies.size() == 1 && dependencies.iterator().next().equals("*")) {
			dependencies.remove("*");
			Workspace ws = project.getWorkspace();
			try {
			for (Project p: ws.getAllProjects()) {
				if (!p.getName().equals(project.getName()) && filter.matcher(p.getName()).matches()) {
					dependencies.add(p.getName());
					reporter.trace("Adding project %s", p.getName());
				}
			}
			} catch (Exception e) {
				reporter.exception(e, "Problem determining all workspace projects");
			}
		}

	}

	@Override
	public void setProperties(Map<String, String> map) {
		String filterString = map.get("projectFilter");
		if (filterString == null) {
			filterString = ".*";
		}
		filter = Pattern.compile(filterString);
		
	}

	@Override
	public void setReporter(Reporter processor) {
		this.reporter = processor;
	}

}
