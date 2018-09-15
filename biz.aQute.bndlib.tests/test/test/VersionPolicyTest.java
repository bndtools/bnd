package test;

import java.io.File;
import java.util.Map;
import java.util.jar.Manifest;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class VersionPolicyTest extends TestCase {

	/**
	 * Test disable default package versions.
	 */
	public static void testDisableDefaultPackageVersion() throws Exception {
		Builder a = new Builder();
		a.addClasspath(new File("bin_test"));
		a.setProperty("Bundle-Version", "1.2.3");
		a.setProperty("Export-Package", "test.refer");
		a.setProperty("-nodefaultversion", "true");
		Jar jar = a.build();

		Manifest m = jar.getManifest();
		Parameters exports = Processor.parseHeader(m.getMainAttributes()
			.getValue(Constants.EXPORT_PACKAGE), null);
		Map<String, String> attrs = exports.get("test.refer");
		assertNotNull(attrs);
		assertNull(attrs.get("version"));
	}

	/**
	 * Test default package versions.
	 */
	public static void testDefaultPackageVersion() throws Exception {
		Builder a = new Builder();
		a.addClasspath(new File("bin_test"));
		a.setProperty("Bundle-Version", "1.2.3");
		a.setProperty("Export-Package", "test.refer");
		Jar jar = a.build();

		Manifest m = jar.getManifest();
		Parameters exports = Processor.parseHeader(m.getMainAttributes()
			.getValue(Constants.EXPORT_PACKAGE), null);
		Map<String, String> attrs = exports.get("test.refer");
		assertNotNull(attrs);
		assertEquals("1.2.3", attrs.get("version"));
	}

	/**
	 * Test import provide:.
	 */
	public static void testImportProvided() throws Exception {
		Builder a = new Builder();
		a.addClasspath(IO.getFile("jar/osgi.jar"));
		a.addClasspath(new File("bin_test"));
		a.setProperty("Private-Package", "test.refer");
		a.setProperty("Import-Package", "org.osgi.service.event;provide:=true,*");
		Jar jar = a.build();
		Map<String, String> event = a.getImports()
			.getByFQN("org.osgi.service.event");
		assertEquals("[1.0,1.1)", event.get("version"));
		Map<String, String> http = a.getImports()
			.getByFQN("org.osgi.service.http");
		assertEquals("[1.2,2)", http.get("version"));

		Manifest m = jar.getManifest();
		String imports = m.getMainAttributes()
			.getValue(Constants.IMPORT_PACKAGE);
		assertFalse(imports.contains(Constants.PROVIDE_DIRECTIVE));
	}

	/**
	 * Test import provide:.
	 */
	public static void testExportProvided() throws Exception {
		Builder a = new Builder();
		a.addClasspath(IO.getFile("jar/osgi.jar"));
		a.addClasspath(new File("bin_test"));
		a.setProperty("Private-Package", "test.refer");
		a.setProperty("Export-Package", "org.osgi.service.http;provide:=true");
		Jar jar = a.build();
		Map<String, String> event = a.getImports()
			.getByFQN("org.osgi.service.event");
		assertEquals("[1.0,2)", event.get("version"));
		Map<String, String> http = a.getImports()
			.getByFQN("org.osgi.service.http");
		assertEquals("[1.2,1.3)", http.get("version"));

		Manifest m = jar.getManifest();
		String imports = m.getMainAttributes()
			.getValue(Constants.IMPORT_PACKAGE);
		assertFalse(imports.contains(Constants.PROVIDE_DIRECTIVE));
	}

	/**
	 * Test export annotation.
	 */
	public static void testExportAnnotation() throws Exception {
		Builder a = new Builder();
		a.addClasspath(new File("bin_test"));
		a.setProperty("build", "xyz");
		a.setProperty("Export-Package", "test.versionpolicy.api");
		a.build();
		Map<String, String> attrs = a.getExports()
			.getByFQN("test.versionpolicy.api");
		assertEquals("1.2.0.xyz", attrs.get("version"));
		assertEquals("PrivateImpl", attrs.get("exclude:"));
		assertEquals("a", attrs.get("mandatory:"));
	}

	/**
	 * Tests if the implementation of the EventAdmin (which is marked as a
	 * ProviderType) causes the import of the api package to use the provider
	 * version policy.
	 */
	public static void testProviderType() throws Exception {
		Builder a = new Builder();
		a.addClasspath(new File("bin_test"));
		a.setPrivatePackage("test.versionpolicy.implemented");
		a.setExportPackage("test.versionpolicy.api");
		a.setImportPackage("test.versionpolicy.api"); // what changed so this is
														// not automatically
														// added?
		a.setProperty("build", "123");
		a.setProperty("-fixupmessages",
			"The annotation aQute.bnd.annotation.Export applied to package test.versionpolicy.api is deprecated and will be removed in a future release. The org.osgi.annotation.bundle.Export should be used instead");
		Jar jar = a.build();
		assertTrue(a.check());
		Manifest m = jar.getManifest();
		m.write(System.err);
		Domain d = Domain.domain(m);

		Parameters parameters = d.getImportPackage();
		Attrs attrs = parameters.get("test.versionpolicy.api");
		assertNotNull(attrs);
		assertEquals("[1.2,1.3)", attrs.get("version"));

	}

	/**
	 * Test if the implementation of "AnnotatedProviderInterface", which is
	 * annotated with OSGi R6 @ProviderType, causes import of the api package to
	 * use the provider version policy
	 */
	public static void testProviderTypeR6() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setPrivatePackage("test.versionpolicy.implemented.osgi");
		b.setProperty("build", "123");

		Jar jar = b.build();
		assertTrue(b.check());
		Manifest m = jar.getManifest();
		m.write(System.err);

		Domain d = Domain.domain(m);
		Parameters params = d.getImportPackage();
		Attrs attrs = params.get("test.version.annotations.osgi");
		assertNotNull(attrs);
		assertEquals("[1.2,1.3)", attrs.get("version"));
	}

	/**
	 * Tests if the implementation of the EventHandler (which is marked as a
	 * ConsumerType) causes the import of the api package to use the consumer
	 * version policy.
	 */
	public static void testConsumerType() throws Exception {
		Builder a = new Builder();
		a.addClasspath(new File("bin_test"));
		a.setPrivatePackage("test.versionpolicy.uses");
		a.setExportPackage("test.versionpolicy.api");
		a.setProperty("build", "123");
		a.setProperty("-fixupmessages",
			"The annotation aQute.bnd.annotation.Export applied to package test.versionpolicy.api is deprecated and will be removed in a future release. The org.osgi.annotation.bundle.Export should be used instead");
		Jar jar = a.build();
		assertTrue(a.check());
		Manifest m = jar.getManifest();
		m.write(System.err);
		Domain d = Domain.domain(m);

		Parameters parameters = d.getImportPackage();
		Attrs attrs = parameters.get("test.versionpolicy.api");
		assertNotNull(attrs);
		assertEquals("[1.2,2)", attrs.get("version"));

	}

	/**
	 * Check implementation version policy. Uses the package
	 * test.versionpolicy.(uses|implemented)
	 */
	static void assertPolicy(String pack, String type) throws Exception {
		Builder a = new Builder();
		a.addClasspath(new File("bin_test"));
		a.setProperty("Export-Package", "test.versionpolicy.api");
		Jar jar = a.build();

		Builder b = new Builder();
		b.addClasspath(jar);
		b.addClasspath(new File("bin_test"));

		b.setProperty("-versionpolicy-impl", "IMPL");
		b.setProperty("-versionpolicy-uses", "USES");
		b.setProperty("Private-Package", pack);
		b.build();
		Manifest m = b.getJar()
			.getManifest();
		m.write(System.err);
		Map<String, String> map = b.getImports()
			.getByFQN("test.versionpolicy.api");
		assertNotNull(map);
		// String s = map.get(Constants.IMPLEMENTED_DIRECTIVE);
		// assertEquals("true", s);
		Parameters mp = Processor.parseHeader(m.getMainAttributes()
			.getValue("Import-Package"), null);
		assertEquals(type, mp.get("test.versionpolicy.api")
			.get("version"));
	}

	/**
	 * hardcoded imports
	 */
	public static void testHardcodedImports() throws Exception {
		Builder b = new Builder();
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.setProperty("-versionpolicy", "${range;[==,+)}");
		b.setProperty("Private-Package", "org.objectweb.asm");
		b.setProperty("Import-Package", "org.osgi.framework,org.objectweb.asm,abc;version=2.0.0,*");
		b.build();
		Manifest m = b.getJar()
			.getManifest();
		m.write(System.err);
		String s = b.getImports()
			.getByFQN("org.objectweb.asm")
			.get("version");
		assertNull(s);
		s = b.getImports()
			.getByFQN("abc")
			.get("version");
		assertEquals("2.0.0", s);

		s = b.getImports()
			.getByFQN("org.osgi.framework")
			.get("version");
		assertEquals("[1.3,2)", s);

	}

	/**
	 * Specify the version on the export and verify that the policy is applied
	 * on the matching import.
	 */
	public static void testExportsSpecifiesVersion() throws Exception {
		Builder b = new Builder();
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "org.osgi.service.event");
		b.setProperty("Private-Package", "test.refer");
		b.build();
		String s = b.getImports()
			.getByFQN("org.osgi.service.event")
			.get("version");
		assertEquals("[1.0,2)", s);

	}

	/**
	 * See if we a can override the version from the export statement and the
	 * version from the source.
	 */
	public static void testImportOverridesDiscoveredVersion() throws Exception {
		Builder b = new Builder();
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "org.osgi.service.event");
		b.setProperty("Private-Package", "test.refer");
		b.setProperty("Import-Package", "org.osgi.service.event;version=2.1.3.q");
		b.build();
		String s = b.getImports()
			.getByFQN("org.osgi.service.event")
			.get("version");
		assertEquals("2.1.3.q", s);
	}

	/**
	 * Test if we can get the version from the source and apply the default
	 * policy.
	 */
	public static void testVersionPolicyImportedExportsDefaultPolicy() throws Exception {
		Builder b = new Builder();
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "org.osgi.service.event");
		b.setProperty("Private-Package", "test.refer");
		b.build();
		String s = b.getImports()
			.getByFQN("org.osgi.service.event")
			.get("version");
		assertEquals("[1.0,2)", s);
	}

	/**
	 * The default policy is truncate micro. Check if this is applied to the
	 * import.
	 */
	public static void testImportMicroTruncated() throws Exception {
		Builder b = new Builder();
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.setProperty("Import-Package", "org.osgi.service.event");
		b.build();
		String s = b.getImports()
			.getByFQN("org.osgi.service.event")
			.get("version");
		assertEquals("[1.0,2)", s);
	}

	/**
	 * Check if we can set a specific version on the import that does not use a
	 * version policy.
	 */
	public static void testImportMicroNotTruncated() throws Exception {
		Builder b = new Builder();
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.setProperty("Import-Package",
			"org.osgi.service.event;version=${@}, org.osgi.service.log;version=\"${range;[==,=+)}\"");
		b.build();
		String s = b.getImports()
			.getByFQN("org.osgi.service.event")
			.get("version");
		String l = b.getImports()
			.getByFQN("org.osgi.service.log")
			.get("version");
		assertEquals("1.0.1", s);
		assertEquals("[1.3,1.4)", l);
	}

}
