package aQute.bnd.xmlattribute;

import java.util.LinkedHashMap;
import java.util.Map;

import aQute.lib.tag.Tag;

public class Namespaces {

	// namespace >> prefix
	final Map<String, String> namespaces = new LinkedHashMap<>();

	public void registerNamespace(String prefix, String namespace) {
		if (namespaces.containsKey(namespace))
			return;

		prefix = prefix == null ? "ns" : prefix;
		String prefix2 = prefix;
		int i = 1;
		while (namespaces.containsValue(prefix2)) {
			if (prefix2.equals(namespaces.get(namespace)))
				return;

			prefix2 = prefix + i++;
		}
		namespaces.put(namespace, prefix2);
	}

	public String getPrefix(String namespace) {
		return namespaces.get(namespace);
	}

	public void addNamespaces(Tag tag) {
		for (Map.Entry<String, String> entry : namespaces.entrySet())
			tag.addAttribute("xmlns:" + entry.getValue(), entry.getKey());
	}

}
