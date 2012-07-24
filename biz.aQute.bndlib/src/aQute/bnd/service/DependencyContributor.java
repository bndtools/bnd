package aQute.bnd.service;

import java.util.*;

import aQute.bnd.build.*;

public interface DependencyContributor {
	void addDependencies(Project project, Set<String> dependencies);
}
