package test.annotationheaders.attrs;

import aQute.bnd.annotation.headers.RequireCapability;

@RequireCapability(ns = "nsx", effective = "active", filter = "(foo=bar)")
public @interface AnnotationWithAttrs {
	enum E {
		A,
		B,
		C
	}

	String[] foo();

	int bar();

	E en();
}
