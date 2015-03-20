package aQute.bnd.metatype;

import java.lang.annotation.Annotation;
import java.util.regex.*;

import aQute.bnd.annotation.plugin.*;
import aQute.bnd.annotation.xml.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Clazz.MethodDef;

@BndPlugin(name = BndExtensionReader.BND_METATYPE_EXTENSIONS)
public class BndExtensionReader implements ExtensionReader {

	public static final String	BND_METATYPE_EXTENSIONS	= "BndMetatypeExtensions";
	final static Pattern		ATTRIBUTE_PATTERN			= Pattern
			.compile("\\s*([^=\\s:]+)\\s*=(.*)");

	@Override
	public void doAnnotation(Annotation a, aQute.bnd.osgi.Annotation bndAnno, OCDDef ocdDef, Analyzer analyzer) {
		if (a instanceof Attribute) {
			doAttribute((Attribute)a, ocdDef, analyzer);
		}
		if (a instanceof Attributes) {
			for (Attribute attr: ((Attributes)a).value()) {
				doAttribute(attr, ocdDef, analyzer);
			}
		}
	}

	private void doAttribute(Attribute a, OCDDef ocdDef, Analyzer analyzer) {
		String namespace = a.namespace();
		String prefix = a.prefix();
		prefix = ocdDef.registerNamespace(prefix, namespace);
		for (String attribute: a.attributes()) {
			Matcher m = ATTRIBUTE_PATTERN.matcher(attribute);
			if (m.matches()) {
				String key = m.group(1);
				String value = m.group(2).trim();
				ocdDef.addExtensionAttribute(prefix, key, value);
			} else {
				analyzer.error("Malformed attribute %s", attribute);
			}
		}
	}

	@Override
	public void doMethodAnnotation(MethodDef method, Annotation ad, aQute.bnd.osgi.Annotation a, OCDDef ocdDef,
			ADDef adDef, Analyzer analyzer) {
		if (ad instanceof Attribute) {
			doADAttribute((Attribute)ad, ocdDef, adDef, analyzer);
		}
		if (ad instanceof Attributes) {
			for (Attribute attr: ((Attributes)ad).value()) {
				doADAttribute(attr, ocdDef, adDef, analyzer);
			}
		}
	}
	private void doADAttribute(Attribute a, OCDDef ocdDef, ADDef adDef, Analyzer analyzer) {
		String namespace = a.namespace();
		String prefix = a.prefix();
		prefix = ocdDef.registerNamespace(prefix, namespace);
		for (String attribute: a.attributes()) {
			Matcher m = ATTRIBUTE_PATTERN.matcher(attribute);
			if (m.matches()) {
				String key = m.group(1);
				String value = m.group(2).trim();
				adDef.addExtensionAttribute(prefix, key, value);
			} else {
				analyzer.error("Malformed attribute %s", attribute);
			}
		}
	}

	@Override
	public String name() {
		return BND_METATYPE_EXTENSIONS;
	}

}
