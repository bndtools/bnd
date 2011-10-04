package aQute.bnd.make.component;

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
		if ( name == null )
			analyzer.error("No name for a reference");
		
		// TODO 
	}
	
	public Tag getTag() {
		Tag ref = new Tag("reference");
		ref.addAttribute("name", name);
		ref.addAttribute("cardinality", cardinality);
		ref.addAttribute("policy", policy);
		
		if ( interfce != null)
			ref.addAttribute("interface", interfce);
			
		if ( target != null)
			ref.addAttribute("target", target);
			
		if ( bind != null)
			ref.addAttribute("bind", bind);
			
		if ( unbind != null)
			ref.addAttribute("unbind", unbind);
			
		if ( modified != null)
			ref.addAttribute("modified", modified);
		
		return ref;
	}



}
