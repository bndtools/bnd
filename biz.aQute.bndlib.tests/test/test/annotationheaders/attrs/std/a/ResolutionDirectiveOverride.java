package test.annotationheaders.attrs.std.a;

import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Directive;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.resource.Namespace;

@Custom(name = "bar", resolution = Namespace.RESOLUTION_OPTIONAL)
public class ResolutionDirectiveOverride {}

@Requirement(namespace = "foo")
@interface Custom {
	@Attribute("foo")
	String name();

	@Directive
	String resolution() default Namespace.RESOLUTION_MANDATORY;
}
