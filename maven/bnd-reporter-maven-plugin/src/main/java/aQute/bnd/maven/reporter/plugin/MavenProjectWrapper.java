package aQute.bnd.maven.reporter.plugin;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.project.MavenProject;

/**
 * This class is a wrapper from which report plugins will extract data.
 */
public class MavenProjectWrapper {

	private Properties			reportConfigs	= new Properties();
	private List<MavenProject>	projects;
	private MavenProject		project;
	private List<MavenProject>	subProjects;

	/**
	 * Constructor of the wrapper.
	 *
	 * @param projects the list of projects of the current session
	 * @param projectBase the current project
	 */
	public MavenProjectWrapper(List<MavenProject> projects, MavenProject projectBase) {
		this.project = projectBase;
		this.projects = projects;

		// Here we get the MavenProject objects of the modules' current project
		this.subProjects = projectBase.getModules()
			.stream()
			.map(module -> {
				return projects.stream()
					.filter(p -> p.getBasedir()
						.getName()
						.equals(module))
					.findAny();
			})
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
	}

	public Properties getReportConfig() {
		return reportConfigs;
	}

	/**
	 * @return the current project
	 */
	public MavenProject getProject() {
		return this.project;
	}

	/**
	 * @return the list of child project of the current project
	 */
	public List<MavenProject> getSubProjects() {
		return this.subProjects;
	}

	/**
	 * @return the list of the project of the current session
	 */
	public List<MavenProject> getProjects() {
		return projects;
	}

	/**
	 * @return true if the current project is an aggregator
	 */
	public boolean isAggregator() {
		return !getProject().getModules()
			.isEmpty();
	}
}
