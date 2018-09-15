package aQute.bnd.cdi;

import aQute.bnd.component.annotations.ReferenceCardinality;

public class ReferenceDef {
	ReferenceCardinality	cardinality	= ReferenceCardinality.MANDATORY;
	String					service;
}
