package aQute.bnd.service.classparser;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.ClassDataCollector;

public interface ClassParser {

	ClassDataCollector getClassDataCollector(Analyzer analyzer);
}
