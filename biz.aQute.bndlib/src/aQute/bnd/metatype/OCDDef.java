package aQute.bnd.metatype;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.xmlattribute.ExtensionDef;
import aQute.bnd.xmlattribute.Namespaces;
import aQute.bnd.xmlattribute.XMLAttributeFinder;
import aQute.lib.tag.Tag;

public class OCDDef extends ExtensionDef {

	final List<ADDef>			attributes	= new ArrayList<>();
	final List<IconDef>			icons		= new ArrayList<>();
	final List<DesignateDef>	designates	= new ArrayList<>();

	String						id;
	String						name;
	String						localization;
	String						description;
	MetatypeVersion				version;

	public OCDDef(XMLAttributeFinder finder, MetatypeVersion minVersion) {
		super(finder);
		this.version = minVersion;
	}

	void prepare(Analyzer analyzer) {
		if (attributes.isEmpty()) {
			updateVersion(MetatypeVersion.VERSION_1_3);
		}
		Set<String> adIds = new HashSet<>();
		for (ADDef ad : attributes) {
			ad.prepare(this);
			if (!adIds.add(ad.id)) {
				analyzer.error("OCD for %s.%s has duplicate AD id %s due to colliding munged element names", id, name,
					ad.id);
			}
		}
	}

	Tag getTag() {

		Tag metadata = new Tag("metatype:MetaData");
		// .addAttribute("xmlns:metatype",
		// MetatypeVersion.VERSION_1_3.getNamespace());
		Namespaces namespaces = new Namespaces();
		String xmlns = version.getNamespace();
		namespaces.registerNamespace("metatype", xmlns);
		addNamespaces(namespaces, xmlns);
		for (ADDef ad : attributes)
			ad.addNamespaces(namespaces, xmlns);

		for (DesignateDef dd : designates)
			dd.addNamespaces(namespaces, xmlns);

		namespaces.addNamespaces(metadata);

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

		addAttributes(ocd, namespaces);

		for (ADDef ad : attributes) {
			ocd.addContent(ad.getTag(namespaces));
		}

		for (IconDef icon : icons) {
			ocd.addContent(icon.getTag());
		}

		for (DesignateDef designate : designates) {
			metadata.addContent(designate.getInnerTag(namespaces));
		}

		return metadata;
	}

	void updateVersion(MetatypeVersion version) {
		this.version = max(this.version, version);
	}

	static <T extends Comparable<T>> T max(T a, T b) {
		int n = a.compareTo(b);
		if (n >= 0)
			return a;
		return b;
	}

}
