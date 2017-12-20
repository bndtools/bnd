@Export(attribute = {
		"fizz=buzz", "foobar:=fizzbuzz"
}, substitution = Substitution.PROVIDER)
@Version("1.0.0")
package test.export.annotation.testProvider;

import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.bundle.Export.Substitution;
import org.osgi.annotation.versioning.Version;
