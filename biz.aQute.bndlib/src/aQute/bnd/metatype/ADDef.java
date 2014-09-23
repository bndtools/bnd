package aQute.bnd.metatype;

import java.util.*;

import aQute.lib.tag.*;

public class ADDef {
	
	String id;
	String name;
	String description;
	String type; //an enum?
	int cardinality;
	String min;
	String max;
	String[] defaults;
	boolean required = true;
	final List<OptionDef> options = new ArrayList<OptionDef>();
	
	
	Tag getTag() {
		Tag ad = new Tag("AD").addAttribute("id", id).addAttribute("type", type);

		if (cardinality != 0) {
			ad.addAttribute("cardinality", cardinality);
		}
		
		if (!required) {
			ad.addAttribute("required", required);
		}
		
		if (name != null) {
			ad.addAttribute("name", name);
		}

		if (description != null) {
			ad.addAttribute("description", description);
		}

		if (min != null) {
			ad.addAttribute("min", min);
		}

		if (max != null) {
			ad.addAttribute("max", max);
		}

		if (defaults != null) {
			StringBuffer b = new StringBuffer();
			String sep = "";
			for (String defaultValue :defaults) {
				b.append(sep).append(defaultValue);
				sep = ",";
			}
			ad.addAttribute("default", b.toString());
		}
		
		for (OptionDef option: options) {
			ad.addContent(option.getTag());
		}

		return ad;
	}

}
