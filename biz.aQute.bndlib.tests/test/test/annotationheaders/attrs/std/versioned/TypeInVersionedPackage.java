package test.annotationheaders.attrs.std.versioned;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Requirement;

@Capability(namespace = "overriding", name = "foo", version = "${@version}")
@Requirement(namespace = "overriding", name = "foo", version = "${@version}")
public class TypeInVersionedPackage {

}
