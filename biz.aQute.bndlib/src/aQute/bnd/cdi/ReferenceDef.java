package aQute.bnd.cdi;

import org.osgi.service.component.annotations.ReferenceCardinality;

public class ReferenceDef {
	ReferenceCardinality	cardinality	= ReferenceCardinality.MANDATORY;
	String					service;
}
