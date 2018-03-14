package aQute.bnd.metatype;

import aQute.lib.tag.Tag;

public class IconDef {

	String	resource;
	int		size;

	public IconDef(String resource, int size) {
		super();
		this.resource = resource;
		this.size = size;
	}

	Tag getTag() {
		Tag icon = new Tag("Icon").addAttribute("resource", resource)
			.addAttribute("size", size);
		return icon;
	}

}
