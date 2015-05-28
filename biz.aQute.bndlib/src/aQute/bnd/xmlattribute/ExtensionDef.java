package aQute.bnd.xmlattribute;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import aQute.bnd.annotation.xml.XMLAttribute;
import aQute.bnd.osgi.Annotation;
import aQute.lib.tag.Tag;

public class ExtensionDef {

	protected final Map<XMLAttribute,Annotation>	attributes	= new LinkedHashMap<XMLAttribute,Annotation>();

	public void addExtensionAttribute(XMLAttribute xmlAttr, Annotation a) {
		attributes.put(xmlAttr, a);
	}

	public void addNamespaces(Namespaces namespaces, String docNS) {
		for (Iterator<XMLAttribute> i = attributes.keySet().iterator(); i.hasNext();) {
			XMLAttribute xmlAttr = i.next();
			if (matches(xmlAttr, docNS))
				namespaces.registerNamespace(xmlAttr.prefix(), xmlAttr.namespace());
			else
				i.remove();
		}
	}

	private boolean matches(XMLAttribute xmlAttr, String docNS) {
		String[] embedIn = xmlAttr.embedIn();
		if (embedIn == null)
			return true;
		for (String match : embedIn)
			if (matches(match, docNS))
				return true;
		return false;
	}

	private boolean matches(String match, String docNS) {
		if (match.equals(docNS))
			return true;
		if (match.endsWith("*")) {
			match = match.substring(0, match.length() - 1);
			return docNS.startsWith(match);
		}
		return false;
	}

	// non-matching attributes have already been removed
	public void addAttributes(Tag tag, Namespaces namespaces) {
		if (namespaces != null) {
			for (Map.Entry<XMLAttribute,Annotation> entry : attributes.entrySet()) {
				String prefix = namespaces.getPrefix(entry.getKey().namespace());
				for (String key : entry.getValue().keySet()) {
					Object obj = entry.getValue().get(key);
					String value;
					if (obj.getClass().isArray()) {
						StringBuilder sb = new StringBuilder();
						String sep = "";
						for (int i = 0; i < Array.getLength(obj); i++) {
							Object el = Array.get(obj, i);
							sb.append(sep).append(String.valueOf(el));
							sep = " ";
						}
						value = sb.toString();
					} else {
						value = String.valueOf(obj);
					}
					tag.addAttribute(prefix + ":" + key, value);
				}
			}
		}
	}

}