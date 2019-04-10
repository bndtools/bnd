package test.annotationheaders.attrs;

@SuppressWarnings("deprecation")
@aQute.bnd.annotation.headers.RequireCapability(ns = "nsx", effective = "active", filter = "(foo=bar)")
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
