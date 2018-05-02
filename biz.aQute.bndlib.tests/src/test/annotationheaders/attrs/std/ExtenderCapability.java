package test.annotationheaders.attrs.std;

import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;

import org.osgi.annotation.bundle.Capability;

@Capability(namespace = EXTENDER_NAMESPACE, version = "1.0.0")
public @interface ExtenderCapability {
	// Adds a name that gets into the generated filter
	String name();

	// overrides the version if set (and affects the generated filter)
	String version() default "0";
}
