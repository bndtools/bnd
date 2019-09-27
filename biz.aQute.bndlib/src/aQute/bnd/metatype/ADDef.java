package aQute.bnd.metatype;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.metatype.annotations.AttributeDefinition;
import aQute.bnd.metatype.annotations.AttributeType;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.xmlattribute.ExtensionDef;
import aQute.bnd.xmlattribute.Namespaces;
import aQute.bnd.xmlattribute.XMLAttributeFinder;
import aQute.lib.tag.Tag;

public class ADDef extends ExtensionDef {
	AttributeDefinition		ad;
	Annotation				a;

	String					id;
	String					name;
	String					description;
	AttributeType			type;
	private String			typeString;
	int						cardinality;
	String					min;
	String					max;
	String[]				defaults;
	boolean					required	= true;
	final List<OptionDef>	options		= new ArrayList<>();

	public ADDef(XMLAttributeFinder finder) {
		super(finder);
	}

	public void prepare(OCDDef ocdDef) {
		if (type == AttributeType.CHARACTER && ocdDef.version == MetatypeVersion.VERSION_1_2) {
			typeString = "Char";
		} else {
			typeString = (type == null) ? "*INVALID*" : type.toString();
		}
	}

	Tag getTag(Namespaces namespaces) {
		Tag ad = new Tag("AD").addAttribute("id", id)
			.addAttribute("type", typeString);

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
			StringBuilder sb = new StringBuilder();
			String sep = "";
			for (String defaultValue : defaults) {
				sb.append(sep);
				escape(defaultValue, sb);
				sep = ",";
			}
			ad.addAttribute("default", sb.toString());
		}

		for (OptionDef option : options) {
			ad.addContent(option.getTag());
		}

		addAttributes(ad, namespaces);

		return ad;
	}

	private static final Pattern escapes = Pattern.compile("[\\\\ ,]");

	private void escape(String defaultValue, StringBuilder sb) {
		Matcher m = escapes.matcher(defaultValue);
		int start = 0;
		for (; m.find(); start = m.end()) {
			sb.append(defaultValue, start, m.start())
				.append('\\')
				.append(m.group());
		}
		sb.append(defaultValue, start, defaultValue.length());
	}

}
