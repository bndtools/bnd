package test.annotationheaders;

import static aQute.lib.env.Header.DUPLICATE_MARKER;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.lib.env.Header;
import aQute.lib.env.Props;
import aQute.lib.io.IO;
import junit.framework.TestCase;

/**
 * This class mirrors the testing from {@link AnnotationHeadersTest} but using
 * the OSGi R7 standard annotations. Annotations and annotated types are present
 * in the test.annotationheaders.xxx.std packages
 */
public class StdAnnotationHeadersTest extends TestCase {

	public void testCardinalityDirectiveOverride() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.attrs.std.b");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			Props p = req.get("foo");
			assertNotNull(p);
			assertTrue(p.containsKey("foo"));
			assertEquals("bar", p.get("foo"));
			assertTrue(p.containsKey("cardinality:"));
			assertEquals("multiple", p.get("cardinality:"));
		}
	}

	public void testResolutionDirectiveOverride() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.attrs.std.a");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			Props p = req.get("foo");
			assertNotNull(p);
			assertTrue(p.containsKey("foo"));
			assertEquals("bar", p.get("foo"));
			assertTrue(p.containsKey("resolution:"));
			assertEquals("optional", p.get("resolution:"));
		}
	}

	/**
	 * A directly annotated class
	 */

	public void testStdAnnotations() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.multiple.std");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			Props p = req.get("require");
			assertNotNull(p);
			assertTrue(p.containsKey("filter:"));
			assertTrue(p.get("filter:")
				.contains("(a=b)"));
			assertTrue(p.get("filter:")
				.contains("(version>=1.0.0)(!(version>=2.0.0))"));
			assertTrue(p.get("filter:")
				.contains("(require=Required)"));
			assertFalse(p.containsKey("foo"));

			p = req.get("require" + Header.DUPLICATE_MARKER);
			assertNotNull(p);
			assertTrue(p.containsKey("filter:"));
			assertTrue(p.get("filter:")
				.contains("(a=b)"));
			assertTrue(p.get("filter:")
				.contains("(version>=2.0.0)(!(version>=3.0.0))"));
			assertTrue(p.get("filter:")
				.contains("(require=Required2)"));
			assertEquals("direct", p.get("foo"));

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			p = cap.get("provide");
			assertNotNull(p);
			assertTrue(p.containsKey("provide"));
			assertEquals("Provided", p.get("provide"));
			assertFalse(p.containsKey("uses:"));
			assertFalse(p.containsKey("foo"));

			p = cap.get("provide" + Header.DUPLICATE_MARKER);
			assertNotNull(p);
			assertTrue(p.containsKey("provide"));
			assertEquals("Provided2", p.get("provide"));
			assertEquals("2", p.get("version:Version"));
			assertEquals("test.annotationheaders.multiple.std,org.osgi.annotation.bundle", p.get("uses:"));
			assertEquals("direct", p.get("foo"));

			assertEquals("bar", mainAttributes.getValue("Foo"));
			assertEquals("buzz", mainAttributes.getValue("Fizz"));

		}
	}

	/**
	 * A Meta annotated class
	 *
	 * @throws Exception
	 */
	public void testStdAnnotationsMetaAnnotated() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.attrs.std");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			Props p = req.get("require");
			assertNotNull(p);
			assertTrue(p.containsKey("filter:"));
			String filter = p.get("filter:");
			assertTrue(filter, filter.contains("(a=b)"));
			assertTrue(filter, filter.contains("(version>=1.0.0)(!(version>=2.0.0))"));
			assertTrue(filter, filter.contains("(require=Indirectly-Required)"));

			p = req.get("require" + DUPLICATE_MARKER);
			assertNotNull(p);
			assertTrue(p.containsKey("filter:"));
			filter = p.get("filter:");
			assertTrue(filter, filter.contains("(a=b)"));
			assertTrue(filter, filter.contains("(version>=2.0.0)(!(version>=3.0.0))"));
			assertTrue(filter, filter.contains("(require=Indirectly-Required2)"));

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			p = cap.get("provide");
			assertNotNull(p);
			assertTrue(p.containsKey("provide"));
			assertEquals("Indirectly-Provided", p.get("provide"));

			p = cap.get("provide" + DUPLICATE_MARKER);
			assertNotNull(p);
			assertTrue(p.containsKey("provide"));
			assertEquals("Indirectly-Provided2", p.get("provide"));
			assertEquals("2", p.get("version:Version"));

			assertEquals("bar", mainAttributes.getValue("Foo"));
			assertEquals("buzz", mainAttributes.getValue("Fizz"));

			// Indirect annotation

			p = req.get("require" + DUPLICATE_MARKER + DUPLICATE_MARKER);
			assertNotNull(p);
			assertTrue(p.containsKey("filter:"));
			filter = p.get("filter:");
			assertTrue(filter, filter.contains("(a=b)"));
			assertTrue(filter, filter.contains("(version>=1.0.0)(!(version>=2.0.0))"));
			assertTrue(filter, filter.contains("(require=Required)"));
			assertTrue(p.containsKey("open"));
			assertEquals("sesame", p.get("open"));
			assertTrue(p.containsKey("usedName:List<String>"));
			assertEquals("sunflower,marigold", p.get("usedName:List<String>"));
			assertTrue(p.containsKey("number:Long"));
			assertEquals("42", p.get("number:Long"));
			assertFalse(p.containsKey("ignoredName"));
			assertTrue(p.containsKey("x-open:"));
			assertEquals("seed", p.get("x-open:"));
			assertTrue(p.containsKey("x-anotherUsedName:"));
			assertEquals("foo,bar", p.get("x-anotherUsedName:"));
			assertTrue(p.containsKey("x-anotherNumber:"));
			assertEquals("17", p.get("x-anotherNumber:"));
			assertFalse(p.containsKey("anotherIgnoredName"));
			assertFalse(p.containsKey("anotherIgnoredName:"));

			p = req.get("require" + DUPLICATE_MARKER + DUPLICATE_MARKER + DUPLICATE_MARKER);
			assertNotNull(p);
			assertTrue(p.containsKey("filter:"));
			filter = p.get("filter:");
			assertTrue(filter, filter.contains("(a=b)"));
			assertTrue(filter, filter.contains("(version>=2.0.0)(!(version>=3.0.0))"));
			assertTrue(filter, filter.contains("(require=Required2)"));
			assertTrue(p.containsKey("open"));
			assertEquals("sesame", p.get("open"));
			assertTrue(p.containsKey("usedName:List<String>"));
			assertEquals("sunflower,marigold", p.get("usedName:List<String>"));
			assertTrue(p.containsKey("number:Long"));
			assertEquals("42", p.get("number:Long"));
			assertFalse(p.containsKey("ignoredName"));
			assertEquals("meta", p.get("foo"));
			assertTrue(p.containsKey("x-open:"));
			assertEquals("seed", p.get("x-open:"));
			assertTrue(p.containsKey("x-anotherUsedName:"));
			assertEquals("foo,bar", p.get("x-anotherUsedName:"));
			assertTrue(p.containsKey("x-anotherNumber:"));
			assertEquals("17", p.get("x-anotherNumber:"));
			assertFalse(p.containsKey("anotherIgnoredName"));
			assertFalse(p.containsKey("anotherIgnoredName:"));

			p = req.get("maybe");
			assertNotNull(p);
			assertTrue(p.containsKey("filter:"));
			assertEquals("(maybe=test)", p.get("filter:"));
			assertEquals("optional", p.get("resolution:"));
			assertEquals("multiple", p.get("cardinality:"));
			assertTrue(p.containsKey("open"));
			assertEquals("sesame", p.get("open"));
			assertTrue(p.containsKey("usedName:List<String>"));
			assertEquals("sunflower,marigold", p.get("usedName:List<String>"));
			assertTrue(p.containsKey("number:Long"));
			assertEquals("42", p.get("number:Long"));
			assertFalse(p.containsKey("ignoredName"));
			assertTrue(p.containsKey("x-open:"));
			assertEquals("seed", p.get("x-open:"));
			assertTrue(p.containsKey("x-anotherUsedName:"));
			assertEquals("foo,bar", p.get("x-anotherUsedName:"));
			assertTrue(p.containsKey("x-anotherNumber:"));
			assertEquals("17", p.get("x-anotherNumber:"));
			assertFalse(p.containsKey("anotherIgnoredName"));
			assertFalse(p.containsKey("anotherIgnoredName:"));

			// These two values are out of order with respect to the annotations
			// due to the TreeSet sorting we do on the values. This has been
			// done for a long time so I won't change it...
			p = cap.get("provide" + DUPLICATE_MARKER + DUPLICATE_MARKER);
			assertNotNull(p);
			assertTrue(p.containsKey("provide"));
			assertEquals("Provided2", p.get("provide"));
			assertEquals("2", p.get("version:Version"));
			assertTrue(p.containsKey("open"));
			assertEquals("sesame", p.get("open"));
			assertTrue(p.containsKey("usedName:List<String>"));
			assertEquals("sunflower,marigold", p.get("usedName:List<String>"));
			assertTrue(p.containsKey("number:Long"));
			assertEquals("42", p.get("number:Long"));
			assertFalse(p.containsKey("ignoredName"));
			assertEquals("meta", p.get("foo"));
			assertTrue(p.containsKey("x-open:"));
			assertEquals("seed", p.get("x-open:"));
			assertTrue(p.containsKey("x-anotherUsedName:"));
			assertEquals("foo,bar", p.get("x-anotherUsedName:"));
			assertTrue(p.containsKey("x-anotherNumber:"));
			assertEquals("17", p.get("x-anotherNumber:"));
			assertFalse(p.containsKey("anotherIgnoredName"));
			assertFalse(p.containsKey("anotherIgnoredName:"));

			p = cap.get("provide" + DUPLICATE_MARKER + DUPLICATE_MARKER + DUPLICATE_MARKER);
			assertNotNull(p);
			assertTrue(p.containsKey("provide"));
			assertEquals("Provided", p.get("provide"));
			assertTrue(p.containsKey("open"));
			assertEquals("sesame", p.get("open"));
			assertTrue(p.containsKey("usedName:List<String>"));
			assertEquals("sunflower,marigold", p.get("usedName:List<String>"));
			assertTrue(p.containsKey("number:Long"));
			assertEquals("42", p.get("number:Long"));
			assertFalse(p.containsKey("ignoredName"));
			assertTrue(p.containsKey("x-open:"));
			assertEquals("seed", p.get("x-open:"));
			assertTrue(p.containsKey("x-anotherUsedName:"));
			assertEquals("foo,bar", p.get("x-anotherUsedName:"));
			assertTrue(p.containsKey("x-anotherNumber:"));
			assertEquals("17", p.get("x-anotherNumber:"));
			assertFalse(p.containsKey("anotherIgnoredName"));
			assertFalse(p.containsKey("anotherIgnoredName:"));

			assertEquals("Indirectly-bar", mainAttributes.getValue("Foo2"));
			assertEquals("Indirectly-buzz", mainAttributes.getValue("Fizz2"));
		}
	}

	/**
	 * Overriding directives and attributes at many levels for the Requirement
	 * and Capability annotation
	 *
	 * @throws Exception
	 */
	public void testStdAnnotationsOverrideAttrsAndDirectives() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.attrs.std");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			Props p = req.get("overriding");
			assertNotNull(p);
			assertTrue(p.containsKey("filter:"));
			assertEquals("(&(overriding=foo)(version>=1.0.0)(!(version>=2.0.0)))", p.get("filter:"));
			assertTrue(p.containsKey("foo:List<String>"));
			assertEquals("foobar", p.get("foo:List<String>"));
			assertTrue(p.containsKey("x-top-name:"));
			assertEquals("fizzbuzz", p.get("x-top-name:"));
			assertTrue(p.containsKey("name"));
			assertEquals("Steve", p.get("name"));
			assertTrue(p.containsKey("x-name:"));
			assertEquals("Dave", p.get("x-name:"));

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			p = cap.get("overriding");
			assertNotNull(p);
			assertTrue(p.containsKey("overriding"));
			assertEquals("foo", p.get("overriding"));
			assertTrue(p.containsKey("version:Version"));
			assertEquals("1", p.get("version:Version"));
			assertTrue(p.containsKey("foo:List<String>"));
			assertEquals("foobar", p.get("foo:List<String>"));
			assertTrue(p.containsKey("x-top-name:"));
			assertEquals("fizzbuzz", p.get("x-top-name:"));
			assertTrue(p.containsKey("name"));
			assertEquals("Steve", p.get("name"));
			assertTrue(p.containsKey("x-name:"));
			assertEquals("Dave", p.get("x-name:"));

		}
	}

	/**
	 * Test support for version macros in Requirement and Capability annotation
	 *
	 * @throws Exception
	 */
	public void testStdAnnotationsMacroVersions() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.attrs.std.versioned");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			Props p = req.get("overriding");
			assertNotNull(p);
			assertTrue(p.containsKey("filter:"));
			assertEquals("(&(overriding=foo)(version>=1.0.0)(!(version>=2.0.0)))", p.get("filter:"));

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			p = cap.get("overriding");
			assertNotNull(p);
			assertTrue(p.containsKey("overriding"));
			assertEquals("foo", p.get("overriding"));
			assertTrue(p.containsKey("version:Version"));
			assertEquals("1.0.0", p.get("version:Version"));
		}
	}

	/**
	 * A Meta annotated class using Repeatable annotations
	 *
	 * @throws Exception
	 */
	public void testStdRepeatableMetaAnnotated() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.attrs.std.repeatable");
			b.build();
			assertTrue(b.check());
			Manifest manifest = b.getJar()
				.getManifest();
			manifest.write(System.out);

			Attributes mainAttributes = manifest.getMainAttributes();

			Header repeated = Header.parseHeader(mainAttributes.getValue("Repeatable"));
			assertThat(repeated).containsOnlyKeys("RepeatableAnnotation");
			Header container = Header.parseHeader(mainAttributes.getValue("Container"));
			assertThat(container).containsOnlyKeys("RepeatableAnnotations");
		}
	}

	/**
	 * Override bundle annotation processing with the `-bundleannotations`
	 * instruction
	 *
	 * @throws Exception
	 */
	public void testOverrideBundleAnnotationsInstruction_success() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.attrs.std.activator");
			b.setProperty(Constants.BUNDLE_ACTIVATOR,
				"test.annotationheaders.attrs.std.activator.TypeInVersionedPackage2");
			b.setProperty("-bundleannotations", "!test.annotationheaders.attrs.std.activator.TypeInVersionedPackage,*");
			b.build();
			assertTrue(b.check());
			Manifest manifest = b.getJar()
				.getManifest();
			manifest.write(System.out);

			Attributes mainAttributes = manifest.getMainAttributes();

			Header repeated = Header.parseHeader(mainAttributes.getValue(Constants.BUNDLE_ACTIVATOR));
			assertThat(repeated).containsOnlyKeys("test.annotationheaders.attrs.std.activator.TypeInVersionedPackage2");
		}
	}

}
