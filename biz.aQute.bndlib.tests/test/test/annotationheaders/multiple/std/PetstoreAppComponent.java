package test.annotationheaders.multiple.std;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Header;
import org.osgi.annotation.bundle.Requirement;

@Capability(namespace = "provide", name = "Provided", uses = {}, attribute = {})
@Capability(namespace = "provide", name = "Provided2", version = "2", uses = {
	PetstoreAppComponent.class, Capability.class
}, attribute = {
	"foo=direct"
})
@Requirement(namespace = "require", name = "Required", version = "1", filter = "(a=b)", attribute = {})
@Requirement(namespace = "require", name = "Required2", version = "2", filter = "(a=b)", attribute = {
	"foo=direct"
})
@Header(name = "Foo", value = "bar")
@Header(name = "Fizz", value = "buzz")
public class PetstoreAppComponent {
	// ..
}
