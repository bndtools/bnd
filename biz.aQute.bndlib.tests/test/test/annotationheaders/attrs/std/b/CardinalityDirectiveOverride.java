package test.annotationheaders.attrs.std.b;

import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Directive;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.annotation.bundle.Requirement.Cardinality;

@Custom(name = "bar", cardinality = Cardinality.MULTIPLE)
public class CardinalityDirectiveOverride {}

@Requirement(namespace = "foo")
@interface Custom {
	@Attribute("foo")
	String name();

	@Directive
	Cardinality cardinality() default Cardinality.SINGLE;
}
