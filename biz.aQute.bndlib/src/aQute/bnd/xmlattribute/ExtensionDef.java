package aQute.bnd.xmlattribute;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import aQute.bnd.annotation.xml.XMLAttribute;
import aQute.bnd.osgi.Annotation;
import aQute.lib.tag.Tag;
import aQute.libg.tuple.Pair;

public class ExtensionDef {

	private final XMLAttributeFinder					finder;

	private final List<Pair<XMLAttribute, Annotation>>	attributes	= new ArrayList<>();

	public ExtensionDef(XMLAttributeFinder finder) {
		this.finder = finder;
	}

	public void addExtensionAttribute(XMLAttribute xmlAttr, Annotation a) {
		attributes.add(new Pair<>(xmlAttr, a));
	}

	public void addNamespaces(Namespaces namespaces, String docNS) {
		for (Iterator<Pair<XMLAttribute, Annotation>> i = attributes.iterator(); i.hasNext();) {
			Pair<XMLAttribute, Annotation> p = i.next();
			XMLAttribute xmlAttr = p.getFirst();
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
			for (Pair<XMLAttribute, Annotation> entry : attributes) {
				XMLAttribute xmlAttribute = entry.getFirst();
				String prefix = namespaces.getPrefix(xmlAttribute.namespace());
				Annotation a = entry.getSecond();
				Map<String, String> props = finder.getDefaults(a);
				for (String key : a.keySet()) {
					Object obj = a.get(key);
					String value;
					if (obj.getClass()
						.isArray()) {
						StringBuilder sb = new StringBuilder();
						String sep = "";
						for (int i = 0; i < Array.getLength(obj); i++) {
							Object el = Array.get(obj, i);
							sb.append(sep)
								.append(String.valueOf(el));
							sep = " ";
						}
						value = sb.toString();
					} else {
						value = String.valueOf(obj);
					}
					props.put(key, value);
				}
				String[] mapping = xmlAttribute.mapping();
				for (Map.Entry<String, String> prop : props.entrySet()) {
					String key = prop.getKey();
					if (mapping != null && mapping.length > 0) {
						String match = key + "=";
						for (String map : mapping) {
							if (map.startsWith(match))
								key = map.substring(match.length());
						}
					}
					tag.addAttribute(prefix + ":" + key, prop.getValue());
				}
			}
		}
	}

}
