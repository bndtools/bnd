package aQute.bnd.metatype;

import java.util.*;

import org.osgi.service.metatype.annotations.*;

import aQute.lib.tag.*;

public class ADDef {
	
	String id;
	String name;
	String description;
	AttributeType type; 
	private String typeString;
	int cardinality;
	String min;
	String max;
	String[] defaults;
	boolean required = true;
	final List<OptionDef> options = new ArrayList<OptionDef>();
	private final Map<String, String> extensionAttributes = new LinkedHashMap<String, String>();

	public void addExtensionAttribute(String prefix, String key, String value) {
		extensionAttributes.put(prefix + ":" + key, value);
	}

	public void prepare() {
		typeString = (type == null)? "*INVALID*": type.toString();		
	}
	
	Tag getTag() {
		Tag ad = new Tag("AD").addAttribute("id", id).addAttribute("type", typeString);

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
		
		for (Map.Entry<String,String> entry: extensionAttributes.entrySet()) {
			ad.addAttribute(entry.getKey(), entry.getValue());
		}
		
		for (OptionDef option: options) {
			ad.addContent(option.getTag());
		}

		return ad;
	}

}
