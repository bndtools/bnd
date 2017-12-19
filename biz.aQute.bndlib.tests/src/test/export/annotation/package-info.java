@org.osgi.annotation.bundle.Export(uses = {
		"foo", "bar"
}, attribute = {
		"fizz=buzz", "foobar:=fizzbuzz"
})
@org.osgi.annotation.versioning.Version("1.0.0")
package test.export.annotation;