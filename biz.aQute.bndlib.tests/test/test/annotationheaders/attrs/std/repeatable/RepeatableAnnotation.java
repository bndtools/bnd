package test.annotationheaders.attrs.std.repeatable;

import java.lang.annotation.Repeatable;

import org.osgi.annotation.bundle.Header;

@Header(name = "Repeatable", value = "RepeatableAnnotation")
@Repeatable(RepeatableAnnotations.class)
public @interface RepeatableAnnotation {
	int value();
}
