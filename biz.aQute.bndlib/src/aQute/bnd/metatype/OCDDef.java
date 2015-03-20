package aQute.bnd.metatype;

import java.util.*;

import aQute.bnd.osgi.*;
import aQute.lib.tag.*;

public class OCDDef {
	
	final List<ADDef> attributes = new ArrayList<ADDef>();
	final List<IconDef> icons = new ArrayList<IconDef>();
	final List<DesignateDef> designates = new ArrayList<DesignateDef>();
	final Map<String, String>		extensionAttributes		= new HashMap<String, String>();
	final Map<String, String>		namespaces		= new HashMap<String, String>();
	
	String id;
	String name;
	String localization;
	String description;
	
	public String registerNamespace(final String prefix, String namespace) {
		if (namespaces.containsValue(namespace)) {
			for (Map.Entry<String, String> entry: namespaces.entrySet()) {
				if (entry.getValue().equals(namespace)) {
					return entry.getKey();
				}
			}
		}
		String prefix2 = prefix;
		int i = 1;
		while (namespaces.containsKey(prefix2)) {
			if (namespaces.get(prefix2).equals(namespace)) {
				return prefix2;
			}
			prefix2 = prefix + i++;
		}
		namespaces.put(prefix2, namespace);
		return prefix2;
	}
	
	public void addExtensionAttribute(String prefix, String key, String value) {
		extensionAttributes.put(prefix + ":" + key, value);
	}

	void prepare(Analyzer analyzer) {
		Set<String> adIds = new HashSet<String>();
		for (ADDef ad: attributes) {
			ad.prepare();
			if (!adIds.add(ad.id)) {
				analyzer.error(
						"OCD for %s.%s has duplicate AD id %s due to colliding munged element names",
						id, name, ad.id);
			}
		}
	}
		
	Tag getTag() {
		Tag metadata = new Tag("metatype:MetaData").addAttribute("xmlns:metatype", MetatypeVersion.VERSION_1_3.getNamespace());
		
		if (localization != null) {
			metadata.addAttribute("localization", localization);
		}
		
		for (Map.Entry<String, String> a: namespaces.entrySet()) {
			metadata.addAttribute("xmlns:" + a.getKey(), a.getValue());
		}
		
		Tag ocd = new Tag(metadata, "OCD").addAttribute("id", id);
		
		if (name != null) {
			ocd.addAttribute("name", name);
		}
		
		for (Map.Entry<String, String> a: extensionAttributes.entrySet()) {
			ocd.addAttribute(a.getKey(), a.getValue());
		}

		if (description != null) {
			ocd.addAttribute("description", description);
		}
		
		for (ADDef ad: attributes) {
			ocd.addContent(ad.getTag());
		}
		
		for (IconDef icon: icons) {
			ocd.addContent(icon.getTag());
		}
		
		for (DesignateDef designate: designates) {
			metadata.addContent(designate.getInnerTag());
		}
		
		return metadata;
	}

}
