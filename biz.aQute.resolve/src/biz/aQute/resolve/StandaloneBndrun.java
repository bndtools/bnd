package biz.aQute.resolve;

import java.io.File;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;

public class StandaloneBndrun extends Bndrun {

	public StandaloneBndrun(File propertiesFile) throws Exception {
		super(createWorkspace(propertiesFile), propertiesFile);
	}

	private static Workspace createWorkspace(File propertiesFile) throws Exception {
		Processor processor = new Processor();
		processor.setProperties(propertiesFile);
		return Workspace.createStandaloneWorkspace(processor, propertiesFile.toURI());
	}

}
