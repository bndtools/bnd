package test.annotationheaders.attrs.std;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Header;
import org.osgi.annotation.bundle.Requirement;

@DirectlyAnnotatedAnnotation
public @interface IndirectlyAnnotatedAnnotation {

}

@Capability(namespace = "provide", name = "Indirectly-Provided")
@Capability(namespace = "provide", name = "Indirectly-Provided2", version = "2")
@Requirement(namespace = "require", name = "Indirectly-Required", version = "1", filter = "(a=b)")
@Requirement(namespace = "require", name = "Indirectly-Required2", version = "2", filter = "(a=b)")
@Header(name = "Foo2", value = "Indirectly-bar")
@Header(name = "Fizz2", value = "Indirectly-buzz")
@interface DirectlyAnnotatedAnnotation {

}
