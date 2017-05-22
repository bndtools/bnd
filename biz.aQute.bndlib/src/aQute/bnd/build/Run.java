package aQute.bnd.build;

import java.io.File;
import java.util.List;
import java.util.Map;

import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.export.Exporter;

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
			if (run.getProperties().get(STANDALONE) == null) {
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
	public void report(Map<String,Object> table) throws Exception {
		super.report(table, false);
	}

	@Override
	public String getName() {
		return getPropertiesFile().getName();
	}

	public Map.Entry<String,Resource> export(String type, Map<String,String> options) throws Exception {
		Exporter exporter = getExporter(type);
		if (exporter == null) {
			error("No exporter for %s", type);
			return null;
		}

		return exporter.export(type, this, options);
	}

	private Exporter getExporter(String type) {
		List<Exporter> exporters = getPlugins(Exporter.class);
		for (Exporter e : exporters) {
			for (String exporterType : e.getTypes()) {
				if (type.equals(exporterType)) {
					return e;
				}
			}
		}
		return null;
	}

	public boolean isStandalone() {
		return getWorkspace().getLayout() == WorkspaceLayout.STANDALONE;
	}
}
