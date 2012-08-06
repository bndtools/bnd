package aQute.bnd.component;

import org.osgi.service.component.annotations.*;

import aQute.bnd.osgi.*;
import aQute.bnd.version.*;
import aQute.lib.tag.*;

/**
 * Holds the information in the reference element.
 */

class ReferenceDef {
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

	/**
	 * Prepare the reference, will check for any errors.
	 * 
	 * @param analyzer
	 *            the analyzer to report errors to.
	 * @throws Exception
	 */
	public void prepare(Analyzer analyzer) throws Exception {
		if (name == null)
			analyzer.error("No name for a reference");

		if ((updated != null && !updated.equals("-")) || policyOption != null)
			version = max(version, AnnotationReader.V1_2);

		if (target != null) {
			String error = Verifier.validateFilter(target);
			if (error != null)
				analyzer.error("Invalid target filter %s for %s", target, name);
		}

		if (service == null)
			analyzer.error("No interface specified on %s", name);

	}

	/**
	 * Calculate the tag.
	 * 
	 * @return a tag for the reference element.
	 */
	public Tag getTag() {
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

		return ref;
	}

	static <T extends Comparable<T>> T max(T a, T b) {
		int n = a.compareTo(b);
		if (n >= 0)
			return a;
		return b;
	}

	@Override
	public String toString() {
		return name;
	}

}
