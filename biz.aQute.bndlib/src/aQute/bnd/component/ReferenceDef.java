package aQute.bnd.component;

import org.osgi.service.component.annotations.*;

import aQute.lib.osgi.*;
import aQute.lib.tag.*;
import aQute.libg.version.*;

class ReferenceDef {
	Version					version;
	String					name;
	String					interfce;
	ReferenceCardinality	cardinality;
	ReferencePolicy			policy;
	String					target;
	String					bind;
	String					unbind;
	String					modified;

	public void prepare(Analyzer analyzer) {
		if (name == null)
			analyzer.error("No name for a reference");

		if (version == null)
			version = AnnotationReader.V1_1;

	}

	public Tag getTag() {
		Tag ref = new Tag("reference");
		ref.addAttribute("name", name);
		if (cardinality != null)
			ref.addAttribute("cardinality", p(cardinality));
		if (policy != null)
			ref.addAttribute("policy", p(policy));

		if (interfce != null)
			ref.addAttribute("interface", interfce);

		if (target != null)
			ref.addAttribute("target", target);

		if (bind != null)
			ref.addAttribute("bind", bind);

		if (unbind != null)
			ref.addAttribute("unbind", unbind);

		if (modified != null) {
			ref.addAttribute("modified", modified);
			version = max(version, AnnotationReader.V1_2);
		}

		return ref;
	}

	private String p(ReferencePolicy policy) {
		switch(policy) {
		case DYNAMIC:
			return "dynamic";
			
		case STATIC:
			return "static";
		}
		return policy.toString();
	}

	private String p(ReferenceCardinality crd) {
		switch (crd) {
		case AT_LEAST_ONE:
			return "1..n";

		case MANDATORY:
			return "1..1";

		case MULTIPLE:
			return "0..n";

		case OPTIONAL:
			return "0..1";

		}
		return crd.toString();
	}

	private <T extends Comparable<T>> T max(T a, T b) {
		int n = a.compareTo(b);
		if (n >= 0)
			return a;
		else
			return b;
	}

	public String toString() {
		return name;
	}

}
