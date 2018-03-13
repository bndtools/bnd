package aQute.bnd.metatype;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.xmlattribute.ExtensionDef;
import aQute.bnd.xmlattribute.Namespaces;
import aQute.bnd.xmlattribute.XMLAttributeFinder;
import aQute.lib.tag.Tag;

class DesignateDef extends ExtensionDef {

	String	ocdRef;
	String	pid;
	boolean	factory;

	public DesignateDef(XMLAttributeFinder finder) {
		super(finder);
	}

	public DesignateDef(String ocdRef, String pid, boolean factory, XMLAttributeFinder finder) {
		super(finder);
		this.ocdRef = ocdRef;
		this.pid = pid;
		this.factory = factory;
	}

	public void prepare(Analyzer analyzer) {

	}

	Tag getOuterTag() {
		Tag metadata = new Tag("metatype:MetaData").addAttribute("xmlns:metatype",
			MetatypeVersion.VERSION_1_3.getNamespace());
		Namespaces namespaces = new Namespaces();
		String xmlns = MetatypeVersion.VERSION_1_3.getNamespace();
		namespaces.registerNamespace("metatype", xmlns);
		addNamespaces(namespaces, xmlns);

		namespaces.addNamespaces(metadata);

		metadata.addContent(getInnerTag(namespaces));

		return metadata;

	}

	Tag getInnerTag(Namespaces namespaces) {
		Tag designate = new Tag("Designate");
		if (factory) {
			designate.addAttribute("factoryPid", pid);
		} else {
			designate.addAttribute("pid", pid);
		}

		addAttributes(designate, namespaces);
		new Tag(designate, "Object").addAttribute("ocdref", ocdRef);
		return designate;
	}

}
