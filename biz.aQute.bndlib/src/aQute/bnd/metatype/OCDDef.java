package aQute.bnd.metatype;

import java.util.*;

import aQute.bnd.osgi.*;
import aQute.lib.tag.*;

public class OCDDef {
	
	final List<ADDef> attributes = new ArrayList<ADDef>();
	final List<IconDef> icons = new ArrayList<IconDef>();
	final List<DesignateDef> designates = new ArrayList<DesignateDef>();
	
	String id;
	String name;
	String localization;
	String description;
	
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
		
		Tag ocd = new Tag(metadata, "OCD").addAttribute("id", id);
		
		if (name != null) {
			ocd.addAttribute("name", name);
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
