@org.osgi.annotation.versioning.ProviderType
@Export(attribute = {
	"fizz=buzz", "foobar:=fizzbuzz"
}, substitution = Substitution.CONSUMER)
@Version("1.0.0")
package test.export.annotation.testConsumer;

import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.bundle.Export.Substitution;
import org.osgi.annotation.versioning.Version;
