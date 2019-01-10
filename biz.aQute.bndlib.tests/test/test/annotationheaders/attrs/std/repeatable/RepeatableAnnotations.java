package test.annotationheaders.attrs.std.repeatable;

import org.osgi.annotation.bundle.Header;

@Header(name = "Container", value = "RepeatableAnnotations")
public @interface RepeatableAnnotations {
	RepeatableAnnotation[] value();
}
