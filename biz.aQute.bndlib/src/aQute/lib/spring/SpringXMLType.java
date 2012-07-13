package aQute.lib.spring;

import java.util.*;

import aQute.bnd.osgi.*;

/**
 * This component is called when we find a resource in the META-INF/*.xml
 * pattern. We parse the resource and and the imports to the builder. Parsing is
 * done with XSLT (first time I see the use of having XML for the Spring
 * configuration files!).
 * 
 * @author aqute
 */
public class SpringXMLType extends XMLTypeProcessor {

	protected List<XMLType> getTypes(Analyzer analyzer) throws Exception {
		List<XMLType> types = new ArrayList<XMLType>();

		String header = analyzer.getProperty("Bundle-Blueprint", "OSGI-INF/blueprint");
		process(types, "extract.xsl", header, ".*\\.xml");
		header = analyzer.getProperty("Spring-Context", "META-INF/spring");
		process(types, "extract.xsl", header, ".*\\.xml");

		return types;
	}

}
