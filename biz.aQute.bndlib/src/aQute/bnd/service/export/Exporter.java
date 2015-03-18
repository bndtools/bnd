package aQute.bnd.service.export;

import java.util.*;

import aQute.bnd.build.*;
import aQute.bnd.osgi.*;

public interface Exporter {
	String[] getTypes();

	Map.Entry<String,Resource> export(String type, Project project, Map<String,String> options) throws Exception;
}
