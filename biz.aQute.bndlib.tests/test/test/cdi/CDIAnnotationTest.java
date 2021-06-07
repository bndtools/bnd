package test.cdi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.Test;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;

public class CDIAnnotationTest {

	@Test
	public void allowAbstract() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Private-Package", "test.cdi.beans_l.*");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, Arrays.asList("test.cdi.beans_l.Bar"));
		}
	}

	@Test
	public void allowInterfaces() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Private-Package", "test.cdi.beans_k.*");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, Arrays.asList("test.cdi.beans_k.Bar"));
		}
	}

	@Test
	public void ignoresAnonymousClass() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Private-Package", "test.cdi.beans_j.*");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, Arrays.asList("test.cdi.beans_j.Bar"));
		}
	}

	@Test
	public void discoveryFromBeansXML() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Private-Package", "test.cdi.beans_i.*");
			b.setProperty("-includeresource", "META-INF/beans.xml=test/test/cdi/beans_i/beans.xml");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, Arrays.asList("test.cdi.beans_i.AppScopedBean"));
		}
	}

	@Test(expected = AssertionError.class)
	public void noRequiresNoSpecifiedBeans() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Private-Package", "test.cdi.beans_a.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, Arrays.asList("test.cdi.beans_a.AppScopedBean"));
		}
	}

	@Test
	public void requiresPackageSpecifiedBeans() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Private-Package", "test.cdi.beans_b.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, Arrays.asList("test.cdi.beans_b.AppScopedBean"));
		}
	}

	@Test
	public void requiresPackageWideBeans() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Private-Package", "test.cdi.beans_c.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, Arrays.asList("test.cdi.beans_c.AppScopedBean", "test.cdi.beans_c.SessionScopedBean"));
		}
	}

	@Test
	public void requiresSpecifiedBeans() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Private-Package", "test.cdi.beans_d.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, Arrays.asList("test.cdi.beans_d.AppScopedBean"));
		}
	}

	@Test
	public void discoveryAnnotated() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.CDIANNOTATIONS, "*;discover=annotated");
			b.setProperty("Private-Package", "test.cdi.beans_e.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a,
				Arrays.asList("test.cdi.beans_e.AppScopedBean", "test.cdi.beans_e.SessionScopedBean",
					"test.cdi.beans_e.ComponentA", "test.cdi.beans_e.DecoratorA", "test.cdi.beans_e.InterceptorA",
					"test.cdi.beans_e.DependentBean", "test.cdi.beans_e.ComponentScopedBean"));
		}
	}

	@Test(expected = AssertionError.class)
	public void discoveryNone() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.CDIANNOTATIONS, "*;discover=none");
			b.setProperty("Private-Package", "test.cdi.beans_e.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, Arrays.asList("test.cdi.beans_a.AppScopedBean"));
		}
	}

	@Test
	public void discoveryAll() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.CDIANNOTATIONS, "*;discover=all");
			b.setProperty("Private-Package", "test.cdi.beans_e.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a,
				Arrays.asList("test.cdi.beans_e.AppScopedBean", "test.cdi.beans_e.SessionScopedBean",
					"test.cdi.beans_e.ComponentA", "test.cdi.beans_e.DecoratorA", "test.cdi.beans_e.InterceptorA",
					"test.cdi.beans_e.DependentBean", "test.cdi.beans_e.ComponentScopedBean",
					"test.cdi.beans_e.ImpliedDependentBean"));
		}
	}

	@Test
	public void providesService() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Private-Package", "test.cdi.beans_f.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a, new String[] {
				"test.cdi.beans_f.AppScopedBean"
			}, new String[] {
				"test.cdi.beans_f.Foo"
			}, new String[] {
				"test.cdi.beans_f.Blah", "test.cdi.beans_f.Bar", "test.cdi.beans_f.Fee"
			});
			checkRequires(a, Arrays.asList("test.cdi.beans_f.AppScopedBean", "test.cdi.beans_f.ServiceB",
				"test.cdi.beans_f.ServiceC"));
		}
	}

	@Test
	public void providesServiceNoProvides() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.CDIANNOTATIONS, "*;noservicecapabilities=true");
			b.setProperty("Private-Package", "test.cdi.beans_f.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, Arrays.asList("test.cdi.beans_f.AppScopedBean", "test.cdi.beans_f.ServiceB",
				"test.cdi.beans_f.ServiceC"));
		}
	}

	@Test
	public void requiresService() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Private-Package", "test.cdi.beans_g.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a, new String[] {
				"test.cdi.beans_g.Gru"
			}, new String[] {
				"test.cdi.beans_g.Glum"
			});
			checkRequires(a,
				Arrays.asList("test.cdi.beans_g.AppScopedBean", "test.cdi.beans_g.ServiceB",
					"test.cdi.beans_g.ProducerA"),
				"test.cdi.beans_g.Foo", "java.lang.Character", "java.lang.Integer", "java.lang.Long",
				"java.lang.Boolean", "java.lang.Short", "test.cdi.beans_g.Bar", "test.cdi.beans_g.Baz",
				"test.cdi.beans_g.Bif", "test.cdi.beans_g.Fum", "test.cdi.beans_g.Glum", "test.cdi.beans_g.Fee");
		}
	}

	@Test
	public void requiresServiceNoRequires() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.CDIANNOTATIONS, "*;noservicerequirements=true");
			b.setProperty("Private-Package", "test.cdi.beans_g.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a, new String[] {
				"test.cdi.beans_g.Gru"
			}, new String[] {
				"test.cdi.beans_g.Glum"
			});
			checkRequires(a, Arrays.asList("test.cdi.beans_g.AppScopedBean", "test.cdi.beans_g.ServiceB",
				"test.cdi.beans_g.ProducerA"));
		}
	}

	@Test
	public void discoverTypo() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.CDIANNOTATIONS, "*;discover=foo");
			b.setProperty("Private-Package", "test.cdi.beans_g.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			Jar jar = b.build();

			if (!b.check()) {
				assertThat(b.getErrors()).asList()
					.containsExactly(
						"Unrecognized discover 'foo', expected values are [all, annotated, annotated_by_bean, none]");
			} else {
				fail("Was supposed to fail parsing discover value 'foo'");
			}
		}
	}

	@Test
	public void requiresServiceFromBinders() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Private-Package", "test.cdi.beans_h.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, Arrays.asList("test.cdi.beans_h.AppScopedBean"), "java.lang.Character",
				"java.lang.Integer", "java.lang.Long");
		}
	}

	@Test
	public void beansXmlInBCP() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Private-Package", "test.cdi.beans_h.*");
			b.setProperty("-fixupmessages", "While traversing the type tree for;is:=ignore");
			b.setProperty("-includeresource", "cxf-rt-rs-sse-*.jar;lib:=true,wicket-cdi-1.1-*.jar;lib:=true");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/cxf-rt-rs-sse-3.2.5.jar"));
			b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
			b.addClasspath(new File("jar/wicket-cdi-1.1-6.28.0.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, Arrays.asList("test.cdi.beans_h.AppScopedBean", "org.apache.wicket.cdi.AutoConversation"),
				"java.lang.Character", "java.lang.Integer", "java.lang.Long");
		}
	}

	@Test
	public void discoverEmptyXmlInInBCP() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Private-Package", "test.cdi.beans_h.*");
			b.setProperty("-fixupmessages", "While traversing the type tree for;is:=ignore");
			b.setProperty("-includeresource", "resteasy-cdi-*.jar;lib:=true");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(new File("jar/resteasy-cdi-4.0.0.Beta8.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, Arrays.asList("test.cdi.beans_h.AppScopedBean"), "java.lang.Character",
				"java.lang.Integer", "java.lang.Long");
		}
	}

	@Test
	public void beansXmlInWab() throws Exception {
		try (Builder b = new Builder(); Jar wab = new Jar(new File("jar/tck-V3URLTests.wab.war"))) {
			b.setJar(wab);
			b.setProperty(Constants.CDIANNOTATIONS, "*;discover=all");
			b.setProperty("-fixupmessages",
				"'No sub JAR or directory ext/WEB-INF/classes';is:=ignore,'While traversing the type tree for';is:=ignore");
			b.setProperty(Constants.BUNDLE_CLASSPATH, Domain.domain(wab.getManifest())
				.getBundleClassPath()
				.toString());
			Jar jar = b.build();

			if (!b.check())
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a,
				Arrays.asList("javax.portlet.tck.portlets.URLTests_ActionURL",
					"javax.portlet.tck.portlets.URLTests_BaseURL", "javax.portlet.tck.portlets.URLTests_RenderURL",
					"javax.portlet.tck.portlets.URLTests_ResourceURL", "javax.portlet.tck.util.ModuleTestCaseDetails"));
		}
	}

	private void checkProvides(Attributes a, String[]... objectClass) {
		String p = a.getValue(Constants.PROVIDE_CAPABILITY);
		System.err.println(Constants.PROVIDE_CAPABILITY + ":" + p);
		Parameters header = new Parameters(p);
		List<Attrs> attrs = getAll(header, "osgi.service");
		assertEquals(objectClass.length, attrs.size());
		for (String[] o : objectClass) {
			List<String> os = Arrays.asList(o);
			boolean found = false;
			for (Attrs at : attrs) {
				@SuppressWarnings("unchecked")
				List<String> oc = (List<String>) at.getTyped("objectClass");
				if (oc.stream()
					.allMatch(e -> os.contains(e))) {
					found = true;
				}
			}
			assertTrue("objectClass not found: " + os, found);
		}
	}

	private void checkRequires(Attributes a, List<String> beans, String... objectClass) {
		String p = a.getValue(Constants.REQUIRE_CAPABILITY);
		System.err.println(Constants.REQUIRE_CAPABILITY + ":" + p);
		Parameters header = new Parameters(p);
		List<Attrs> attrs = getAll(header, "osgi.service");
		assertEquals("osgi.service attributes: " + attrs, objectClass.length, attrs.size());
		for (String o : objectClass) {
			boolean found = false;
			for (Attrs at : attrs) {
				if (("(objectClass=" + o + ")").equals(at.get("filter:"))) {
					assertEquals("no effective:=\"active\"", "active", at.get("effective:"));
					found = true;
				}
			}
			assertTrue("objectClass not found: " + o, found);
		}

		if (beans != null) {
			Attrs attr = header.get("osgi.extender");
			assertNotNull(attr);
			assertEquals("(&(osgi.extender=osgi.cdi)(version>=1.0.0)(!(version>=2.0.0)))", attr.get("filter:"));
			assertThat(attr.getTyped("beans")).isInstanceOf(List.class)
				.asList()
				.containsAll(beans)
				.hasSize(beans.size());
		}
	}

	private Attributes getAttr(Jar jar) throws Exception {
		Manifest m = jar.getManifest();
		return m.getMainAttributes();
	}

	private List<Attrs> getAll(Parameters p, String key) {
		List<Attrs> l = new ArrayList<>();
		for (; p.containsKey(key); key += aQute.bnd.osgi.Constants.DUPLICATE_MARKER) {
			l.add(p.get(key));
		}
		return l;
	}

}
