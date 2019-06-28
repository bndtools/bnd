package test.annotationheaders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.annotation.headers.BundleCategory;
import aQute.bnd.annotation.headers.BundleContributors;
import aQute.bnd.annotation.headers.BundleCopyright;
import aQute.bnd.annotation.headers.BundleDevelopers;
import aQute.bnd.annotation.headers.BundleDocURL;
import aQute.bnd.annotation.headers.Category;
import aQute.bnd.annotation.licenses.Apache_2_0;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("deprecation")
public class AnnotationHeadersTest extends TestCase {

	/**
	 * Say I want to define a capability namespace for web applications, e.g.
	 * Provide-Capability: webapp; webapp=Petstore. Web application components
	 * necessarily require an HTTP implementation so they should
	 * Require-Capability: osgi.implementation;
	 * filter:="(osgi.implementation=osgi.http)". I want to define an annotation
	 * that I can put onto a component that implies both the above provide and
	 * require. I tried the following:
	 *
	 * <pre>
	 * &#64;ProvideCapability(ns = "webapp")
	 * &#64;RequireCapability(ns = "osgi.implementation", filter = "(osgi.implementation=osgi.http)")
	 * &#64;interface WebApplication {
	 * 	String name();
	 * }
	 *
	 * &#64;WebApplication(name = "Petstore")
	 * &#64;Component
	 * public class PetstoreAppComponent {
	 * 	// ..
	 * }
	 * </pre>
	 *
	 * However this only generated the Provide, it did not generate the Require.
	 * If I switch the order of annotations so that @RequireCapability is first,
	 * then it only generates the Require.
	 */

	public void testMultipleManifestHeaders() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.multiple");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);
			Attributes mainAttributes = b.getJar()
				.getManifest()
				.getMainAttributes();
			Parameters req = new Parameters(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY));
			Parameters cap = new Parameters(mainAttributes.getValue(Constants.PROVIDE_CAPABILITY));
			assertTrue(cap.get("provide") != null);
			assertTrue(req.get("require") != null);
		}
	}

	/**
	 * Default values of annotation attributes not included for customized
	 * webresource annotations #976
	 * <p>
	 * When I add a customized webresource annotation to a type definition, the
	 * default attributes are not included in the Require-Capability header. For
	 * example with the Jsonrpc webresource annotation this leads to the
	 * following Require-Capability in the manifest:
	 * osgi.enroute.webresource;filter:=
	 * "(&(osgi.enroute.webresource=/osgi/enroute/jsonrpc)(&(version>=1.1.1)(!(version>=2.0.0))))".
	 * Only when I explicitly add resource={"jsonrpc.js"} this is reflected in
	 * the manifest: osgi.enroute.webresource;resource:List
	 * <String>="jsonrpc.js";filter:=
	 * "(&(osgi.enroute.webresource=/osgi/enroute/jsonrpc)(&(version>=1.1.1)(!(version>=2.0.0))))",
	 * although jsonrpc.js is set as default for the resource attribute.
	 */

	public void testDefaultAttrs() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.attrs.defaults");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);

			Domain d = Domain.domain(b.getJar()
				.getManifest());

			Parameters rc = d.getRequireCapability();
			assertNotNull(rc);
			assertEquals(2, rc.size());

			Attrs attrs = rc.get("default-attrs");
			assertEquals("42", attrs.get("foo"));

			Parameters pc = d.getProvideCapability();
			assertNotNull(pc);
			assertEquals(1, pc.size());

			attrs = pc.get("default-attrs");
			assertEquals("42", attrs.get("foo"));

		}
	}

	public void testWithAttrs() throws Exception {
		Builder b = new Builder();
		try {
			b.addClasspath(new File("bin_test"));
			b.setProperty("Private-Package", "test.annotationheaders.attrs");
			b.build();
			assertTrue(b.check());

			Manifest m = b.getJar()
				.getManifest();
			m.write(System.out);

			Parameters p = new Parameters(m.getMainAttributes()
				.getValue("Provide-Capability"));
			Attrs attrs = p.get("nsx");
			assertNotNull(attrs);
			assertEquals(Long.valueOf(3), attrs.getTyped("foo"));

			p = new Parameters(m.getMainAttributes()
				.getValue("Bundle-License"));
			attrs = p.get("license");
			assertNotNull(attrs);
			assertEquals("abc", attrs.get("foo"));

			p = new Parameters(m.getMainAttributes()
				.getValue("Require-Capability"));
			// namespaces must be "osgi.ee", "nsx" and "nsy" ONLY
			assertNotNull(p.get("nsx"));
			assertNotNull(p.get("nsy"));
			assertNotNull(p.get("nsz"));
			assertNotNull(p.get("param"));
			assertNotNull(p.get("osgi.ee"));
			assertEquals("spurious Require-Capability namespaces", 5, p.size());

			attrs = p.get("nsx");
			assertEquals(Arrays.asList("abc", "def"), attrs.getTyped("foo"));

			attrs = p.get("nsy");
			assertEquals("hello", attrs.get("value"));

			attrs = p.get("nsz");
			assertEquals("(nsz=*)", attrs.get("filter:"));
			assertEquals("world", attrs.get("hello"));

			attrs = p.get("param");
			assertEquals("(&(a=hello)(b=goodbye))", attrs.get("filter:"));
		} finally {
			b.close();
		}
	}

	@aQute.bnd.annotation.headers.RequireCapability(ns = "osgi.webresource", filter = "(&(osgi.webresource=/google/angular)${frange;${@version}})")
	@interface Angular {

	}

	@aQute.bnd.annotation.headers.RequireCapability(ns = "not.there", filter = "(a=3)")
	@interface Notused {

	}

	@BundleDevelopers("Peter.Kriens@aQute.biz;name='Peter Kriens';organization=aQute")
	@interface pkriens {}

	@BundleContributors(value = "Mieke.Kriens@aQute.biz", name = "Mieke Kriens", organization = "aQute")
	@interface mkriens {}

	@BundleContributors(value = "Thomas.Kriens@aQute.biz", name = "Thomas Kriens", organization = "aQute")
	@interface tkriens {}

	@BundleContributors(value = "Mischa.Kriens@aQute.biz", name = "Mischa Kriens", organization = "aQute")
	@interface mischakriens {}

	@aQute.bnd.annotation.headers.RequireCapability(ns = "abcdef", filter = "(&(abcdef=xyz)${frange;${@version}})")
	@Apache_2_0
	@pkriens
	@mkriens
	@tkriens
	class A {

	}

	@BundleDocURL("http://www.aQute.biz")
	@BundleCopyright("(c) ${tstamp;yyyy} aQute All Rights Reserved and other baloney")
	@pkriens
	@mkriens
	@tkriens
	@BundleCategory(Category.adoption)

	class B {

	}

	@BundleCopyright("[[\n\rXyz: Hello world. , ; = \\]]")
	class Z {

	}

	@BundleCopyright("v=${@version} p=${@package} c=${@class} s=${@class-short}")
	@Angular
	class C {

	}

	//
	// Check if we can
	interface X {
		@aQute.bnd.annotation.headers.RequireCapability(ns = "x", filter = "(x=xx)")
		@interface Require {}

		@aQute.bnd.annotation.headers.ProvideCapability(ns = "x", name = "xx")
		@interface Provide {}
	}

	@X.Provide
	class XImpl {}

	@aQute.bnd.annotation.headers.ProvideCapability(ns = "extrattrs", name = "extrattrs", value = "extra=YES")
	interface ExtraAttrs {

	}

	@aQute.bnd.annotation.headers.RequireCapability(ns = "nofilter", filter = "")
	@interface NoFilterRequirement {}

	@NoFilterRequirement
	class NoFilterImpl {}

	public void testBasic() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Private-Package", "test.annotationheaders");
		b.build();
		assertTrue(b.check());
		Manifest manifest = b.getJar()
			.getManifest();
		manifest.write(System.out);
		Parameters provideWithExtraAttrs = new Parameters(manifest.getMainAttributes()
			.getValue(Constants.PROVIDE_CAPABILITY));
		Attrs attrs = provideWithExtraAttrs.get("extrattrs");
		assertNotNull(attrs);
		assertEquals("extrattrs=extrattrs;extra=YES", attrs.toString());

		String rc = manifest.getMainAttributes()
			.getValue(Constants.REQUIRE_CAPABILITY);
		assertNotNull(rc);
		System.out.println(rc);
		assertTrue(rc.contains("osgi.webresource=/google"));
		assertTrue(rc.contains(">=1.2.3")); // from package info
		assertTrue(rc.contains(">=2.0.0")); // from package info

		assertFalse(rc.contains("xx"));

		assertTrue(rc.contains(",nofilter,"));

		String pc = manifest.getMainAttributes()
			.getValue(Constants.PROVIDE_CAPABILITY);
		assertNotNull(pc);
		System.out.println(pc);
		assertTrue(pc.contains("x=xx"));

		assertFalse(rc.contains("not.there"));

		String bl = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_LICENSE);
		assertNotNull(bl);
		System.out.println(bl);
		assertTrue(bl.contains("Apache-2.0"));
		assertTrue(bl.contains("MIT"));
		assertFalse(bl.contains("GPL"));

		String dv = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_DEVELOPERS);
		assertNotNull(dv);
		System.out.println(dv);
		assertTrue(dv.contains("Peter Kriens"));

		dv = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_CONTRIBUTORS);
		assertNotNull(dv);
		System.out.println(dv);
		assertTrue(dv.contains("Mieke Kriens"));
		assertTrue(dv.contains("Thomas Kriens"));
		assertFalse(dv.contains("Mischa Kriens"));

		dv = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_COPYRIGHT);
		assertNotNull(dv);
		System.out.println(dv);
		assertTrue(dv.contains("other baloney"));
		Matcher m = Pattern.compile("([0-9]{4})")
			.matcher(dv);
		assertTrue(m.find());
		assertTrue(Integer.parseInt(m.group(1)) >= 2014);
		assertTrue(dv.contains("v=1.2.3"));
		assertTrue(dv.contains("p=test.annotationheaders"));
		assertTrue(dv.contains("c=test.annotationheaders.AnnotationHeadersTest$C"));
		assertTrue(dv.contains("s=AnnotationHeadersTest$C"));

		dv = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_DOCURL);
		assertNotNull(dv);
		System.out.println(dv);
		assertTrue(dv.contains("http://www.aQute.biz"));

		Parameters cpr = new Parameters(manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_COPYRIGHT));
		for (Entry<String, Attrs> e : cpr.entrySet()) {
			System.out.println("cpr: " + e);
		}
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Jar.writeManifest(manifest, bout);
		String s = new String(bout.toByteArray(), "UTF-8");
		System.out.println(s);
		ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
		Manifest m2 = new Manifest(bin);
		String v = m2.getMainAttributes()
			.getValue(Constants.BUNDLE_COPYRIGHT);
		assertNotNull(v);
		assertTrue(v.contains("Hello world"));
		assertNull(m2.getMainAttributes()
			.getValue("Xyz"));
		b.close();
	}

}
