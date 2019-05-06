package test.annotationheaders.attrs.defaults;

@SuppressWarnings("deprecation")
@aQute.bnd.annotation.headers.RequireCapability(ns = "default-attrs")
public @interface RequireDefaultAttrs {
	int foo() default 42;
}
