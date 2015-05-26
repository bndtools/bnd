package test.annotationheaders.attrs;

import aQute.bnd.annotation.headers.RequireCapability;

@RequireCapability(ns = "nsy", effective = "active", filter = "(foo=bar)")
public @interface AnnotationWithValue {
	String value();
}
