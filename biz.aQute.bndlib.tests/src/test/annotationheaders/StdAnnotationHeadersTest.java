package test.annotationheaders;

import static aQute.lib.env.Header.DUPLICATE_MARKER;

import java.util.jar.Attributes;

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

	/**
	 * A directly annotated class
	 */

	public void testStdAnnotations() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin"));
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
				.contains("(&(version>=1.0.0)(!(version>=2.0.0)))"));
			assertTrue(p.get("filter:")
				.contains("(require=Required)"));

			p = req.get("require" + Header.DUPLICATE_MARKER);
			assertNotNull(p);
			assertTrue(p.containsKey("filter:"));
			assertTrue(p.get("filter:")
				.contains("(a=b)"));
			assertTrue(p.get("filter:")
				.contains("(&(version>=2.0.0)(!(version>=3.0.0)))"));
			assertTrue(p.get("filter:")
				.contains("(require=Required2)"));

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			p = cap.get("provide");
			assertNotNull(p);
			assertTrue(p.containsKey("provide"));
			assertEquals("Provided", p.get("provide"));

			p = cap.get("provide" + Header.DUPLICATE_MARKER);
			assertNotNull(p);
			assertTrue(p.containsKey("provide"));
			assertEquals("Provided2", p.get("provide"));
			assertEquals("2", p.get("version:Version"));

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
			b.addClasspath(IO.getFile("bin"));
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
			assertTrue(filter, filter.contains("(&(version>=1.0.0)(!(version>=2.0.0)))"));
			assertTrue(filter, filter.contains("(require=Indirectly-Required)"));

			p = req.get("require" + DUPLICATE_MARKER);
			assertNotNull(p);
			assertTrue(p.containsKey("filter:"));
			filter = p.get("filter:");
			assertTrue(filter, filter.contains("(a=b)"));
			assertTrue(filter, filter.contains("(&(version>=2.0.0)(!(version>=3.0.0)))"));
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
			assertTrue(filter, filter.contains("(&(version>=1.0.0)(!(version>=2.0.0)))"));
			assertTrue(filter, filter.contains("(require=Required)"));
			assertTrue(p.containsKey("open"));
			assertEquals("sesame", p.get("open"));
			assertTrue(p.containsKey("usedName:List<String>"));
			assertEquals("sunflower,marigold", p.get("usedName:List<String>"));
			assertTrue(p.containsKey("number:Long"));
			assertEquals("42", p.get("number:Long"));
			assertFalse(p.containsKey("ignoredName"));

			p = req.get("require" + DUPLICATE_MARKER + DUPLICATE_MARKER + DUPLICATE_MARKER);
			assertNotNull(p);
			assertTrue(p.containsKey("filter:"));
			filter = p.get("filter:");
			assertTrue(filter, filter.contains("(a=b)"));
			assertTrue(filter, filter.contains("(&(version>=2.0.0)(!(version>=3.0.0)))"));
			assertTrue(filter, filter.contains("(require=Required2)"));
			assertTrue(p.containsKey("open"));
			assertEquals("sesame", p.get("open"));
			assertTrue(p.containsKey("usedName:List<String>"));
			assertEquals("sunflower,marigold", p.get("usedName:List<String>"));
			assertTrue(p.containsKey("number:Long"));
			assertEquals("42", p.get("number:Long"));
			assertFalse(p.containsKey("ignoredName"));

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

			assertEquals("Indirectly-bar", mainAttributes.getValue("Foo2"));
			assertEquals("Indirectly-buzz", mainAttributes.getValue("Fizz2"));
		}
	}

}
