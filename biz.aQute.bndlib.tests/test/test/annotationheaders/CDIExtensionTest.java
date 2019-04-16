package test.annotationheaders;

import static aQute.lib.exceptions.PredicateWithException.asPredicate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;

import org.junit.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.version.Version;
import aQute.lib.env.Header;
import aQute.lib.env.Props;
import aQute.lib.filter.Filter;
import aQute.lib.io.IO;

public class CDIExtensionTest {

	@Test
	public void CDIExtensionSPIWeak_WithUses() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.cdi.spiweak.e");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(3, cap.size());

			Props p = cap.get("osgi.cdi.extension");
			assertThat(p).isNotNull()
				.extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("version:Version")
				.isNotNull()
				.containsExactly("1.0.0");

			p = cap.get("osgi.service");
			assertThat(p).isNotNull()
				.extracting("objectClass:List<String>")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");

			p = cap.get("osgi.serviceloader");
			assertThat(p).isNotNull()
				.extracting("osgi.serviceloader")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");
			assertThat(p).extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("register:")
				.isNotNull()
				.containsExactly("test.annotationheaders.cdi.spiweak.e.WithUses");
			assertThat(p).extracting("uses:")
				.containsExactly("javax.enterprise.context.spi,javax.enterprise.inject.spi");

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(5, req.size());

			p = req.get("osgi.extender");

			final Map<String, Object> map = new HashMap<>();
			map.put("osgi.extender", "osgi.serviceloader.registrar");
			map.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map)));
			assertThat(p).extracting("cardinality:")
				.containsExactly((Object) null);
			assertThat(p).extracting("effective:")
				.containsExactly("active");
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("uses:")
				.allMatch(o -> o == null);

			p = req.get("osgi.implementation");

			final Map<String, Object> map2 = new HashMap<>();
			map2.put("osgi.implementation", "osgi.cdi");
			map2.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map2)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.containsExactly("active");
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("uses:")
				.allMatch(o -> o == null);
		}
	}

	@Test
	public void CDIExtensionSPIWeak_WithServiceProperty() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.cdi.spiweak.b");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(3, cap.size());

			Props p = cap.get("osgi.cdi.extension");
			assertThat(p).isNotNull()
				.extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("version:Version")
				.isNotNull()
				.containsExactly("1.0.0");

			p = cap.get("osgi.service");
			assertThat(p).isNotNull()
				.extracting("objectClass:List<String>")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");

			p = cap.get("osgi.serviceloader");
			assertThat(p).isNotNull()
				.extracting("osgi.serviceloader")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");
			assertThat(p).extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("register:")
				.isNotNull()
				.containsExactly("test.annotationheaders.cdi.spiweak.b.WithServiceProperty");
			assertThat(p).extracting("foo:Integer")
				.isNotNull()
				.containsExactly("15");
			assertThat(p).extracting("uses:")
				.allMatch(o -> o == null);

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(5, req.size());

			p = req.get("osgi.extender");

			final Map<String, Object> map = new HashMap<>();
			map.put("osgi.extender", "osgi.serviceloader.registrar");
			map.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.containsExactly("active");
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);

			p = req.get("osgi.extender~");

			final Map<String, Object> map2 = new HashMap<>();
			map2.put("osgi.extender", "osgi.serviceloader.registrar");
			map2.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map2)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.containsExactly("optional");

			p = req.get("osgi.implementation");

			final Map<String, Object> map3 = new HashMap<>();
			map3.put("osgi.implementation", "osgi.cdi");
			map3.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map3)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.containsExactly("active");
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);

			p = req.get("osgi.implementation~");

			final Map<String, Object> map4 = new HashMap<>();
			map4.put("osgi.implementation", "osgi.cdi");
			map4.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map4)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.containsExactly("optional");
		}
	}

	@Test
	public void CDIExtensionSPIWeak_Basic() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.cdi.spiweak.a");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(3, cap.size());

			Props p = cap.get("osgi.cdi.extension");
			assertThat(p).isNotNull()
				.extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("version:Version")
				.isNotNull()
				.containsExactly("1.0.0");

			p = cap.get("osgi.service");
			assertThat(p).isNotNull()
				.extracting("objectClass:List<String>")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");

			p = cap.get("osgi.serviceloader");
			assertThat(p).isNotNull()
				.extracting("osgi.serviceloader")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");
			assertThat(p).extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("register:")
				.isNotNull()
				.containsExactly("test.annotationheaders.cdi.spiweak.a.BasicExtension");
			assertThat(p).extracting("uses:")
			.allMatch(o -> o == null);

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(5, req.size());

			p = req.get("osgi.extender");

			final Map<String, Object> map = new HashMap<>();
			map.put("osgi.extender", "osgi.serviceloader.registrar");
			map.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.containsExactly("active");
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);

			p = req.get("osgi.extender~");

			final Map<String, Object> map2 = new HashMap<>();
			map2.put("osgi.extender", "osgi.serviceloader.registrar");
			map2.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map2)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.containsExactly("optional");

			p = req.get("osgi.implementation");

			final Map<String, Object> map3 = new HashMap<>();
			map3.put("osgi.implementation", "osgi.cdi");
			map3.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map3)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.containsExactly("active");
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);

			p = req.get("osgi.implementation~");

			final Map<String, Object> map4 = new HashMap<>();
			map4.put("osgi.implementation", "osgi.cdi");
			map4.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map4)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.containsExactly("optional");
		}
	}

	///////////////////

	@Test
	public void CDIExtensionSPI_WithUses() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.cdi.spi.e");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(3, cap.size());

			Props p = cap.get("osgi.cdi.extension");
			assertThat(p).isNotNull()
				.extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("version:Version")
				.isNotNull()
				.containsExactly("1.0.0");

			p = cap.get("osgi.service");
			assertThat(p).isNotNull()
				.extracting("objectClass:List<String>")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");

			p = cap.get("osgi.serviceloader");
			assertThat(p).isNotNull()
				.extracting("osgi.serviceloader")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");
			assertThat(p).extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("register:")
				.isNotNull()
				.containsExactly("test.annotationheaders.cdi.spi.e.WithUses");
			assertThat(p).extracting("uses:")
				.containsExactly("javax.enterprise.context.spi,javax.enterprise.inject.spi");

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(3, req.size());

			p = req.get("osgi.extender");

			final Map<String, Object> map = new HashMap<>();
			map.put("osgi.extender", "osgi.serviceloader.registrar");
			map.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map)));
			assertThat(p).extracting("cardinality:")
				.containsExactly((Object) null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("uses:")
				.allMatch(o -> o == null);

			p = req.get("osgi.implementation");

			final Map<String, Object> map2 = new HashMap<>();
			map2.put("osgi.implementation", "osgi.cdi");
			map2.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map2)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("uses:")
				.allMatch(o -> o == null);
		}
	}

	@Test
	public void CDIExtensionSPI_ResolutionOptional() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.cdi.spi.d");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(3, cap.size());

			Props p = cap.get("osgi.cdi.extension");
			assertThat(p).isNotNull()
				.extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("version:Version")
				.isNotNull()
				.containsExactly("1.0.0");

			p = cap.get("osgi.service");
			assertThat(p).isNotNull()
				.extracting("objectClass:List<String>")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");

			p = cap.get("osgi.serviceloader");
			assertThat(p).isNotNull()
				.extracting("osgi.serviceloader")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");
			assertThat(p).extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("register:")
				.isNotNull()
				.containsExactly("test.annotationheaders.cdi.spi.d.ResolutionOptional");
			assertThat(p).extracting("uses:")
				.allMatch(o -> o == null);

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(3, req.size());

			p = req.get("osgi.extender");

			final Map<String, Object> map = new HashMap<>();
			map.put("osgi.extender", "osgi.serviceloader.registrar");
			map.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.allMatch(o -> "optional".equals(o));
			assertThat(p).extracting("uses:")
				.allMatch(o -> o == null);

			p = req.get("osgi.implementation");

			final Map<String, Object> map2 = new HashMap<>();
			map2.put("osgi.implementation", "osgi.cdi");
			map2.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map2)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.allMatch(o -> "optional".equals(o));
			assertThat(p).extracting("uses:")
				.allMatch(o -> o == null);
		}
	}

	@Test
	public void CDIExtensionSPI_EffectiveActive() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.cdi.spi.c");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(3, cap.size());

			Props p = cap.get("osgi.cdi.extension");
			assertThat(p).isNotNull()
				.extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("version:Version")
				.isNotNull()
				.containsExactly("1.0.0");

			p = cap.get("osgi.service");
			assertThat(p).isNotNull()
				.extracting("objectClass:List<String>")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");

			p = cap.get("osgi.serviceloader");
			assertThat(p).isNotNull()
				.extracting("osgi.serviceloader")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");
			assertThat(p).extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("register:")
				.isNotNull()
				.containsExactly("test.annotationheaders.cdi.spi.c.EffectiveActive");
			assertThat(p).extracting("uses:")
				.allMatch(o -> o == null);

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(3, req.size());

			p = req.get("osgi.extender");

			final Map<String, Object> map = new HashMap<>();
			map.put("osgi.extender", "osgi.serviceloader.registrar");
			map.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> "active".equals(o));
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);

			p = req.get("osgi.implementation");

			final Map<String, Object> map2 = new HashMap<>();
			map2.put("osgi.implementation", "osgi.cdi");
			map2.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map2)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> "active".equals(o));
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);
		}
	}

	@Test
	public void CDIExtensionSPI_WithServiceProperty() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.cdi.spi.b");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(3, cap.size());

			Props p = cap.get("osgi.cdi.extension");
			assertThat(p).isNotNull()
				.extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("version:Version")
				.isNotNull()
				.containsExactly("1.0.0");

			p = cap.get("osgi.service");
			assertThat(p).isNotNull()
				.extracting("objectClass:List<String>")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");

			p = cap.get("osgi.serviceloader");
			assertThat(p).isNotNull()
				.extracting("osgi.serviceloader")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");
			assertThat(p).extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("register:")
				.isNotNull()
				.containsExactly("test.annotationheaders.cdi.spi.b.WithServiceProperty");
			assertThat(p).extracting("foo:Integer")
				.isNotNull()
				.containsExactly("15");
			assertThat(p).extracting("uses:")
				.allMatch(o -> o == null);

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(3, req.size());

			p = req.get("osgi.extender");

			final Map<String, Object> map = new HashMap<>();
			map.put("osgi.extender", "osgi.serviceloader.registrar");
			map.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);

			p = req.get("osgi.implementation");

			final Map<String, Object> map2 = new HashMap<>();
			map2.put("osgi.implementation", "osgi.cdi");
			map2.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map2)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);
		}
	}

	@Test
	public void CDIExtensionSPI_Basic() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.cdi.spi.a");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(3, cap.size());

			Props p = cap.get("osgi.cdi.extension");
			assertThat(p).isNotNull()
				.extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("version:Version")
				.isNotNull()
				.containsExactly("1.0.0");

			p = cap.get("osgi.service");
			assertThat(p).isNotNull()
				.extracting("objectClass:List<String>")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");

			p = cap.get("osgi.serviceloader");
			assertThat(p).isNotNull()
				.extracting("osgi.serviceloader")
				.isNotNull()
				.containsExactly("javax.enterprise.inject.spi.Extension");
			assertThat(p).extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("register:")
				.isNotNull()
				.containsExactly("test.annotationheaders.cdi.spi.a.BasicExtension");
			assertThat(p).extracting("uses:")
				.allMatch(o -> o == null);

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(3, req.size());

			p = req.get("osgi.extender");

			final Map<String, Object> map = new HashMap<>();
			map.put("osgi.extender", "osgi.serviceloader.registrar");
			map.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);

			p = req.get("osgi.implementation");

			final Map<String, Object> map2 = new HashMap<>();
			map2.put("osgi.implementation", "osgi.cdi");
			map2.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map2)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);
		}
	}

	//////////////////

	@Test
	public void CDIExtension_ResolutionOptional() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.cdi.d");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, cap.size());

			Props p = cap.get("osgi.cdi.extension");
			assertThat(p).isNotNull()
				.extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("version:Version")
				.isNotNull()
				.containsExactly("1.0.0");

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(2, req.size());

			p = req.get("osgi.implementation");

			final Map<String, Object> map2 = new HashMap<>();
			map2.put("osgi.implementation", "osgi.cdi");
			map2.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map2)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.allMatch(o -> "optional".equals(o));
			assertThat(p).extracting("uses:")
				.allMatch(o -> o == null);
		}
	}

	@Test
	public void CDIExtension_EffectiveActive() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.cdi.c");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, cap.size());

			Props p = cap.get("osgi.cdi.extension");
			assertThat(p).isNotNull()
				.extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("version:Version")
				.isNotNull()
				.containsExactly("1.0.0");

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(2, req.size());

			p = req.get("osgi.implementation");

			final Map<String, Object> map2 = new HashMap<>();
			map2.put("osgi.implementation", "osgi.cdi");
			map2.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map2)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> "active".equals(o));
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);
		}
	}

	@Test
	public void CDIExtension_Basic() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.cdi.a");
			b.build();
			b.getJar()
				.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();

			Header cap = Header.parseHeader(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertEquals(1, cap.size());

			Props p = cap.get("osgi.cdi.extension");
			assertThat(p).isNotNull()
				.extracting("osgi.cdi.extension")
				.isNotNull()
				.containsExactly("foo.extension");
			assertThat(p).extracting("version:Version")
				.isNotNull()
				.containsExactly("1.0.0");

			Header req = Header.parseHeader(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			assertEquals(2, req.size());

			p = req.get("osgi.implementation");

			final Map<String, Object> map2 = new HashMap<>();
			map2.put("osgi.implementation", "osgi.cdi");
			map2.put("version", new Version(1, 0, 0));

			assertThat(p).isNotNull()
				.extracting("filter:")
				.isNotNull()
				.allMatch(asPredicate(fd -> new Filter((String) fd).matchMap(map2)));
			assertThat(p).extracting("cardinality:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("effective:")
				.allMatch(o -> o == null);
			assertThat(p).extracting("resolution:")
				.allMatch(o -> o == null);
		}
	}

}
