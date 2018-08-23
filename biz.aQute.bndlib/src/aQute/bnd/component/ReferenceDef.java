package aQute.bnd.component;

import org.osgi.service.component.annotations.CollectionType;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ReferenceScope;

import aQute.bnd.osgi.Analyzer;
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

	Version					version	= AnnotationReader.V1_0;
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
		if (name == null)
			analyzer.error("No name for a reference");

		if ((updated != null && !updated.equals("-")) || policyOption != null)
			updateVersion(AnnotationReader.V1_2);

		if (target != null) {
			String error = Verifier.validateFilter(target);
			if (error != null)
				analyzer.error("Invalid target filter %s for %s", target, name);
		}

		if (service == null)
			analyzer.error("No interface specified on %s", name);

		if (scope != null || field != null)
			updateVersion(AnnotationReader.V1_3);

	}

	/**
	 * Calculate the tag.
	 * 
	 * @param namespaces
	 * @return a tag for the reference element.
	 */
	public Tag getTag(Namespaces namespaces) {
		Tag ref = new Tag("reference");

		ref.addAttribute("name", name);
		if (cardinality != null)
			ref.addAttribute("cardinality", cardinality.toString());

		if (policy != null)
			ref.addAttribute("policy", policy.toString());

		ref.addAttribute("interface", service);

		if (target != null)
			ref.addAttribute("target", target);

		if (bind != null && !"-".equals(bind))
			ref.addAttribute("bind", bind);

		if (unbind != null && !"-".equals(unbind))
			ref.addAttribute("unbind", unbind);

		if (updated != null && !"-".equals(updated))
			ref.addAttribute("updated", updated);

		if (policyOption != null)
			ref.addAttribute("policy-option", policyOption.toString());

		if (scope != null)
			ref.addAttribute("scope", scope.toString());

		if (field != null)
			ref.addAttribute("field", field);

		if (fieldOption != null)
			ref.addAttribute("field-option", fieldOption.toString());

		if (collectionType != null)
			ref.addAttribute("field-collection-type", collectionType.toString());

		addAttributes(ref, namespaces);

		return ref;
	}

	@Override
	public String toString() {
		return name;
	}

	void updateVersion(Version version) {
		this.version = ComponentDef.max(this.version, version);
	}

}
