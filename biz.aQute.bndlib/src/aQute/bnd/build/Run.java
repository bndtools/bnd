package aQute.bnd.build;

import java.io.File;
import java.util.List;
import java.util.Map;

import aQute.bnd.osgi.Resource;
import aQute.bnd.service.export.Exporter;

public class Run extends Project {

	public Run(Workspace workspace, File projectDir, File propertiesFile ) throws Exception {
		super(workspace, projectDir, propertiesFile);
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
