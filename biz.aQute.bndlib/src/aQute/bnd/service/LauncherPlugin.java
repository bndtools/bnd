package aQute.bnd.service;

import aQute.bnd.build.*;

public interface LauncherPlugin {
	ProjectLauncher getLauncher(Project project) throws Exception;

	ProjectTester getTester(Project project);
}
