@Export(attribute = {
	"fizz:String=buzz", "foobar:=fizzbuzz", "viking:Version=\"1.2.3.qual\""
}, substitution = Substitution.CALCULATED)
@Version("1.0.0")
package test.export.annotation.testCalculated;

import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.bundle.Export.Substitution;
import org.osgi.annotation.versioning.Version;
