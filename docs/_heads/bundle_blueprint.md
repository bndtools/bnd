---
layout: default
title: Bundle-Blueprint RESOURE (',' RESOURCE )
class: Header
summary: |
   The Bundle-Activator header specifies the name of the class used to start and stop the bundle
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Bundle-Blueprint: /blueprint/*.xml`

- Pattern: `.*`

<!-- Manual content from: ext/bundle_blueprint.md --><br /><br />
	

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
