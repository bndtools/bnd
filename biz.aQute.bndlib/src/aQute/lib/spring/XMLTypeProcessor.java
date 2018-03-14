package aQute.lib.spring;

import java.util.ArrayList;
import java.util.List;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.AnalyzerPlugin;

public class XMLTypeProcessor implements AnalyzerPlugin {

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		List<XMLType> types = getTypes(analyzer);
		for (XMLType type : types) {
			type.analyzeJar(analyzer);
		}
		return false;
	}

	protected List<XMLType> getTypes(@SuppressWarnings("unused") Analyzer analyzer) throws Exception {
		return new ArrayList<>();
	}

	protected void process(List<XMLType> types, String resource, String paths, String pattern) throws Exception {

		Parameters map = Processor.parseHeader(paths, null);
		for (String path : map.keySet()) {
			types.add(new XMLType(getClass().getResource(resource), path, pattern));
		}
	}

}
