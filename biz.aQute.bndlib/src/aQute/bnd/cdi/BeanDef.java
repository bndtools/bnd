package aQute.bnd.cdi;

import java.util.ArrayList;
import java.util.List;

import aQute.bnd.osgi.Annotation.ElementType;
import aQute.bnd.osgi.Descriptors.TypeRef;

public class BeanDef {
	TypeRef						implementation;
	boolean						marked		= false;
	final List<ReferenceDef>	references	= new ArrayList<>();
	final List<TypeRef>			service		= new ArrayList<>();
	ElementType					serviceOrigin;
}
