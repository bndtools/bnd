package aQute.bnd.service.export;

import java.util.*;

import aQute.bnd.build.*;
import aQute.bnd.osgi.*;

public interface Exporter {
	String getType();
	Resource export(Project project, Map<String,String> options) throws Exception;
}
