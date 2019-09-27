package test.annotationheaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;

import org.junit.Test;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.version.Version;
import aQute.lib.env.Header;
import aQute.lib.env.Props;
import aQute.lib.filter.Filter;
import aQute.lib.io.IO;

public class CustomAnnotationsTest {

	@Test
	public void testUses() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.uses");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, req.size());
			Props p = req.get("type");
			assertNotNull(p);
			assertEquals("java.io,org.osgi.annotation.bundle", p.get("uses:"));
		}
	}

	@Test
	public void testConversion_Annotation() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.metaAnnotation");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check("No translation found for macro: #value", "No translation found for macro: #array"));

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, req.size());
			Props p = req.get("type");
			assertNotNull(p);
			assertEquals("${#value}", p.get("value"));
			assertEquals("${#array}", p.get("array"));
		}
	}

	@Test
	public void testConversion_Enum() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.metaEnum");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, req.size());
			Props p = req.get("type");
			assertNotNull(p);
			assertEquals("BAR", p.get("value"));
			assertEquals("BAZ,FOO", p.get("array"));
		}
	}

	@Test
	public void testConversion_Class() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.metaClass");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, req.size());
			Props p = req.get("type");
			assertNotNull(p);
			assertEquals("java.lang.Boolean", p.get("value"));
			assertEquals("java.lang.Long,java.lang.Integer,test.annotationheaders.custom.metaClass.ConversionCheck",
				p.get("array"));
			assertEquals("test.annotationheaders.custom.metaClass.ConversionCheck", p.get("host"));
		}
	}

	@Test
	public void testConversion_String() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.metaString");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Parameters req = OSGiHeader.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, req.size());
			Attrs p = req.get("type");
			assertNotNull(p);
			assertEquals("green", p.get("value"));
			assertEquals("red,blue\\,green", p.get("array"));
			assertEquals(Arrays.asList("red", "blue,green"), p.getTyped("array"));
		}
	}

	@Test
	public void testConversion_boolean() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.metaboolean");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, req.size());
			Props p = req.get("type");
			assertNotNull(p);
			assertEquals("false", p.get("value"));
			assertEquals("true,false", p.get("array"));
		}
	}

	@Test
	public void testConversion_char() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.metachar");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, req.size());
			Props p = req.get("type");
			assertNotNull(p);
			assertEquals("a", p.get("value"));
			assertEquals("[,]", p.get("array"));
		}
	}

	@Test
	public void testConversion_double() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.metadouble");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, req.size());
			Props p = req.get("type");
			assertNotNull(p);
			assertEquals("100.0", p.get("value"));
			assertEquals("120.0,15.0", p.get("array"));
		}
	}

	@Test
	public void testConversion_float() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.metafloat");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, req.size());
			Props p = req.get("type");
			assertNotNull(p);
			assertEquals("100.0", p.get("value"));
			assertEquals("120.0,15.0", p.get("array"));
		}
	}

	@Test
	public void testConversion_long() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.metalong");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, req.size());
			Props p = req.get("type");
			assertNotNull(p);
			assertEquals("100", p.get("value"));
			assertEquals("120,15", p.get("array"));
		}
	}

	@Test
	public void testConversion_int() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.metaint");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, req.size());
			Props p = req.get("type");
			assertNotNull(p);
			assertEquals("100", p.get("value"));
			assertEquals("120,15", p.get("array"));
		}
	}

	@Test
	public void testConversion_short() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.metashort");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, req.size());
			Props p = req.get("type");
			assertNotNull(p);
			assertEquals("100", p.get("value"));
			assertEquals("120,15", p.get("array"));
		}
	}

	@Test
	public void testConversion_byte() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.metabyte");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, req.size());
			Props p = req.get("type");
			assertNotNull(p);
			assertEquals("100", p.get("value"));
			assertEquals("120,15", p.get("array"));
		}
	}

	@Test
	public void testServiceConsumer_cardinalityMultiple() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.d");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(2, req.size());

			assertEE(req);

			Props p = req.get("osgi.serviceloader");
			assertNotNull(p);
			assertEquals("test.annotationheaders.custom.d.Consumer", p.get("osgi.serviceloader"));
			assertNotNull(p.get("cardinality:"));
			assertEquals("multiple", p.get("cardinality:"));
			assertNull(p.get("effective:"));
			assertNull(p.get("resolution:"));
			Filter filter = new Filter(p.get(Constants.FILTER_DIRECTIVE));
			Map<String, Object> map = new HashMap<>();
			map.put("osgi.serviceloader", "test.annotationheaders.custom.d.Consumer");
			map.put("version", new Version(1, 0, 0));
			assertTrue(filter.matchMap(map));
		}
	}

	@Test
	public void testServiceConsumer_resolutionOptional() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.c");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(2, req.size());

			assertEE(req);

			Props p = req.get("osgi.serviceloader");
			assertNotNull(p);
			assertEquals("test.annotationheaders.custom.c.Consumer", p.get("osgi.serviceloader"));
			assertNull(p.get("cardinality:"));
			assertNull(p.get("effective:"));
			assertNotNull(p.get("resolution:"));
			assertEquals("optional", p.get("resolution:"));
			Filter filter = new Filter(p.get(Constants.FILTER_DIRECTIVE));
			Map<String, Object> map = new HashMap<>();
			map.put("osgi.serviceloader", "test.annotationheaders.custom.c.Consumer");
			map.put("version", new Version(1, 0, 0));
			assertTrue(filter.matchMap(map));
		}
	}

	@Test
	public void testServiceConsumer_effectiveActive() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.b");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(2, req.size());

			assertEE(req);

			Props p = req.get("osgi.serviceloader");
			assertNotNull(p);
			assertEquals("test.annotationheaders.custom.b.Consumer", p.get("osgi.serviceloader"));
			assertNull(p.get("cardinality:"));
			assertNotNull(p.get("effective:"));
			assertEquals("active", p.get("effective:"));
			assertNull(p.get("resolution:"));
			Filter filter = new Filter(p.get(Constants.FILTER_DIRECTIVE));
			Map<String, Object> map = new HashMap<>();
			map.put("osgi.serviceloader", "test.annotationheaders.custom.b.Consumer");
			map.put("version", new Version(1, 0, 0));
			assertTrue(filter.matchMap(map));
		}
	}

	@Test
	public void testServiceConsumerAnnotation() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.a");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(2, req.size());

			assertEE(req);

			Props p = req.get("osgi.serviceloader");
			assertNotNull(p);
			assertEquals("test.annotationheaders.custom.a.Consumer", p.get("osgi.serviceloader"));
			assertEquals("roger", p.get("foo"));
			Filter filter = new Filter(p.get(Constants.FILTER_DIRECTIVE));
			Map<String, Object> map = new HashMap<>();
			map.put("osgi.serviceloader", "test.annotationheaders.custom.a.Consumer");
			map.put("version", new Version(1, 0, 0));
			assertTrue(filter.matchMap(map));
		}
	}

	@Test
	public void testServiceProvider_serviceProperties() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.e");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(1, req.size());

			assertEE(req);

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, cap.size());

			Props p = cap.get("osgi.serviceloader");
			assertNotNull(p);
			assertNotNull(p.get("osgi.serviceloader"));
			assertEquals("test.annotationheaders.custom.a.Consumer", p.get("osgi.serviceloader"));
			assertNotNull(p.get("register:"));
			assertEquals("test.annotationheaders.custom.e.Provider", p.get("register:"));
			assertNotNull(p.get("foo"));
			assertEquals("bar", p.get("foo"));
			assertNotNull(p.get("service.ranking:Integer"));
			assertEquals("5", p.get("service.ranking:Integer"));
		}
	}

	@Test
	public void testServiceProvider() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.custom.provider");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(1, req.size());

			assertEE(req);

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, cap.size());

			Props p = cap.get("osgi.serviceloader");
			assertNotNull(p);
			assertNotNull(p.get("osgi.serviceloader"));
			assertEquals("test.annotationheaders.custom.a.Consumer", p.get("osgi.serviceloader"));
			assertNotNull(p.get("register:"));
			assertEquals("test.annotationheaders.custom.provider.Provider", p.get("register:"));
			assertNull(p.get("foo"));
			assertNull(p.get("service.ranking:Integer"));
		}
	}

	void assertEE(Header req) throws Exception {
		Props p = req.get("osgi.ee");
		assertNotNull(p);
		Filter filter = new Filter(p.get(Constants.FILTER_DIRECTIVE));
		Map<String, Object> map = new HashMap<>();
		map.put("osgi.ee", "JavaSE");
		map.put("version", new Version(1, 8, 0));
		assertTrue(filter.matchMap(map));
	}

}
