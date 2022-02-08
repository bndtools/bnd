package test.annotationheaders.attrs.std.b;

import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Directive;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.resource.Namespace;

@Custom(name = "bar", cardinality = Namespace.CARDINALITY_MULTIPLE)
public class CardinalityDirectiveOverride {}

@Requirement(namespace = "foo")
@interface Custom {
	@Attribute("foo")
	String name();

	@Directive
	String cardinality() default Namespace.CARDINALITY_SINGLE;
}
