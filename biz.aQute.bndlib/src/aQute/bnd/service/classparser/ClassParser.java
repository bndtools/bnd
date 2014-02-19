package aQute.bnd.service.classparser;

import aQute.bnd.osgi.*;

public interface ClassParser {
	
	ClassDataCollector getClassDataCollector(Analyzer analyzer);
}
