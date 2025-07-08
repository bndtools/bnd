---
layout: default
class: Header
title: Bundle-Blueprint RESOURE (',' RESOURCE )
summary: The Bundle-Activator header specifies the name of the class used to start and stop the bundle
---
	

	public class SpringXMLType extends XMLTypeProcessor {

	@Override
	protected List<XMLType> getTypes(Analyzer analyzer) throws Exception {
		List<XMLType> types = new ArrayList<XMLType>();

		String header = analyzer.getProperty(Constants.BUNDLE_BLUEPRINT, "OSGI-INF/blueprint");
		process(types, "extract.xsl", header, ".*\\.xml");
		header = analyzer.getProperty("Spring-Context", "META-INF/spring");
		process(types, "extract.xsl", header, ".*\\.xml");

		return types;
	}

	}
