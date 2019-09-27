package aQute.bnd.component;

import static aQute.bnd.component.DSAnnotationReader.V1_0;
import static aQute.bnd.component.DSAnnotationReader.V1_2;
import static aQute.bnd.component.DSAnnotationReader.V1_3;

import aQute.bnd.component.annotations.CollectionType;
import aQute.bnd.component.annotations.FieldOption;
import aQute.bnd.component.annotations.ReferenceCardinality;
import aQute.bnd.component.annotations.ReferencePolicy;
import aQute.bnd.component.annotations.ReferencePolicyOption;
import aQute.bnd.component.annotations.ReferenceScope;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.version.Version;
import aQute.bnd.xmlattribute.ExtensionDef;
import aQute.bnd.xmlattribute.Namespaces;
import aQute.bnd.xmlattribute.XMLAttributeFinder;
import aQute.lib.tag.Tag;

/**
 * Holds the information in the reference element.
 */

class ReferenceDef extends ExtensionDef {

	String					className;
	String					bindDescriptor;

	Version					version	= V1_0;
	String					name;
	String					service;
	ReferenceCardinality	cardinality;
	ReferencePolicy			policy;
	ReferencePolicyOption	policyOption;
	String					target;
	String					bind;
	String					unbind;
	String					updated;
	ReferenceScope			scope;
	String					field;
	FieldOption				fieldOption;
	CollectionType			collectionType;
	boolean					isCollection;
	boolean					isCollectionSubClass;
	Integer					parameter;
	String					reasonForVersion;

	public ReferenceDef(XMLAttributeFinder finder) {
		super(finder);
	}

	/**
	 * Prepare the reference, will check for any errors.
	 *
	 * @param analyzer the analyzer to report errors to.
	 * @throws Exception
	 */
	public void prepare(Analyzer analyzer) throws Exception {
		if (name == null) {
			analyzer.error("No name for a reference");
		}

		if ((updated != null && !updated.equals("-"))) {
			updateVersion(V1_2, "updated in reference");
		}

		if (policyOption != null) {
			updateVersion(V1_2, "policyOption");
		}

		if (target != null) {
			String error = Verifier.validateFilter(target);
			if (error != null) {
				analyzer.error("Invalid target filter %s for %s", target, name);
			}
		}

		if (service == null) {
			analyzer.error("No interface specified on %s", name);
		} else {
			TypeRef ref = analyzer.getTypeRefFromFQN(service);
			analyzer.nonClassReferTo(ref);
		}

		if (scope != null) {
			updateVersion(V1_3, "scope in reference");
		}
		if (field != null) {
			updateVersion(V1_3, "field reference");
		}

	}

	/**
	 * Calculate the tag.
	 *
	 * @param namespaces
	 * @return a tag for the reference element.
	 */
	Tag getTag(Namespaces namespaces) {
		Tag tag = new Tag("reference");

		tag.addAttribute("name", name)
			.addAttribute("cardinality", cardinality)
			.addAttribute("policy", policy)
			.addAttribute("interface", service)
			.addAttribute("target", target);

		if (!"-".equals(bind)) {
			tag.addAttribute("bind", bind);
		}

		if (!"-".equals(unbind)) {
			tag.addAttribute("unbind", unbind);
		}

		if (!"-".equals(updated)) {
			tag.addAttribute("updated", updated);
		}

		tag.addAttribute("policy-option", policyOption)
			.addAttribute("scope", scope)
			.addAttribute("field", field)
			.addAttribute("field-option", fieldOption)
			.addAttribute("field-collection-type", collectionType)
			.addAttribute("parameter", parameter);

		addAttributes(tag, namespaces);

		return tag;
	}

	@Override
	public String toString() {
		return name;
	}

	void updateVersion(Version version, String reason) {
		if (this.version.compareTo(version) >= 0)
			return;

		this.version = version;
		this.reasonForVersion = reason;
	}

}
