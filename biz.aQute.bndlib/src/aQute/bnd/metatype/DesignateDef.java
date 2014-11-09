package aQute.bnd.metatype;

import aQute.bnd.osgi.*;
import aQute.lib.tag.*;

class DesignateDef {
	
	String ocdRef;
	String pid;
	boolean factory;
	
	public DesignateDef(String ocdRef, String pid, boolean factory) {
		this.ocdRef = ocdRef;
		this.pid = pid;
		this.factory = factory;
	}

	Tag getTag() {
		Tag metadata = new Tag("metatype:MetaData").addAttribute("xmlns:metatype", MetatypeVersion.VERSION_1_2.getNamespace());
		
		Tag designate = new Tag(metadata, "Designate");
		if (factory) {
			designate.addAttribute("factoryPid", pid);
		} else {
			designate.addAttribute("pid", pid);
		}
		new Tag(designate, "Object").addAttribute("ocdref", ocdRef);
		
		return metadata;
		
	}

	public void prepare(Analyzer analyzer) {
		
	}

}
