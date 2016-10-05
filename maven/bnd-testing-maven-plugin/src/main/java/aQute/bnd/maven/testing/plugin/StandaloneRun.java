package aQute.bnd.maven.testing.plugin;

import java.io.File;
import java.util.Collection;

import aQute.bnd.build.Container;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;

public class StandaloneRun extends Run {

	public StandaloneRun(File propertiesFile) throws Exception {
		super(createWorkspace(propertiesFile), propertiesFile);
	}

	private static Workspace createWorkspace(File propertiesFile) throws Exception {
		Processor processor = new Processor();
		processor.setProperties(propertiesFile);
		return Workspace.createStandaloneWorkspace(processor, propertiesFile.toURI());
	}

	public void setRunBundles(Collection< ? extends Container> runBundles) throws Exception {
		Collection<Container> runbundles = getRunbundles();
		runbundles.clear();
		runbundles.addAll(runBundles);
	}
}
