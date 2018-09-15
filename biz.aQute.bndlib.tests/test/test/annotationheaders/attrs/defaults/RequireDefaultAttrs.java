package test.annotationheaders.attrs.defaults;

import aQute.bnd.annotation.headers.RequireCapability;

@RequireCapability(ns = "default-attrs")
public @interface RequireDefaultAttrs {
	int foo() default 42;
}
