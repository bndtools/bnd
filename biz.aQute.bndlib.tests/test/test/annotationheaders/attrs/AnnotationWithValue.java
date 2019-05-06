package test.annotationheaders.attrs;

@SuppressWarnings("deprecation")
@aQute.bnd.annotation.headers.RequireCapability(ns = "nsy", effective = "active", filter = "(foo=bar)")
public @interface AnnotationWithValue {
	String value();
}
