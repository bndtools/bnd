package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import junit.framework.TestCase;

class T0 {}

abstract class T1 extends T0 {}

class T2 extends T1 {}

class T3 extends T2 {}

@SuppressWarnings("resource")
public class AnalyzerTest extends TestCase {
	static File cwd = new File(System.getProperty("user.dir"));

	/**
	 * #1352 support globbing during includeresource's buildpath reference
	 * resolution
	 *
	 * @throws Exception
	 */

	public void testIncludeResourceFromClasspathWithGlobbing() throws Exception {
		Builder b = new Builder();
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.addClasspath(IO.getFile("jar/asm.jar"));
		b.addClasspath(IO.getFile("jar/bcel.jar"));
		b.setProperty("-includeresource", "@o*i.jar, a*m.jar");
		Jar jar = b.build();
		assertTrue(b.check());
		assertNotNull(jar.getResource("LICENSE"));
		assertNotNull(jar.getResource("asm.jar"));
	}

	public void testIncludeResourceFromClasspathWithGlobbingMultiple() throws Exception {
		Builder b = new Builder();
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.addClasspath(IO.getFile("jar/asm.jar"));
		b.addClasspath(IO.getFile("jar/bcel.jar"));
		List<Jar> jars = b.getJarsFromName("*.jar", "");
		assertEquals(3, jars.size());
	}

	/**
	 * #525 Test if exceptions are imported
	 */

	public void testExceptionImports() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.setExportPackage("test.exceptionimport");
		b.build();
		assertTrue(b.check());

		assertNotNull(b.getImports()
			.containsFQN("org.osgi.framework"));

	}

	/**
	 * Verify that the OSGi and the bnd Version annotation both work
	 */

	public void testVersionAnnotation() throws Exception {
		Builder b = new Builder();
		try {
			b.addClasspath(new File("bin_test"));
			b.setExportPackage("test.version.annotations.*");
			b.build();
			assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);
			Attrs bnd = b.getExports()
				.getByFQN("test.version.annotations.bnd");
			Attrs osgi = b.getExports()
				.getByFQN("test.version.annotations.osgi");
			assertEquals("1.2.3.bnd", bnd.getVersion());
			assertEquals("1.2.3.osgi", osgi.getVersion());
		} finally {
			b.close();
		}
	}

	/**
	 * For the following annotation class in an OSGi
	 * bundle @Retention(RetentionPolicy.RUNTIME) @Target({ ElementType.TYPE,
	 * ElementType.METHOD }) public @interface Transactional { @Nonbinding
	 * Class<? extends Annotation>[] qualifier() default Any.class; }
	 * maven-bundle-plugin fails to generate the package import for
	 * javax.enterprise.inject.Any, the default value of the annotation method.
	 * At runtime, this leads to a non-descriptive exception
	 *
	 * <pre>
	 *  Caused by:
	 * java.lang.ArrayStoreException:
	 * sun.reflect.annotation.TypeNotPresentExceptionProxy at
	 * sun.reflect.annotation.AnnotationParser.parseClassArray(AnnotationParser.
	 * java:673) ~[na:1.7.0_04] at
	 * sun.reflect.annotation.AnnotationParser.parseArray(AnnotationParser.java:
	 * 480) ~[na:1.7.0_04] at
	 * sun.reflect.annotation.AnnotationParser.parseMemberValue(AnnotationParser
	 * .java:306) ~[na:1.7.0_04] at
	 * java.lang.reflect.Method.getDefaultValue(Method.java:726) ~[na:1.7.0_04]
	 * at sun.reflect.annotation.AnnotationType.<init>(AnnotationType.java:117)
	 * ~[na:1.7.0_04] at
	 * sun.reflect.annotation.AnnotationType.getInstance(AnnotationType.java:84)
	 * ~[na:1.7.0_04] at
	 * sun.reflect.annotation.AnnotationParser.parseAnnotation(AnnotationParser.
	 * java:221) ~[na:1.7.0_04] at
	 * sun.reflect.annotation.AnnotationParser.parseAnnotations2(
	 * AnnotationParser.java:88) ~[na:1.7.0_04] at
	 * sun.reflect.annotation.AnnotationParser.parseAnnotations(AnnotationParser
	 * .java:70) ~[na:1.7.0_04] at
	 * java.lang.Class.initAnnotationsIfNecessary(Class.java:3089)
	 * ~[na:1.7.0_04] at java.lang.Class.getDeclaredAnnotations(Class.java:3077)
	 * ~[na:1.7.0_04]
	 * </pre>
	 */
	public void testAnnotationWithDefaultClass() throws Exception {
		Builder b = new Builder();
		try {
			b.addClasspath(IO.getFile(cwd, "bin_test"));
			b.setExportPackage("test.annotation");
			b.build();
			assertTrue(b.check());
			assertTrue(b.getExports()
				.containsKey(b.getPackageRef("test/annotation")));
			assertFalse(b.getContained()
				.containsKey(b.getPackageRef("test/annotation/any")));
			assertTrue(b.getImports()
				.containsKey(b.getPackageRef("test/annotation/any")));
		} finally {
			b.close();
		}
	}

	/**
	 * Check the cross references
	 */

	/**
	 * The -removeheaders header can be used as a whitelist.
	 */

	public void testRemoveheadersAsWhiteList() throws Exception {
		Builder b = new Builder();
		try {
			b.addClasspath(IO.getFile("jar/asm.jar"));
			b.setExportPackage("*");
			b.setImportPackage("something");
			b.setProperty("Foo", "Foo");
			b.setProperty("Bar", "Bar");
			b.setProperty(Constants.REMOVEHEADERS, "!Bundle-*,!*-Package,!Service-Component,*");
			b.build();
			assertTrue(b.check());
			Manifest m = b.getJar()
				.getManifest();

			assertNotNull(m.getMainAttributes()
				.getValue(Constants.BUNDLE_MANIFESTVERSION));
			assertNotNull(m.getMainAttributes()
				.getValue(Constants.BUNDLE_NAME));
			assertNotNull(m.getMainAttributes()
				.getValue(Constants.BUNDLE_SYMBOLICNAME));
			assertNotNull(m.getMainAttributes()
				.getValue(Constants.IMPORT_PACKAGE));
			assertNotNull(m.getMainAttributes()
				.getValue(Constants.EXPORT_PACKAGE));
			assertNull(m.getMainAttributes()
				.getValue("Foo"));
			assertNull(m.getMainAttributes()
				.getValue("Bar"));
		} finally {
			b.close();
		}
	}

	/**
	 * Check if bnd detects references to private packages and gives a warning.
	 */

	public void testExportReferencesToPrivatePackages() throws Exception {
		Builder b = new Builder();
		try {
			b.addClasspath(IO.getFile("jar/osgi.jar"));
			b.addClasspath(new File("bin_test"));
			b.setExportPackage("test.referApi"); // refers to Event Admin
			b.setConditionalPackage("org.osgi.service.*");
			Jar jar = b.build();
			assertTrue(b.check(
				"((, )?(org.osgi.service.event|org.osgi.service.component|org.osgi.service.http|org.osgi.service.log|org.osgi.service.condpermadmin|org.osgi.service.wireadmin|org.osgi.service.device)){7,7}"));
		} finally {
			b.close();
		}
	}

	/**
	 * Test basic functionality of he BCP
	 */

	public void testBundleClasspath() throws Exception {
		Builder b = new Builder();
		try {
			b.setProperty(Constants.BUNDLE_CLASSPATH, "foo");
			b.setProperty(Constants.INCLUDE_RESOURCE, "foo/test/refer=bin_test/test/refer");
			b.setProperty(Constants.EXPORT_CONTENTS, "test.refer");
			Jar jar = b.build();
			Manifest m = jar.getManifest();
			assertTrue(b.check());
			m.write(System.err);
		} finally {
			b.close();
		}

	}

	public void testBundleClasspathWithVersionedExports() throws Exception {
		Builder b = new Builder();
		try {
			b.setBundleClasspath("foo");
			b.setIncludeResource("foo/test/refer_versioned=bin_test/test/refer_versioned");
			b.setProperty(Constants.EXPORT_CONTENTS, "${packages;ANNOTATED;org.osgi.annotation.versioning.Version}");
			Jar jar = b.build();
			Manifest m = jar.getManifest();
			assertTrue(b.check());
			Packages exports = b.getExports();
			String version = exports.getByFQN("test.refer_versioned")
				.getVersion();
			assertEquals("3.5.7", version);
			m.write(System.err);
		} finally {
			b.close();
		}

	}

	public void testNegatedPackagesFiltering() throws Exception {
		Builder b = new Builder();
		try {
			b.setIncludePackage("test.packageinfo.*");
			b.setProperty("X-Not-Versioned", "${packages;ANNOTATED;!org.osgi.annotation.versioning.Version}");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();
			Manifest m = jar.getManifest();
			m.write(System.err);
			assertTrue(b.check());
			Parameters notVersioned = Domain.domain(m)
				.getParameters("X-Not-Versioned");
			assertThat(notVersioned) //
				.containsKeys("test.packageinfo", "test.packageinfo.both_no_version",
				"test.packageinfo.nopackageinfo", "test.packageinfo.ref")
				.doesNotContainKeys("test.packageinfo.annotated", "test.packageinfo.notannotated");
		} finally {
			b.close();
		}

	}

	/**
	 * Very basic sanity test
	 */

	public void testSanity() throws Exception {
		Builder b = new Builder();
		try {
			b.setProperty("Export-Package", "thinlet;version=1.0");
			b.addClasspath(IO.getFile("jar/thinlet.jar"));
			b.build();
			assertTrue(b.check());
			assertEquals("1.0", b.getExports()
				.getByFQN("thinlet")
				.getVersion());
			assertTrue(b.getJar()
				.getDirectories()
				.containsKey("thinlet"));
			assertTrue(b.getJar()
				.getResources()
				.containsKey("thinlet/Thinlet.class"));
		} finally {
			b.close();
		}
	}

	/**
	 * Fastest way to create a manifest
	 *
	 * @throws Exception
	 */

	public void testGenerateManifest() throws Exception {
		Analyzer analyzer = new Analyzer();
		try {
			Jar bin = new Jar(IO.getFile("jar/osgi.jar"));
			bin.setManifest(new Manifest());
			analyzer.setJar(bin);
			analyzer.addClasspath(IO.getFile("jar/spring.jar"));
			analyzer.setProperty("Bundle-SymbolicName", "org.osgi.core");
			analyzer.setProperty("Export-Package", "org.osgi.framework,org.osgi.service.event");
			analyzer.setProperty("Bundle-Version", "1.0.0.x");
			analyzer.setProperty("Import-Package", "*");
			Manifest manifest = analyzer.calcManifest();
			assertTrue(analyzer.check());
			manifest.write(System.err);
			Domain main = Domain.domain(manifest);
			Parameters export = main.getExportPackage();
			Parameters expected = new Parameters(
				"org.osgi.framework;version=\"1.3\",org.osgi.service.event;uses:=\"org.osgi.framework\";version=\"1.0.1\"");
			assertTrue(expected.isEqual(export));
			assertEquals("1.0.0.x", manifest.getMainAttributes()
				.getValue("Bundle-Version"));
		} finally {
			analyzer.close();
		}

	}

	/**
	 * Make sure packages from embedded directories referenced from
	 * Bundle-Classpath are considered during import/export calculation.
	 */

	public void testExportContentsDirectory() throws Exception {
		Builder b = new Builder();
		try {
			File embedded = IO.getFile("bin_test/test/refer")
				.getCanonicalFile();
			assertTrue(embedded.isDirectory()); // sanity check
			b.setProperty("Bundle-ClassPath", ".,jars/some.jar");
			b.setProperty("-includeresource", "jars/some.jar/test/refer=" + embedded.getAbsolutePath());
			b.setProperty("-exportcontents", "test.refer");
			b.build();
			assertTrue(b.check("Bundle-ClassPath uses a directory 'jars/some.jar'"));
			assertTrue(b.getImports()
				.toString(),
				b.getImports()
					.getByFQN("org.osgi.service.event") != null);
		} finally {
			b.close();
		}
	}

	/**
	 * Uses constraints must be filtered by imports or exports.
	 *
	 * @throws Exception
	 */

	public void testUsesFiltering() throws Exception {
		Builder b = new Builder();
		try {
			b.setTrace(true);
			b.addClasspath(IO.getFile("jar/osgi.jar"));
			b.setProperty("Export-Package", "org.osgi.service.event");
			Jar jar = b.build();
			assertTrue(b.check());

			assertNotNull(jar.getResource("org/osgi/service/event/EventAdmin.class"));

			String exports = jar.getManifest()
				.getMainAttributes()
				.getValue("Export-Package");
			System.err.println(exports);
			assertTrue(exports.contains("uses:=\"org.osgi.framework\""));

			b = new Builder();
			b.addClasspath(IO.getFile("jar/osgi.jar"));
			b.setProperty("Import-Package", "");
			b.setProperty("Export-Package", "org.osgi.service.event");
			b.setPedantic(true);
			jar = b.build();
			exports = jar.getManifest()
				.getMainAttributes()
				.getValue("Export-Package");
			System.err.println(exports);
			assertTrue(b.check("Empty Import-Package header"));
			exports = jar.getManifest()
				.getMainAttributes()
				.getValue("Export-Package");
			assertFalse(exports.contains("uses:=\"org.osgi.framework\""));
		} finally {
			b.close();
		}

	}

	/**
	 * Test if require works
	 *
	 * @throws Exception
	 */

	public void testRequireFail() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("jar/osgi.jar"));
			b.setProperty("Private-Package", "org.osgi.framework");
			b.setProperty("-require-bnd", "\"(version=10000)\"");
			b.build();
			assertTrue(b.check("-require-bnd fails for filter \\(version=10000\\) values=\\{version=.*\\}"));
		}
	}

	public void testRequirePass() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("jar/osgi.jar"));
			b.setProperty("Private-Package", "org.osgi.framework");
			b.setProperty("-require-bnd", "\"(version>=" + About.CURRENT + ")\"");
			b.build();
			assertTrue(b.check());
		}
	}

	public void testComponentImportReference() throws Exception {
		Builder b = new Builder();
		try {
			b.addClasspath(IO.getFile("jar/osgi.jar"));
			b.setProperty("Private-Package", "org.osgi.framework");
			b.setProperty("Import-Package", "not.here,*");
			b.setProperty("Service-Component", "org.osgi.framework.Bundle;ref=not.here.Reference");
			b.build();
			System.err.println(b.getErrors());
			System.err.println(b.getWarnings());
			assertEquals(0, b.getErrors()
				.size());
			assertEquals(0, b.getWarnings()
				.size());
		} finally {
			b.close();
		}
	}

	public void testFindClass() throws Exception {
		Builder a = new Builder();
		try {
			a.setProperty("Export-Package", "org.osgi.service.io");
			a.addClasspath(IO.getFile("jar/osgi.jar"));
			a.build();
			System.err.println(a.getErrors());
			System.err.println(a.getWarnings());

			Collection<Clazz> c = a.getClasses("", "IMPORTS", "javax.microedition.io");
			System.err.println(c);
		} finally {
			a.close();
		}
	}

	public void testMultilevelInheritance() throws Exception {
		try (Analyzer a = new Analyzer()) {
			a.setJar(new File("bin_test"));
			a.analyze();

			String result = a._classes("cmd", "named", "*T?", "extends", "test.T0", "concrete");
			System.err.println(result);
			assertTrue(result.contains("test.T2"));
			assertTrue(result.contains("test.T3"));
		}

	}

	public void testClassQuery() throws Exception {
		try (Analyzer a = new Analyzer()) {
			a.setJar(IO.getFile("jar/osgi.jar"));
			a.analyze();

			String result = a._classes("cmd", "named", "org.osgi.service.http.*", "abstract");
			TreeSet<String> r = new TreeSet<>(Processor.split(result));
			assertEquals(
				new TreeSet<>(Arrays.asList("org.osgi.service.http.HttpContext", "org.osgi.service.http.HttpService")),
				r);
		}
	}

	public void testClassQuery_b() throws Exception {
		try (Analyzer a = new Analyzer()) {
			a.setJar(IO.getFile("jar/osgi.jar"));
			a.analyze();

			String result = a._classes("cmd", "named", "org.osgi.service.cu.*", "named", "!org.osgi.service.cu.admin.*",
				"named", "!org.osgi.service.cu.diag.*");
			TreeSet<String> r = new TreeSet<>(Processor.split(result));
			assertEquals(new TreeSet<>(
				Arrays.asList("org.osgi.service.cu.ControlUnit", "org.osgi.service.cu.ControlUnitConstants",
					"org.osgi.service.cu.ControlUnitException", "org.osgi.service.cu.StateVariableListener")),
				r);
		}
	}

	/**
	 * Use a private activator, check it is not imported.
	 *
	 * @throws Exception
	 */
	public void testEmptyHeader() throws Exception {
		try (Builder a = new Builder()) {
			a.setProperty("Bundle-Blueprint", "  <<EMPTY>> ");
			a.setProperty("Export-Package", "org.osgi.framework");
			a.addClasspath(IO.getFile("jar/osgi.jar"));
			a.build();
			Manifest manifest = a.getJar()
				.getManifest();
			System.err.println(a.getErrors());
			System.err.println(a.getWarnings());
			assertEquals(0, a.getErrors()
				.size());
			assertEquals(0, a.getWarnings()
				.size());
			String bb = manifest.getMainAttributes()
				.getValue("Bundle-Blueprint");
			System.err.println(bb);
			assertNotNull(bb);
			assertEquals("", bb);
		}
	}

	/**
	 * Test name section.
	 */

	public void testNameSection() throws Exception {
		Builder a = new Builder();
		try {
			a.setProperty("Export-Package", "org.osgi.service.event, org.osgi.service.io");
			a.addClasspath(IO.getFile("jar/osgi.jar"));
			a.setProperty("@org@osgi@service@event@Specification-Title", "spec title");
			a.setProperty("@org@osgi@service@io@Specification-Title", "spec title io");
			a.setProperty("@org@osgi@service@event@Specification-Version", "1.1");
			a.setProperty("@org@osgi@service@event@Implementation-Version", "5.1");
			Jar jar = a.build();
			Manifest m = jar.getManifest();
			Attributes attrs = m.getAttributes("org/osgi/service/event");
			assertNotNull(attrs);
			assertEquals("5.1", attrs.getValue("Implementation-Version"));
			assertEquals("1.1", attrs.getValue("Specification-Version"));
			assertEquals("spec title", attrs.getValue("Specification-Title"));

			attrs = m.getAttributes("org/osgi/service/io");
			assertNotNull(attrs);
			assertEquals("spec title io", attrs.getValue("Specification-Title"));
		} finally {
			a.close();
		}

	}

	/**
	 * Check if calcManifest sets the version
	 */

	/**
	 * Test if mandatory attributes are augmented even when the version is not
	 * set.
	 */
	public void testMandatoryWithoutVersion() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Import-Package", "*");
			p.put("Private-Package", "org.apache.mina.management.*");
			a.setClasspath(new Jar[] {
				new Jar(IO.getFile("jar/mandatorynoversion.jar"))
			});
			a.setProperties(p);
			Jar jar = a.build();
			assertTrue(a.check());

			String imports = jar.getManifest()
				.getMainAttributes()
				.getValue("Import-Package");
			System.err.println(imports);
			assertTrue(imports.contains("x=1"));
			assertTrue(imports.contains("y=2"));
		} finally {
			a.close();
		}
	}

	/**
	 * Test Import-Packages marked with resolution:=dynamic are expanded, moved
	 * to DynamicImport-Packages with no original DIP instruction.
	 */
	public void testDynamicImportExpansionPackagesAreSet() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Import-Package", "org.osgi.service.*;resolution:=dynamic, *");
			p.put("Private-Package", "test.dynamicimport");

			a.addClasspath(new File("bin_test"));
			a.addClasspath(IO.getFile("jar/osgi.jar"));

			a.setProperties(p);
			Jar jar = a.build();
			assertTrue(a.check());

			String imports = jar.getManifest()
				.getMainAttributes()
				.getValue("Import-Package");
			String dynamicImports = jar.getManifest()
				.getMainAttributes()
				.getValue("DynamicImport-Package");

			assertTrue(imports.contains("org.osgi.framework;version=\"[1.3,2)\""));
			assertFalse(imports.contains("org.osgi.service.cm;version=\"[1.2,2)\""));
			assertFalse(imports.contains("org.osgi.service.event;version=\"[1.0,2)\""));
			assertTrue(dynamicImports.contains("org.osgi.service.cm;version=\"[1.2,2)\""));
			assertTrue(dynamicImports.contains("org.osgi.service.event;version=\"[1.0,2)\""));

		} finally {
			a.close();
		}
	}

	/**
	 * Test Import-Packages marked with resolution:=dynamic are expanded, moved
	 * to DynamicImport-Packages and added to the original DIP instruction.
	 */
	public void testDynamicImportExpansionPackagesAreAdded() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Import-Package", "org.osgi.service.*;resolution:=dynamic, *");
			p.put("DynamicImport-Package", "javax.servlet.*");
			p.put("Private-Package", "test.dynamicimport");

			a.addClasspath(new File("bin_test"));
			a.addClasspath(IO.getFile("jar/osgi.jar"));

			a.setProperties(p);
			Jar jar = a.build();
			assertTrue(a.check());

			String imports = jar.getManifest()
				.getMainAttributes()
				.getValue("Import-Package");
			String dynamicImports = jar.getManifest()
				.getMainAttributes()
				.getValue("DynamicImport-Package");

			assertTrue(imports.contains("org.osgi.framework;version=\"[1.3,2)\""));
			assertFalse(imports.contains("org.osgi.service.cm;version=\"[1.2,2)\""));
			assertFalse(imports.contains("org.osgi.service.event;version=\"[1.0,2)\""));
			assertTrue(dynamicImports.contains("org.osgi.service.cm;version=\"[1.2,2)\""));
			assertTrue(dynamicImports.contains("org.osgi.service.event;version=\"[1.0,2)\""));
			assertTrue(dynamicImports.contains("javax.servlet.*"));

		} finally {
			a.close();
		}
	}

	/**
	 * Use a private activator, check it is not imported.
	 *
	 * @throws Exception
	 */
	public void testPrivataBundleActivatorNotImported() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Import-Package", "!org.osgi.service.component, *");
			p.put("Private-Package", "test.activator");
			p.put("Bundle-Activator", "test.activator.Activator");
			a.addClasspath(new File("bin_test"));
			a.setProperties(p);
			a.build();
			Manifest manifest = a.getJar()
				.getManifest();

			assertTrue(a.check());

			String imports = manifest.getMainAttributes()
				.getValue("Import-Package");
			System.err.println(imports);
			assertEquals("org.osgi.framework", imports);
		} finally {
			a.close();
		}
	}

	/**
	 * Use an activator that is not in the bundle but do not allow it to be
	 * imported, this should generate an error.
	 *
	 * @throws Exception
	 */
	public void testBundleActivatorNotImported() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Import-Package", "!org.osgi.framework,*");
			p.put("Private-Package", "org.objectweb.*");
			p.put("Bundle-Activator", "org.osgi.framework.BundleActivator");
			a.setClasspath(new Jar[] {
				new Jar(IO.getFile("jar/asm.jar")), new Jar(IO.getFile("jar/osgi.jar"))
			});
			a.setProperties(p);
			a.build();
			assertTrue(a.check("Bundle-Activator not found"));
			Manifest manifest = a.getJar()
				.getManifest();
			String imports = manifest.getMainAttributes()
				.getValue("Import-Package");
			assertNull(imports);
		} finally {
			a.close();
		}

	}

	/**
	 * Use an activator that is on the class path but that is not in the bundle.
	 *
	 * @throws Exception
	 */
	public void testBundleActivatorImport() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Private-Package", "org.objectweb.*");
			p.put("Bundle-Activator", "org.osgi.framework.BundleActivator");
			a.setClasspath(new Jar[] {
				new Jar(IO.getFile("jar/asm.jar")), new Jar(IO.getFile("jar/osgi.jar"))
			});
			a.setProperties(p);
			a.build();
			Manifest manifest = a.getJar()
				.getManifest();

			assertEquals(0, a.getErrors()
				.size());
			assertEquals(1, a.getWarnings()
				.size());
			assertTrue(
				a.check("Bundle-Activator org.osgi.framework.BundleActivator is being imported into the bundle"));

			String imports = manifest.getMainAttributes()
				.getValue("Import-Package");
			assertNotNull(imports);
			assertTrue(imports.contains("org.osgi.framework"));
		} finally {
			a.close();
		}
	}

	/**
	 * Use an activator that abstract, and so cannot be instantiated.
	 *
	 * @throws Exception
	 */
	public void testBundleActivatorAbstract() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Private-Package", "test.activator");
			p.put("Bundle-Activator", "test.activator.AbstractActivator");
			a.addClasspath(new File("bin_test"));
			a.setProperties(p);
			a.build();
			Manifest manifest = a.getJar()
				.getManifest();

			assertEquals(1, a.getErrors()
				.size());
			assertEquals(0, a.getWarnings()
				.size());
			assertTrue(a.check("The Bundle Activator test.activator.AbstractActivator is abstract"));
		} finally {
			a.close();
		}
	}

	/**
	 * Use an activator that is an interface, and so cannot be instantiated.
	 *
	 * @throws Exception
	 */
	public void testBundleActivatorInterface() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Private-Package", "test.activator");
			p.put("Bundle-Activator", "test.activator.IActivator");
			a.addClasspath(new File("bin_test"));
			a.setProperties(p);
			a.build();
			Manifest manifest = a.getJar()
				.getManifest();

			assertEquals(1, a.getErrors()
				.size());
			assertEquals(0, a.getWarnings()
				.size());
			assertTrue(a.check("The Bundle Activator test.activator.IActivator is an interface"));
		} finally {
			a.close();
		}
	}

	/**
	 * Use an activator that has no default constructor, and so cannot be
	 * instantiated.
	 *
	 * @throws Exception
	 */
	public void testBundleActivatorNoDefaultConstructor() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Private-Package", "test.activator");
			p.put("Bundle-Activator", "test.activator.MissingNoArgsConstructorActivator");
			a.addClasspath(new File("bin_test"));
			a.setProperties(p);
			a.build();
			Manifest manifest = a.getJar()
				.getManifest();

			assertEquals(1, a.getErrors()
				.size());
			assertEquals(0, a.getWarnings()
				.size());
			assertTrue(a.check(
				"Bundle Activator classes must have a public zero-argument constructor and test.activator.MissingNoArgsConstructorActivator does not"));
		} finally {
			a.close();
		}
	}

	/**
	 * Use an activator that is not public, and so cannot be instantiated.
	 *
	 * @throws Exception
	 */
	public void testBundleActivatorNotPublic() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Private-Package", "test.activator");
			p.put("Bundle-Activator", "test.activator.DefaultVisibilityActivator");
			a.addClasspath(new File("bin_test"));
			a.setProperties(p);
			a.build();
			Manifest manifest = a.getJar()
				.getManifest();

			assertEquals(2, a.getErrors()
				.size());
			assertEquals(0, a.getWarnings()
				.size());
			assertTrue(a.check(
				"Bundle Activator classes must be public, and test.activator.DefaultVisibilityActivator is not",
				"Bundle Activator classes must have a public zero-argument constructor and test.activator.DefaultVisibilityActivator does not"));
		} finally {
			a.close();
		}
	}

	/**
	 * Use an activator that is not an instance of BundleActivator, and so
	 * cannot be used.
	 *
	 * @throws Exception
	 */
	public void testNotABundleActivator() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Private-Package", "test.activator");
			p.put("Bundle-Activator", "test.activator.NotAnActivator");
			a.addClasspath(new File("bin_test"));
			a.setProperties(p);
			a.build();
			Manifest manifest = a.getJar()
				.getManifest();

			assertEquals(1, a.getErrors()
				.size());
			assertEquals(0, a.getWarnings()
				.size());
			assertTrue(
				a.check("The Bundle Activator test.activator.NotAnActivator does not implement BundleActivator"));
		} finally {
			a.close();
		}
	}

	/**
	 * Scan for a BundleActivator, but there are no matches!
	 *
	 * @throws Exception
	 */
	public void testBundleActivatorNoType() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Private-Package", "test.api");
			p.put("Bundle-Activator", "");
			a.addClasspath(new File("bin_test"));
			a.setProperties(p);
			a.build();
			Manifest manifest = a.getJar()
				.getManifest();

			assertEquals(a.getErrors()
				.toString(), 0,
				a.getErrors()
					.size());
			assertEquals(a.getWarnings()
				.toString(), 1,
				a.getWarnings()
					.size());
			assertTrue(a.check("A Bundle-Activator header was present but no activator class was defined"));
		} finally {
			a.close();
		}
	}

	/**
	 * Scan for a BundleActivator, but the value is not a Java type identifier!
	 *
	 * @throws Exception
	 */
	public void testBundleActivatorNotAType() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Private-Package", "test.api");
			p.put("Bundle-Activator", "123");
			a.addClasspath(new File("bin_test"));
			a.setProperties(p);
			a.build();
			Manifest manifest = a.getJar()
				.getManifest();

			assertEquals(a.getErrors()
				.toString(), 2,
				a.getErrors()
					.size());
			assertEquals(a.getWarnings()
				.toString(), 0,
				a.getWarnings()
					.size());
			assertTrue(a.check("A Bundle-Activator header is present and its value is not a valid type name 123",
				"The default package '.' is not permitted by the Import-Package syntax."));
		} finally {
			a.close();
		}
	}

	/**
	 * Scan for a BundleActivator, but there are no matches!
	 *
	 * @throws Exception
	 */
	public void testScanForABundleActivatorNoMatches() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Private-Package", "test.api");
			p.put("Bundle-Activator", "${classes;IMPLEMENTS;org.osgi.framework.BundleActivator}");
			a.addClasspath(new File("bin_test"));
			a.setProperties(p);
			a.build();
			Manifest manifest = a.getJar()
				.getManifest();

			assertEquals(a.getErrors()
				.toString(), 1,
				a.getErrors()
					.size());
			assertEquals(a.getWarnings()
				.toString(), 0,
				a.getWarnings()
					.size());
			assertTrue(a.check(
				"A Bundle-Activator header is present but no activator class was found using the macro \\$\\{classes;IMPLEMENTS;org\\.osgi\\.framework\\.BundleActivator\\}"));
		} finally {
			a.close();
		}
	}

	/**
	 * Scan for a BundleActivator, but there are multiple matches!
	 *
	 * @throws Exception
	 */
	public void testScanForABundleActivatorMultipleMatches() throws Exception {
		Builder a = new Builder();
		try {
			Properties p = new Properties();
			p.put("Private-Package", "test.activator");
			p.put("Bundle-Activator", "${classes;IMPLEMENTS;org.osgi.framework.BundleActivator}");
			a.addClasspath(new File("bin_test"));
			a.setProperties(p);
			a.build();
			Manifest manifest = a.getJar()
				.getManifest();

			assertEquals(a.getErrors()
				.toString(), 1,
				a.getErrors()
					.size());
			assertEquals(a.getWarnings()
				.toString(), 0,
				a.getWarnings()
					.size());
			assertTrue(
				a.check("The Bundle-Activator header only supports a single type. The following types were found: "
					+ "test.activator.AbstractActivator,test.activator.Activator,test.activator.Activator11,test.activator.Activator2,test.activator.Activator3,test.activator.ActivatorPackage,test.activator.ActivatorPrivate,test.activator.DefaultVisibilityActivator,test.activator.IActivator,test.activator.MissingNoArgsConstructorActivator"
					+ ". This usually happens when a macro resolves to multiple types"));
		} finally {
			a.close();
		}
	}

	/**
	 * The -removeheaders header removes any necessary after the manifest is
	 * calculated.
	 */

	public void testRemoveheaders() throws Exception {
		try (Analyzer a = new Analyzer()) {
			a.setJar(IO.getFile("jar/asm.jar"));
			Manifest m = a.calcManifest();
			assertNotNull(m.getMainAttributes()
				.getValue("Implementation-Title"));
		}
		try (Analyzer a = new Analyzer()) {
			a.setJar(IO.getFile("jar/asm.jar"));
			a.setProperty("-removeheaders", "Implementation-Title");
			Manifest m = a.calcManifest();
			assertNull(m.getMainAttributes()
				.getValue("Implementation-Title"));
		}

	}

	/**
	 * There was an export generated for a jar file.
	 *
	 * @throws Exception
	 */
	public void testExportForJar() throws Exception {
		try (Analyzer an = new Analyzer()) {
			Jar jar = new Jar("dot");
			jar.putResource("target/aopalliance.jar", new FileResource(IO.getFile("jar/asm.jar")));
			an.setJar(jar);
			an.setProperty("Export-Package", "target");
			Manifest manifest = an.calcManifest();
			assertTrue(an.check());
			String exports = manifest.getMainAttributes()
				.getValue(Constants.EXPORT_PACKAGE);
			Parameters map = Processor.parseHeader(exports, null);
			assertEquals(1, map.size());
			assertEquals("target", map.keySet()
				.iterator()
				.next());
		}

	}

	/**
	 * Test if version works
	 *
	 * @throws IOException
	 */

	public void testVersion() throws IOException {
		try (Analyzer a = new Analyzer()) {
			String v = a.getBndVersion();
			assertNotNull(v);
		}
	}

	/**
	 * asm is a simple library with two packages. No imports are done.
	 */
	public void testAsm() throws Exception {
		Properties base = new Properties();
		base.put(Constants.IMPORT_PACKAGE, "*");
		base.put(Constants.EXPORT_PACKAGE, "*;-noimport:=true");

		try (Analyzer analyzer = new Analyzer()) {
			analyzer.setJar(IO.getFile("jar/asm.jar"));
			analyzer.setProperties(base);
			analyzer.calcManifest()
				.write(System.err);
			assertTrue(analyzer.check());

			assertTrue(analyzer.getExports()
				.getByFQN("org.objectweb.asm.signature") != null);
			assertTrue(analyzer.getExports()
				.getByFQN("org.objectweb.asm") != null);
			assertFalse(analyzer.getImports()
				.getByFQN("org.objectweb.asm.signature") != null);
			assertFalse(analyzer.getImports()
				.getByFQN("org.objectweb.asm") != null);

			assertEquals("Expected size", 2, analyzer.getExports()
				.size());
		}

	}

	/**
	 * See if we set attributes on export
	 *
	 * @throws IOException
	 */
	public void testAsm2() throws Exception {
		Properties base = new Properties();
		base.put(Constants.IMPORT_PACKAGE, "*");
		base.put(Constants.EXPORT_PACKAGE, "org.objectweb.asm;name=short, org.objectweb.asm.signature;name=long");
		try (Analyzer h = new Analyzer()) {
			h.setJar(IO.getFile("jar/asm.jar"));
			h.setProperties(base);
			h.calcManifest()
				.write(System.err);
			assertTrue(h.check());
			Packages exports = h.getExports();
			assertTrue(exports.getByFQN("org.objectweb.asm.signature") != null);
			assertTrue(exports.getByFQN("org.objectweb.asm") != null);
			Packages imports = h.getImports();
			assertTrue(imports.getByFQN("org.objectweb.asm.signature") == null);
			assertTrue(imports.getByFQN("org.objectweb.asm") == null);
			assertEquals("Expected size", 2, exports.size());
			assertEquals("short", get(exports, h.getPackageRef("org.objectweb.asm"), "name"));
			assertEquals("long", get(exports, h.getPackageRef("org.objectweb.asm.signature"), "name"));
		}
	}

	public void testDs() throws Exception {
		Properties base = new Properties();
		base.put(Constants.IMPORT_PACKAGE, "*");
		base.put(Constants.EXPORT_PACKAGE, "*;-noimport:=true");
		File tmp = IO.getFile("jar/ds.jar");
		try (Analyzer analyzer = new Analyzer()) {
			analyzer.setJar(tmp);
			analyzer.setProperties(base);
			analyzer.calcManifest()
				.write(System.err);
			assertTrue(analyzer.check());
			assertPresent(analyzer.getImports()
				.keySet(),
				"org.osgi.service.packageadmin, " + "org.xml.sax, org.osgi.service.log," + " javax.xml.parsers,"
					+ " org.xml.sax.helpers," + " org.osgi.framework," + " org.eclipse.osgi.util,"
					+ " org.osgi.util.tracker, " + "org.osgi.service.component, " + "org.osgi.service.cm");
			assertPresent(analyzer.getExports()
				.keySet(),
				"org.eclipse.equinox.ds.parser, " + "org.eclipse.equinox.ds.tracker, " + "org.eclipse.equinox.ds, "
					+ "org.eclipse.equinox.ds.instance, " + "org.eclipse.equinox.ds.model, "
					+ "org.eclipse.equinox.ds.resolver, " + "org.eclipse.equinox.ds.workqueue");
		}
	}

	public void testDsSkipOsgiImport() throws Exception {
		Properties base = new Properties();
		base.put(Constants.IMPORT_PACKAGE, "!org.osgi.*, *");
		base.put(Constants.EXPORT_PACKAGE, "*;-noimport:=true");
		File tmp = IO.getFile("jar/ds.jar");
		try (Analyzer h = new Analyzer()) {
			h.setJar(tmp);
			h.setProperties(base);
			h.calcManifest()
				.write(System.err);
			assertPresent(h.getImports()
				.keySet(),
				"org.xml.sax, " + " javax.xml.parsers," + " org.xml.sax.helpers," + " org.eclipse.osgi.util");

			System.err.println("IMports " + h.getImports());
			assertNotPresent(h.getImports()
				.keySet(),
				"org.osgi.service.packageadmin, " + "org.osgi.service.log," + " org.osgi.framework,"
					+ " org.osgi.util.tracker, " + "org.osgi.service.component, " + "org.osgi.service.cm");
			assertPresent(h.getExports()
				.keySet(),
				"org.eclipse.equinox.ds.parser, " + "org.eclipse.equinox.ds.tracker, " + "org.eclipse.equinox.ds, "
					+ "org.eclipse.equinox.ds.instance, " + "org.eclipse.equinox.ds.model, "
					+ "org.eclipse.equinox.ds.resolver, " + "org.eclipse.equinox.ds.workqueue");
		}
	}

	public void testDsNoExport() throws Exception {
		Properties base = new Properties();
		base.put(Constants.IMPORT_PACKAGE, "*");
		base.put(Constants.EXPORT_PACKAGE, "!*");
		File tmp = IO.getFile("jar/ds.jar");
		try (Analyzer h = new Analyzer()) {
			h.setJar(tmp);
			h.setProperties(base);
			h.calcManifest()
				.write(System.err);
			assertPresent(h.getImports()
				.keySet(),
				"org.osgi.service.packageadmin, " + "org.xml.sax, org.osgi.service.log," + " javax.xml.parsers,"
					+ " org.xml.sax.helpers," + " org.osgi.framework," + " org.eclipse.osgi.util,"
					+ " org.osgi.util.tracker, " + "org.osgi.service.component, " + "org.osgi.service.cm");
			assertNotPresent(h.getExports()
				.keySet(),
				"org.eclipse.equinox.ds.parser, " + "org.eclipse.equinox.ds.tracker, " + "org.eclipse.equinox.ds, "
					+ "org.eclipse.equinox.ds.instance, " + "org.eclipse.equinox.ds.model, "
					+ "org.eclipse.equinox.ds.resolver, " + "org.eclipse.equinox.ds.workqueue");
			System.err.println(h.getUnreachable());
		}
	}

	public void testClasspath() throws Exception {
		Properties base = new Properties();
		base.put(Constants.IMPORT_PACKAGE, "*");
		base.put(Constants.EXPORT_PACKAGE, "*;-noimport:=true");
		File tmp = IO.getFile("jar/ds.jar");
		File osgi = IO.getFile("jar/osgi.jar");
		try (Analyzer h = new Analyzer()) {
			h.setJar(tmp);
			h.setProperties(base);
			h.setClasspath(new File[] {
				osgi
			});
			h.calcManifest()
				.write(System.err);
			assertEquals("Version from osgi.jar", "[1.2,2)",
				get(h.getImports(), h.getPackageRef("org.osgi.service.packageadmin"), "version"));
			assertEquals("Version from osgi.jar", "[1.3,2)",
				get(h.getImports(), h.getPackageRef("org.osgi.util.tracker"), "version"));
			assertEquals("Version from osgi.jar", null, get(h.getImports(), h.getPackageRef("org.xml.sax"), "version"));
		}
	}

	/**
	 * We detect that there are instruction on im/export package headers that
	 * are never used. This usually indicates a misunderstanding or a change in
	 * the underlying classpath. These are reflected as warnings. If there is an
	 * extra import, and it contains no wildcards, then it is treated as a
	 * wildcard
	 *
	 * @throws IOException
	 */
	public void testSuperfluous() throws Exception {
		Properties base = new Properties();
		base.put(Constants.IMPORT_PACKAGE, "*, =com.foo, com.foo.bar.*");
		base.put(Constants.EXPORT_PACKAGE, "*, com.bar, baz.*");
		File tmp = IO.getFile("jar/ds.jar");
		try (Analyzer h = new Analyzer()) {
			h.setJar(tmp);
			h.setProperties(base);
			Manifest m = h.calcManifest();
			m.write(System.err);
			assertTrue(h.check( //
				"Unused Export-Package instructions: \\[baz.*\\]", //
				"Unused Import-Package instructions: \\[com.foo.bar.*\\]"));
			assertTrue(h.getImports()
				.getByFQN("com.foo") != null);
			assertTrue(h.getExports()
				.getByFQN("com.bar") != null);
		}
	}

	static void assertNotPresent(Collection<?> map, String string) {
		Collection<String> ss = new HashSet<>();
		for (Object o : map)
			ss.add(o + "");

		StringTokenizer st = new StringTokenizer(string, ", ");
		while (st.hasMoreTokens()) {
			String member = st.nextToken();
			assertFalse("Must not contain  " + member, map.contains(member));
		}
	}

	static void assertPresent(Collection<?> map, String string) {
		Collection<String> ss = new HashSet<>();
		for (Object o : map)
			ss.add(o + "");

		StringTokenizer st = new StringTokenizer(string, ", ");
		while (st.hasMoreTokens()) {
			String member = st.nextToken();
			assertTrue("Must contain  " + member, ss.contains(member));
		}
	}

	static <K, V> V get(Map<K, ? extends Map<String, V>> headers, K key, String attr) {
		Map<String, V> clauses = headers.get(key);
		if (clauses == null)
			return null;
		return clauses.get(attr);
	}
}
