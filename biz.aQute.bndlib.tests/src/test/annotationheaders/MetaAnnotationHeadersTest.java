package test.annotationheaders;

import java.util.jar.Manifest;

import org.junit.Test;

import aQute.bnd.annotation.headers.BundleHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class MetaAnnotationHeadersTest extends TestCase {

	@BundleHeader(name = "Foo-Bundle-Activator")
	@interface FooActivatorHeader {}

	@FooActivatorHeader
	class A {}

	@BundleHeader(name = "Foo-Multi-Header")
	@interface MultiBundleHeader {}

	@MultiBundleHeader
	class B {}

	@MultiBundleHeader
	class C {}

	@BundleHeader(name = "Foo-Package-Header", value = "${@package}")
	@interface PackageHeader {}

	@PackageHeader
	class D {}

	@PackageHeader
	class E {}

	@Test
	public void testBasic() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("bin"));
			b.setPrivatePackage("test.annotationheaders");
			b.build();
			assertTrue(b.check());

			Manifest manifest = b.getJar()
				.getManifest();
			assertEquals(A.class.getName(), manifest.getMainAttributes()
				.getValue("Foo-Bundle-Activator"));
		}
	}

	@Test
	public void testMultiValueHeader() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("bin"));
			b.setPrivatePackage("test.annotationheaders");
			b.build();
			assertTrue(b.check());

			Manifest manifest = b.getJar()
				.getManifest();
			String value = manifest.getMainAttributes()
				.getValue("Foo-Multi-Header");
			Parameters params = new Parameters(value);
			assertTrue(params.containsKey(B.class.getName()));
			assertTrue(params.containsKey(C.class.getName()));
		}
	}

	@Test
	public void testMergeRepeated() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("bin"));
			b.setPrivatePackage("test.annotationheaders");
			b.build();
			assertTrue(b.check());

			Manifest manifest = b.getJar()
				.getManifest();
			assertEquals(D.class.getPackage()
				.getName(),
				manifest.getMainAttributes()
					.getValue("Foo-Package-Header"));
		}
	}

	@Test
	public void testSingleton() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("bin"));
			b.setPrivatePackage("test.annotationheaders.singleton");
			b.build();
			assertFalse("bundle should be invalid due to repeated use of singleton header annotation", b.check());
			assertTrue(b.getErrors()
				.get(0)
				.contains("Repeated use"));
		}
	}

}
