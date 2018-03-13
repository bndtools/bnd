package aQute.bnd.metatype;

import aQute.lib.tag.Tag;

public class OptionDef {

	String	label;
	String	value;

	public OptionDef(String label, String value) {
		this.label = label;
		this.value = value;
	}

	Tag getTag() {
		Tag option = new Tag("Option").addAttribute("label", label)
			.addAttribute("value", value);
		return option;
	}

}
