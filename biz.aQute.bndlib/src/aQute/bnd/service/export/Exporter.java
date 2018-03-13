package aQute.bnd.service.export;

import java.util.Map;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Resource;

public interface Exporter {
	String[] getTypes();

	Map.Entry<String, Resource> export(String type, Project project, Map<String, String> options) throws Exception;
}
