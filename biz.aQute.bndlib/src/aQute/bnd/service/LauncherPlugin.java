package aQute.bnd.service;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.ProjectTester;

public interface LauncherPlugin {
	ProjectLauncher getLauncher(Project project) throws Exception;

	ProjectTester getTester(Project project);
}
