package test.annotationheaders.attrs;

import aQute.bnd.annotation.headers.ProvideCapability;

@ProvideCapability(ns = "nsx", name = "ExtendedProvide", version = "1.2.3")
public @interface ExtendedProvide {
	byte foo();

	double bar();
}
