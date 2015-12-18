package aQute.bnd.service;

import java.util.Set;

import aQute.bnd.build.Project;

public interface DependencyContributor {
	void addDependencies(Project project, Set<String> dependencies);
}
