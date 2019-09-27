package test.annotationheaders.attrs.std.a;

import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Directive;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.annotation.bundle.Requirement.Resolution;

@Custom(name = "bar", resolution = Resolution.OPTIONAL)
public class ResolutionDirectiveOverride {}

@Requirement(namespace = "foo")
@interface Custom {
	@Attribute("foo")
	String name();

	@Directive
	Resolution resolution() default Resolution.MANDATORY;
}
