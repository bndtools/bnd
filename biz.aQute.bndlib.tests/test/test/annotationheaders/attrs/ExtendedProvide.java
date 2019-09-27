package test.annotationheaders.attrs;

@SuppressWarnings("deprecation")
@aQute.bnd.annotation.headers.ProvideCapability(ns = "nsx", name = "ExtendedProvide", version = "1.2.3")
public @interface ExtendedProvide {
	byte foo();

	double bar();
}
