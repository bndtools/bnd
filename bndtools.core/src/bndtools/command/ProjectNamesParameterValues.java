package bndtools.command;

import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.commands.IParameterValues;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

public class ProjectNamesParameterValues implements IParameterValues {

	static final String PROJECT_NAME = "bnd.command.projectName";

	@Override
	public Map getParameterValues() {
		return Arrays.stream(ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProjects())
			.collect(toMap(IProject::getName, IProject::getName));
	}
}
