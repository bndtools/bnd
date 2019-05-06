package test.annotationheaders.attrs.defaults;

@SuppressWarnings("deprecation")
@aQute.bnd.annotation.headers.ProvideCapability(ns = "default-attrs")
public @interface ProvideDefaultAttrs {
	int foo() default 42;
}
