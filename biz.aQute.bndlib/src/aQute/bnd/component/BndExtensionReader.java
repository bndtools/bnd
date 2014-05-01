package aQute.bnd.component;

import java.util.regex.*;

import aQute.bnd.annotation.plugin.*;
import aQute.bnd.annotation.xml.*;
import aQute.bnd.osgi.*;

@BndPlugin(name = BndExtensionReader.BND_DS_EXTENSIONS)
public class BndExtensionReader implements ExtensionReader {

	public static final String	BND_DS_EXTENSIONS	= "BndDSExtensions";
	final static Pattern		ATTRIBUTE_PATTERN			= Pattern
			.compile("\\s*([^=\\s:]+)\\s*=(.*)");

	@Override
	public void doAnnotation(java.lang.annotation.Annotation a, aQute.bnd.osgi.Annotation bndAnno, ComponentDef componentDef, Analyzer analyzer) {
		if (a instanceof Attribute) {
			doAttribute((Attribute)a, componentDef, analyzer);
		}
		if (a instanceof Attributes) {
			for (Attribute attr: ((Attributes)a).value()) {
				doAttribute(attr, componentDef, analyzer);
			}
		}

	}

	private void doAttribute(Attribute a, ComponentDef component, Analyzer analyzer) {
		String namespace = a.namespace();
		String prefix = a.prefix();
		prefix = component.registerNamespace(prefix, namespace);
		for (String attribute: a.attributes()) {
			Matcher m = ATTRIBUTE_PATTERN.matcher(attribute);
			if (m.matches()) {
				String key = m.group(1);
				String value = m.group(2).trim();
				component.addExtensionAttribute(prefix, key, value);
			} else {
				analyzer.error("Malformed attribute %s", attribute);
			}
		}
	}

	@Override
	public String name() {
		return BND_DS_EXTENSIONS;
	}

}
