package test.annotationheaders.attrs;

import aQute.bnd.annotation.headers.RequireCapability;

@RequireCapability(ns = "param", effective = "active", filter = "(&(a=${param1})(b=${param2}))")
public @interface ParameterisedAnnotation {
	String param1();

	String param2();
}
