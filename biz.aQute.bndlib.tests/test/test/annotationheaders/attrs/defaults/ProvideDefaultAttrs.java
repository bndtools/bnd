package test.annotationheaders.attrs.defaults;

import aQute.bnd.annotation.headers.ProvideCapability;

@ProvideCapability(ns = "default-attrs")
public @interface ProvideDefaultAttrs {
	int foo() default 42;
}
