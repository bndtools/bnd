package aQute.bnd.service.export;

import java.util.*;

import aQute.bnd.build.*;
import aQute.bnd.osgi.*;

public interface Exporter {
	String[] getTypes();

	Resource export(String type, Project project, Map<String,String> options) throws Exception;
}
