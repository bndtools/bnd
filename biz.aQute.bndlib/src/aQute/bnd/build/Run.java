package aQute.bnd.build;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.export.Exporter;

public class Run extends Project {

	/**
	 * Create a stand alone Run without a workspace. @throws Exception
	 */
	public static Run createStandaloneRun(File file) throws Exception {
		try (Processor run = new Processor();) {
			run.setProperties(file);
			String standalone = run.getProperty("-standalone");
			if (standalone == null)
				return null;

			return createStandaloneRun(run.getProperties(), file.toURI());
		}
	}

	/**
	 * Create a stand alone Run based on properties.
	 */
	public static Run createStandaloneRun(Properties template, URI base) throws Exception {
		Workspace standaloneWorkspace = Workspace.createStandaloneWorkspace(template, base);
		Run run = new Run(standaloneWorkspace, null);
		run.getProperties().putAll(template);
		return run;
	}

	/**
	 * Create a Run that will be stand alone if it contains -standalone. In that
	 * case the given workspace is ignored. Otherwise, the workspace must be a
	 * valid workspace.
	 */
	@SuppressWarnings("resource")
	public static Run createRun(Workspace workspace, File file) throws Exception {
		Processor run = new Processor();
		run.setProperties(file);
		String standalone = run.getProperty("-standalone");
		if (standalone == null) {
			if (workspace == null)
				throw new IllegalArgumentException("The bndrun file is not standalone and no workspace is passed");

			return new Run(workspace, file);
		} else
			return createStandaloneRun(run.getProperties(), file.toURI());
	}

	public Run(Workspace workspace, File projectDir, File propertiesFile) throws Exception {
		super(workspace, projectDir, propertiesFile);
	}

	public Run(Workspace workspace, File propertiesFile) throws Exception {
		super(workspace, propertiesFile == null ? null : propertiesFile.getParentFile(), propertiesFile);
	}

	public void report(Map<String,Object> table) throws Exception {
		super.report(table, false);
	}

	public String toString() {
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

}
