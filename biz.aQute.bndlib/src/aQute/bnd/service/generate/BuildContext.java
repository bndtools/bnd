package aQute.bnd.service.generate;

import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Processor;

@ProviderType
public class BuildContext extends Processor {

	private final Project		project;
	private final List<String>	arguments;

	public BuildContext(Project project, Map<String, String> localProperties, List<String> arguments) {
		super(project);
		this.setBase(project.getBase());
		use(project);
		this.project = project;
		this.arguments = arguments;
		for (Map.Entry<String, String> e : localProperties.entrySet()) {
			this.set(e.getKey(), e.getValue());
		}

	}

	public Project getProject() {
		return project;
	}

	public List<String> getArguments() {
		return arguments;
	}
}
