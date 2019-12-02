package test.annotationheaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;

import org.junit.Test;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.version.Version;
import aQute.lib.env.Header;
import aQute.lib.env.Props;
import aQute.lib.filter.Filter;
import aQute.lib.io.IO;

public class SPIAnnotationsTest {

	@Test
	public void testServiceProviderOnPackage() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.providerF");
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

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(2, cap.size());

			Props p = cap.get("osgi.serviceloader");
			assertNotNull(p);
			assertNotNull(p.get("osgi.serviceloader"));
			assertEquals("test.annotationheaders.spi.SPIService", p.get("osgi.serviceloader"));
			assertNotNull(p.get("register:"));
			assertEquals("test.annotationheaders.spi.providerF.Provider", p.get("register:"));

			assertServiceMappingFile(b.getJar(), "test.annotationheaders.spi.SPIService",
				"test.annotationheaders.spi.providerF.Provider");
		}
	}

	@Test
	public void testServiceProvider_existingdescriptor() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.providerE");
			b.setProperty("-includeresource",
				"META-INF/services/test.annotationheaders.spi.SPIService;literal='test.annotationheaders.spi.providerE.Provider'");
			b.setProperty("Provide-Capability",
				"osgi.serviceloader;osgi.serviceloader='test.annotationheaders.spi.SPIService';register:='test.annotationheaders.spi.providerE.Provider'");
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
			assertEquals("test.annotationheaders.spi.SPIService", p.get("osgi.serviceloader"));
			assertNotNull(p.get("register:"));
			assertEquals("test.annotationheaders.spi.providerE.Provider", p.get("register:"));

			assertServiceMappingFile(b.getJar(), "test.annotationheaders.spi.SPIService",
				"test.annotationheaders.spi.providerE.Provider");
		}
	}

	@Test
	public void testServiceProvider_warning() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.providerE");
			b.setProperty("-includeresource",
				"META-INF/services/test.annotationheaders.spi.SPIService=testresources/services");
			b.setProperty("Provide-Capability",
				"osgi.serviceloader;osgi.serviceloader=\"test.annotationheaders.spi.SPIService\"");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check(
				"osgi.serviceloader capability found with no 'register:' directive. Descriptor cannot be managed for osgi.serviceloader;osgi.serviceloader=\"test.annotationheaders.spi.SPIService\""));

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
			assertEquals("test.annotationheaders.spi.SPIService", p.get("osgi.serviceloader"));
			assertNull(p.get("register:"));

			assertServiceMappingFile(b.getJar(), "test.annotationheaders.spi.SPIService",
				"another.provider.ProviderImpl");
		}
	}

	@Test
	public void testServiceProvider_mergeDescriptor() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.providerD");
			b.setProperty("-includeresource",
				"META-INF/services/test.annotationheaders.spi.SPIService=testresources/services");
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

			assertExtender(req, "osgi.serviceloader.registrar");
			assertEE(req);

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(2, cap.size());

			Props p = cap.get("osgi.serviceloader");
			assertNotNull(p);
			assertNotNull(p.get("osgi.serviceloader"));
			assertEquals("test.annotationheaders.spi.SPIService", p.get("osgi.serviceloader"));
			assertNotNull(p.get("register:"));
			assertThat(p.get("register:")).isIn("test.annotationheaders.spi.providerD.Provider");
			assertNotNull(p.get("foo"));
			assertEquals("bar", p.get("foo"));

			assertServiceMappingFile(b.getJar(), "test.annotationheaders.spi.SPIService",
				"test.annotationheaders.spi.providerD.Provider");
			assertServiceMappingFile(b.getJar(), "test.annotationheaders.spi.SPIService",
				"another.provider.ProviderImpl");
		}
	}

	@Test
	public void testServiceConsumerMetaAnnotatingCustom() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.consumerE.*");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(3, req.size());

			assertExtender(req, "osgi.serviceloader.processor");

			Props p = req.get("osgi.serviceloader");
			assertNotNull(p);
			assertEquals("test.annotationheaders.spi.SPIService", p.get("osgi.serviceloader"));
		}
	}

	@Test
	public void testFilteredAnalysis() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setProperty(Constants.BUNDLEANNOTATIONS, "test.annotationheaders.spi.providerC.other.*");
			b.setPrivatePackage("test.annotationheaders.spi.providerC.*");
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

			assertExtender(req, "osgi.serviceloader.registrar");
			assertEE(req);

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(2, cap.size());

			Props p = cap.get("osgi.serviceloader");
			assertNotNull(p);
			assertEquals("test.annotationheaders.spi.SPIService", p.get("osgi.serviceloader"));
			assertEquals("test.annotationheaders.spi.providerC.other.Other", p.get("register:"));
			assertNotNull(p.get("foo"));
			assertEquals("bar", p.get("foo"));
		}
	}

	@Test
	public void testServiceConsumerMultiple() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.consumerF");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(4, req.size());

			assertExtender(req, "osgi.serviceloader.processor");
			assertEE(req);

			Props p = req.get("osgi.serviceloader");
			assertNotNull(p);
			assertThat(p.get("osgi.serviceloader")).isIn("test.annotationheaders.spi.SPIService", "java.lang.Integer");
			assertNull(p.get("cardinality:"));
			assertNull(p.get("effective:"));
			assertNull(p.get("resolution:"));

			p = req.get("osgi.extender");
			assertNotNull(p);
			assertNull(p.get("cardinality:"));
			assertNull(p.get("effective:"));
			assertNull(p.get("resolution:"));
		}
	}

	@Test
	public void testServiceConsumer_cardinalityMultiple() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.consumerD");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(3, req.size());

			assertExtender(req, "osgi.serviceloader.processor");
			assertEE(req);

			Props p = req.get("osgi.serviceloader");
			assertNotNull(p);
			assertEquals("test.annotationheaders.spi.SPIService", p.get("osgi.serviceloader"));
			assertNotNull(p.get("cardinality:"));
			assertEquals("multiple", p.get("cardinality:"));
			assertNull(p.get("effective:"));
			assertNull(p.get("resolution:"));
			Filter filter = new Filter(p.get(Constants.FILTER_DIRECTIVE));
			Map<String, Object> map = new HashMap<>();
			map.put("osgi.serviceloader", "test.annotationheaders.spi.SPIService");
			map.put("version", new Version(1, 0, 0));
			assertTrue(filter.matchMap(map));

			p = req.get("osgi.extender");
			assertNotNull(p);
			assertNull(p.get("cardinality:"));
			assertNull(p.get("effective:"));
			assertNull(p.get("resolution:"));
		}
	}

	@Test
	public void testServiceConsumer_resolutionOptional() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.consumerC");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(3, req.size());

			assertExtender(req, "osgi.serviceloader.processor");
			assertEE(req);

			Props p = req.get("osgi.serviceloader");
			assertNotNull(p);
			assertEquals("test.annotationheaders.spi.SPIService", p.get("osgi.serviceloader"));
			assertNull(p.get("cardinality:"));
			assertNull(p.get("effective:"));
			assertNotNull(p.get("resolution:"));
			assertEquals("optional", p.get("resolution:"));
			Filter filter = new Filter(p.get(Constants.FILTER_DIRECTIVE));
			Map<String, Object> map = new HashMap<>();
			map.put("osgi.serviceloader", "test.annotationheaders.spi.SPIService");
			map.put("version", new Version(1, 0, 0));
			assertTrue(filter.matchMap(map));

			p = req.get("osgi.extender");
			assertNotNull(p);
			assertNull(p.get("cardinality:"));
			assertNull(p.get("effective:"));
			assertNotNull(p.get("resolution:"));
			assertEquals("optional", p.get("resolution:"));
		}
	}

	@Test
	public void testServiceConsumer_effectiveActive() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.consumerB");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(3, req.size());

			assertExtender(req, "osgi.serviceloader.processor");
			assertEE(req);

			Props p = req.get("osgi.serviceloader");
			assertNotNull(p);
			assertEquals("test.annotationheaders.spi.SPIService", p.get("osgi.serviceloader"));
			assertNull(p.get("cardinality:"));
			assertNotNull(p.get("effective:"));
			assertEquals("active", p.get("effective:"));
			assertNull(p.get("resolution:"));
			Filter filter = new Filter(p.get(Constants.FILTER_DIRECTIVE));
			Map<String, Object> map = new HashMap<>();
			map.put("osgi.serviceloader", "test.annotationheaders.spi.SPIService");
			map.put("version", new Version(1, 0, 0));
			assertTrue(filter.matchMap(map));

			p = req.get("osgi.extender");
			assertNotNull(p);
			assertNull(p.get("cardinality:"));
			assertNotNull(p.get("effective:"));
			assertEquals("active", p.get("effective:"));
			assertNull(p.get("resolution:"));
		}
	}

	@Test
	public void testServiceConsumerAnnotation() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.consumer");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(3, req.size());

			assertExtender(req, "osgi.serviceloader.processor");
			assertEE(req);

			Props p = req.get("osgi.serviceloader");
			assertNotNull(p);
			assertEquals("test.annotationheaders.spi.SPIService", p.get("osgi.serviceloader"));
			assertNull(p.get("cardinality:"));
			assertNull(p.get("effective:"));
			assertNull(p.get("resolution:"));
			Filter filter = new Filter(p.get(Constants.FILTER_DIRECTIVE));
			Map<String, Object> map = new HashMap<>();
			map.put("osgi.serviceloader", "test.annotationheaders.spi.SPIService");
			map.put("version", new Version(1, 0, 0));
			assertTrue(filter.matchMap(map));

			p = req.get("osgi.extender");
			assertNotNull(p);
			assertNull(p.get("cardinality:"));
			assertNull(p.get("effective:"));
			assertNull(p.get("resolution:"));
		}
	}

	@Test
	public void testServiceProvider_serviceProperties() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.providerB");
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

			assertExtender(req, "osgi.serviceloader.registrar");
			assertEE(req);

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(2, cap.size());

			Props p = cap.get("osgi.serviceloader");
			assertNotNull(p);
			assertNotNull(p.get("osgi.serviceloader"));
			assertEquals("test.annotationheaders.spi.SPIService", p.get("osgi.serviceloader"));
			assertNotNull(p.get("register:"));
			assertEquals("test.annotationheaders.spi.providerB.Provider", p.get("register:"));
			assertNotNull(p.get("foo"));
			assertEquals("bar", p.get("foo"));
			assertNotNull(p.get("foo"));
			assertEquals("bar", p.get("foo"));

			assertServiceMappingFile(b.getJar(), "test.annotationheaders.spi.SPIService",
				"test.annotationheaders.spi.providerB.Provider");
		}
	}

	@Test
	public void testServiceProviderProvidesService() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.provider");
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

			assertExtender(req, "osgi.serviceloader.registrar");
			assertEE(req);

			Parameters cap = OSGiHeader.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(3, cap.size());

			Attrs p = cap.get("osgi.service");
			assertNotNull(p);
			assertNotNull(p.getTyped("objectClass"));
			assertThat(p.getTyped(Attrs.LIST_STRING, "objectClass")).contains("test.annotationheaders.spi.SPIService");
			assertEquals("active", p.get("effective:"));
		}
	}

	@Test
	public void testServiceProvider() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.provider");
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

			assertExtender(req, "osgi.serviceloader.registrar");
			assertEE(req);

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(3, cap.size());

			Props p = cap.get("osgi.serviceloader");
			assertNotNull(p);
			assertNotNull(p.get("osgi.serviceloader"));
			assertEquals("test.annotationheaders.spi.SPIService", p.get("osgi.serviceloader"));
			assertNotNull(p.get("register:"));
			assertThat(p.get("register:")).isIn("test.annotationheaders.spi.provider.Provider",
				"test.annotationheaders.spi.provider.Provider2");
			assertNull(p.get("foo"));
			assertNull(p.get("service.ranking:Integer"));

			assertServiceMappingFile(b.getJar(), "test.annotationheaders.spi.SPIService",
				"test.annotationheaders.spi.provider.Provider");
			assertServiceMappingFile(b.getJar(), "test.annotationheaders.spi.SPIService",
				"test.annotationheaders.spi.provider.Provider2");
		}
	}

	void assertExtender(Header req, String extender) throws Exception {
		Props p = req.get("osgi.extender");
		assertNotNull(p);
		Filter filter = new Filter(p.get(Constants.FILTER_DIRECTIVE));
		Map<String, Object> map = new HashMap<>();
		map.put("osgi.extender", extender);
		map.put("version", new Version(1, 0, 0));
		assertTrue(filter.matchMap(map));
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

	void assertServiceMappingFile(Jar jar, String spi, String impl) throws Exception {
		Resource resource = jar.getResource("META-INF/services/" + spi);
		assertNotNull(resource);
		String contents = IO.collect(resource.openInputStream());
		assertTrue("does not contain " + impl, contents.contains(impl));
	}

}
