package test.annotationheaders.attrs;

import aQute.bnd.annotation.headers.*;

@ProvideCapability(ns="nsx", name="ExtendedProvide")
public @interface ExtendedProvide {
	byte foo();
}
