package aQute.bnd.build;

import java.io.File;
import java.util.Map;

import aQute.bnd.osgi.Processor;

public class Run extends Project {

	/**
	 * Create a Run that will be stand alone if it contains -standalone. In that
	 * case the given workspace is ignored. Otherwise, the workspace must be a
	 * valid workspace.
	 */
	public static Run createRun(Workspace workspace, File file) throws Exception {
		Processor processor;
		if (workspace != null) {
			Run run = new Run(workspace, file);
			if (run.getProperties()
				.get(STANDALONE) == null) {
				return run;
			}
			// -standalone specified
			processor = run;
		} else {
			processor = new Processor();
			processor.setProperties(file);
		}

		Workspace standaloneWorkspace = Workspace.createStandaloneWorkspace(processor, file.toURI());
		Run run = new Run(standaloneWorkspace, file);
		return run;
	}

	public Run(Workspace workspace, File projectDir, File propertiesFile) throws Exception {
		super(workspace, projectDir, propertiesFile);
	}

	public Run(Workspace workspace, File propertiesFile) throws Exception {
		super(workspace, propertiesFile == null ? null : propertiesFile.getParentFile(), propertiesFile);
	}

	@Override
	public void report(Map<String, Object> table) throws Exception {
		super.report(table, false);
	}

	@Override
	public String getName() {
		return getPropertiesFile().getName();
	}
}
