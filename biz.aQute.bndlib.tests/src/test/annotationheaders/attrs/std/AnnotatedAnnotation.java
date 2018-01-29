package test.annotationheaders.attrs.std;

import static org.osgi.annotation.bundle.Requirement.Cardinality.MULTIPLE;
import static org.osgi.annotation.bundle.Requirement.Resolution.OPTIONAL;

import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Header;
import org.osgi.annotation.bundle.Requirement;

@Capability(namespace = "provide", name = "Provided")
@Capability(namespace = "provide", name = "Provided2", version = "2")
@Requirement(namespace = "require", name = "Required", version = "1", filter = "(a=b)")
@Requirement(namespace = "require", name = "Required2", version = "2", filter = "(a=b)")
@Requirement(namespace = "maybe", name = "test", resolution = OPTIONAL, cardinality = MULTIPLE)
@Header(name = "Foo", value = "bar")
@Header(name = "Fizz", value = "buzz")
public @interface AnnotatedAnnotation {

	@Attribute("open")
	String ignoredName();

	@Attribute
	String usedName();
}
