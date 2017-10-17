package test.annotationheaders.attrs.std;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Header;
import org.osgi.annotation.bundle.Requirement;

@Capability(namespace = "provide", name = "Provided")
@Capability(namespace = "provide", name = "Provided2")
@Requirement(namespace = "require", name = "Required", version = "1", filter = "(a=b)")
@Requirement(namespace = "require", name = "Required2", version = "2", filter = "(a=b)")
@Header(name = "foo", value = "bar")
@Header(name = "fizz", value = "buzz")
public @interface AnnotatedAnnotation {
}
