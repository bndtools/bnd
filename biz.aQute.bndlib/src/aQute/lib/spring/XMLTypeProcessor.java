package aQute.lib.spring;

import java.util.*;

import aQute.bnd.service.*;
import aQute.lib.osgi.*;
import aQute.libg.header.*;

public class XMLTypeProcessor implements AnalyzerPlugin {

	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		List<XMLType> types = getTypes(analyzer);
		for (XMLType type : types) {
			type.analyzeJar(analyzer);
		}
		return false;
	}

	protected List<XMLType> getTypes(Analyzer analyzer) throws Exception {
		return new ArrayList<XMLType>();
	}

	protected void process(List<XMLType> types, String resource, String paths, String pattern) throws Exception {

		Parameters map = Processor.parseHeader(paths, null);
		for (String path : map.keySet()) {
			types.add(new XMLType(getClass().getResource(resource), path, pattern));
		}
	}

}
