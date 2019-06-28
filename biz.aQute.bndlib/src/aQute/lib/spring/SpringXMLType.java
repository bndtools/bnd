package aQute.lib.spring;

import java.util.ArrayList;
import java.util.List;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;

/**
 * This component is called when we find a resource in the META-INF/*.xml
 * pattern. We parse the resource and and the imports to the builder. Parsing is
 * done with XSLT (first time I see the use of having XML for the Spring
 * configuration files!).
 *
 * @author aqute
 */
@BndPlugin(name = "blueprint")
public class SpringXMLType extends XMLTypeProcessor {

	@Override
	protected List<XMLType> getTypes(Analyzer analyzer) throws Exception {
		List<XMLType> types = new ArrayList<>();

		String header = analyzer.getProperty(Constants.BUNDLE_BLUEPRINT, "OSGI-INF/blueprint");
		process(types, "extract.xsl", header, ".*\\.xml");
		header = analyzer.getProperty("Spring-Context", "META-INF/spring");
		process(types, "extract.xsl", header, ".*\\.xml");

		return types;
	}

}
