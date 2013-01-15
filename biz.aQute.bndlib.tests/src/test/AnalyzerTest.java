package test;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Clazz.Def;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.test.*;
import aQute.lib.io.*;

class T0 {}

abstract class T1 extends T0 {}

class T2 extends T1 {}

class T3 extends T2 {}

public class AnalyzerTest extends BndTestCase {
	static File	cwd	= new File(System.getProperty("user.dir"));

	/**
	 * For the following annotation class in an OSGi bundle
	 * 
	 * @Retention(RetentionPolicy.RUNTIME)
	 * @Target({ ElementType.TYPE, ElementType.METHOD }) public @interface
	 *           Transactional {
	 * @Nonbinding Class<? extends Annotation>[] qualifier() default Any.class;
	 *             } maven-bundle-plugin fails to generate the package import
	 *             for javax.enterprise.inject.Any, the default value of the
	 *             annotation method. At runtime, this leads to a
	 *             non-descriptive exception
	 * 
	 *             <pre>
	 * Caused by: java.lang.ArrayStoreException: sun.reflect.annotation.TypeNotPresentExceptionProxy 
	 *         at sun.reflect.annotation.AnnotationParser.parseClassArray(AnnotationParser.java:673) ~[na:1.7.0_04] 
	 *         at sun.reflect.annotation.AnnotationParser.parseArray(AnnotationParser.java:480) ~[na:1.7.0_04] 
	 *         at sun.reflect.annotation.AnnotationParser.parseMemberValue(AnnotationParser.java:306) ~[na:1.7.0_04] 
	 *         at java.lang.reflect.Method.getDefaultValue(Method.java:726) ~[na:1.7.0_04] 
	 *         at sun.reflect.annotation.AnnotationType.<init>(AnnotationType.java:117) ~[na:1.7.0_04] 
	 *         at sun.reflect.annotation.AnnotationType.getInstance(AnnotationType.java:84) ~[na:1.7.0_04] 
	 *         at sun.reflect.annotation.AnnotationParser.parseAnnotation(AnnotationParser.java:221) ~[na:1.7.0_04] 
	 *         at sun.reflect.annotation.AnnotationParser.parseAnnotations2(AnnotationParser.java:88) ~[na:1.7.0_04] 
	 *         at sun.reflect.annotation.AnnotationParser.parseAnnotations(AnnotationParser.java:70) ~[na:1.7.0_04] 
	 *         at java.lang.Class.initAnnotationsIfNecessary(Class.java:3089) ~[na:1.7.0_04] 
	 *         at java.lang.Class.getDeclaredAnnotations(Class.java:3077) ~[na:1.7.0_04]
	 * </pre>
	 */
	public void testAnnotationWithDefaultClass() throws Exception {
		Builder b = new Builder();
		b.addClasspath(IO.getFile(cwd, "bin"));
		b.setExportPackage("test.annotation");
		b.build();
		assertTrue(b.check());
		assertTrue( b.getExports().containsKey(b.getPackageRef("test/annotation")));
		assertFalse( b.getContained().containsKey(b.getPackageRef("test/annotation/any")));
		assertTrue( b.getImports().containsKey(b.getPackageRef("test/annotation/any")));
	}
	 

	/**
	 * Check the cross references
	 */

	public static void testCrossReference() throws Exception {
		Builder b = new Builder();
		b.addClasspath(IO.getFile(cwd, "bin"));
		b.addClasspath(IO.getFile(cwd, "../aQute.libg/bin"));
		b.addClasspath(IO.getFile(cwd, "../biz.aQute.bndlib/bin"));
		b.setExportPackage("aQute.bnd.build.model.conversions");
		b.setConditionalPackage("aQute.lib*");
		Jar jar = b.build();
		assertTrue(b.check("((, )?(aQute.libg.tuple)){1,1}"));

		Map<Def,List<TypeRef>> xRef = b.getXRef(b.getPackageRef("aQute.bnd.build.model.conversions"),
				Arrays.asList(b.getPackageRef("aQute.libg.tuple")), Modifier.PUBLIC + Modifier.PROTECTED);

		for (Clazz.Def def : xRef.keySet())
			System.out.println(def.getOwnerType() + " " + def.getName() + " " + def.getType() + " : " + xRef.get(def));

		assertEquals(5, xRef.size());
	}

	/**
	 * The -removeheaders header can be used as a whitelist.
	 */

	public static void testRemoveheadersAsWhiteList() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/asm.jar"));
		b.setExportPackage("*");
		b.setImportPackage("something");
		b.set("Foo", "Foo");
		b.set("Bar", "Bar");
		b.set(Constants.REMOVEHEADERS, "!Bundle-*,!*-Package,!Service-Component,*");
		b.build();
		assertTrue(b.check());
		Manifest m = b.getJar().getManifest();

		assertNotNull(m.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION));
		assertNotNull(m.getMainAttributes().getValue(Constants.BUNDLE_NAME));
		assertNotNull(m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME));
		assertNotNull(m.getMainAttributes().getValue(Constants.IMPORT_PACKAGE));
		assertNotNull(m.getMainAttributes().getValue(Constants.EXPORT_PACKAGE));
		assertNull(m.getMainAttributes().getValue("Foo"));
		assertNull(m.getMainAttributes().getValue("Bar"));
	}

	/**
	 * Check if bnd detects references to private packages and gives a warning.
	 */

	public static void testExportReferencesToPrivatePackages() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.addClasspath(new File("bin"));
		b.setExportPackage("test.referApi"); // refers to Event Admin
		b.setConditionalPackage("org.osgi.service.*");
		Jar jar = b.build();
		assertTrue(b
				.check("((, )?(org.osgi.service.event|org.osgi.service.component|org.osgi.service.http|org.osgi.service.log|org.osgi.service.condpermadmin|org.osgi.service.wireadmin|org.osgi.service.device)){7,7}"));
	}

	/**
	 * Test basic functionality of he BCP
	 */

	public static void testBundleClasspath() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.BUNDLE_CLASSPATH, "foo");
		b.setProperty(Constants.INCLUDE_RESOURCE, "foo/test/refer=bin/test/refer");
		b.setProperty(Constants.EXPORT_CONTENTS, "test.refer");
		Jar jar = b.build();
		Manifest m = jar.getManifest();
		assertTrue(b.check());
		m.write(System.err);
	}

	/**
	 * Very basic sanity test
	 */

	public static void testSanity() throws Exception {
		Builder b = new Builder();
		b.set("Export-Package", "thinlet;version=1.0");
		b.addClasspath(new File("jar/thinlet.jar"));
		b.build();
		assertTrue(b.check());
		assertEquals("version=1.0", b.getExports().getByFQN("thinlet").toString());
		assertTrue(b.getJar().getDirectories().containsKey("thinlet"));
		assertTrue(b.getJar().getResources().containsKey("thinlet/Thinlet.class"));
	}

	/**
	 * Fastest way to create a manifest
	 * 
	 * @throws Exception
	 */

	public static void testGenerateManifest() throws Exception {
		Analyzer analyzer = new Analyzer();
		Jar bin = new Jar(new File("jar/osgi.jar"));
		bin.setManifest(new Manifest());
		analyzer.setJar(bin);
		analyzer.addClasspath(new File("jar/spring.jar"));
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
		assertEquals("1.0.0.x", manifest.getMainAttributes().getValue("Bundle-Version"));
	}

	/**
	 * Make sure packages from embedded directories referenced from
	 * Bundle-Classpath are considered during import/export calculation.
	 */

	public static void testExportContentsDirectory() throws Exception {
		Builder b = new Builder();
		File embedded = new File("bin/test/refer").getCanonicalFile();
		assertTrue(embedded.isDirectory()); // sanity check
		b.setProperty("Bundle-ClassPath", ".,jars/some.jar");
		b.setProperty("-includeresource", "jars/some.jar/test/refer=" + embedded.getAbsolutePath());
		b.setProperty("-exportcontents", "test.refer");
		b.build();
		assertTrue(b.check("Bundle-ClassPath uses a directory 'jars/some.jar'"));
		assertTrue(b.getImports().toString(), b.getImports().getByFQN("org.osgi.service.event") != null);
	}

	/**
	 * Uses constraints must be filtered by imports or exports.
	 * 
	 * @throws Exception
	 */

	public static void testUsesFiltering() throws Exception {
		Builder b = new Builder();
		b.setTrace(true);
		b.addClasspath(new File("jar/osgi.jar"));
		b.setProperty("Export-Package", "org.osgi.service.event");
		Jar jar = b.build();
		assertTrue(b.check());

		assertNotNull(jar.getResource("org/osgi/service/event/EventAdmin.class"));

		String exports = jar.getManifest().getMainAttributes().getValue("Export-Package");
		System.err.println(exports);
		assertTrue(exports.contains("uses:=\"org.osgi.framework\""));

		b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.setProperty("Import-Package", "");
		b.setProperty("Export-Package", "org.osgi.service.event");
		b.setPedantic(true);
		jar = b.build();
		exports = jar.getManifest().getMainAttributes().getValue("Export-Package");
		System.err.println(exports);
		assertTrue(b.check("Empty Import-Package header"));
		exports = jar.getManifest().getMainAttributes().getValue("Export-Package");
		assertFalse(exports.contains("uses:=\"org.osgi.framework\""));

	}

	/**
	 * Test if require works
	 * 
	 * @throws Exception
	 */

	public static void testRequire() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.setProperty("Private-Package", "org.osgi.framework");
		b.setProperty("-require-bnd", "10000");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(1, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

	}

	public static void testComponentImportReference() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.setProperty("Private-Package", "org.osgi.framework");
		b.setProperty("Import-Package", "not.here,*");
		b.setProperty("Service-Component", "org.osgi.framework.Bundle;ref=not.here.Reference");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
	}

	public static void testFindClass() throws Exception {
		Builder a = new Builder();
		a.setProperty("Export-Package", "org.osgi.service.io");
		a.addClasspath(new File("jar/osgi.jar"));
		a.build();
		System.err.println(a.getErrors());
		System.err.println(a.getWarnings());

		Collection<Clazz> c = a.getClasses("", "IMPORTS", "javax.microedition.io");
		System.err.println(c);
	}

	public static void testMultilevelInheritance() throws Exception {
		Analyzer a = new Analyzer();
		a.setJar(new File("bin"));
		a.analyze();

		String result = a._classes("cmd", "named", "*T?", "extends", "test.T0", "concrete");
		System.err.println(result);
		assertTrue(result.contains("test.T2"));
		assertTrue(result.contains("test.T3"));
	}

	public static void testClassQuery() throws Exception {
		Analyzer a = new Analyzer();
		a.setJar(new File("jar/osgi.jar"));
		a.analyze();

		String result = a._classes("cmd", "named", "org.osgi.service.http.*", "abstract");
		TreeSet<String> r = new TreeSet<String>(Processor.split(result));
		assertEquals(
				new TreeSet<String>(Arrays.asList("org.osgi.service.http.HttpContext",
						"org.osgi.service.http.HttpService")), r);
	}

	/**
	 * Use a private activator, check it is not imported.
	 * 
	 * @throws Exception
	 */
	public static void testEmptyHeader() throws Exception {
		Builder a = new Builder();
		a.setProperty("Bundle-Blueprint", "  <<EMPTY>> ");
		a.setProperty("Export-Package", "org.osgi.framework");
		a.addClasspath(new File("jar/osgi.jar"));
		a.build();
		Manifest manifest = a.getJar().getManifest();
		System.err.println(a.getErrors());
		System.err.println(a.getWarnings());
		assertEquals(0, a.getErrors().size());
		assertEquals(0, a.getWarnings().size());
		String bb = manifest.getMainAttributes().getValue("Bundle-Blueprint");
		System.err.println(bb);
		assertNotNull(bb);
		assertEquals("", bb);
	}

	/**
	 * Test name section.
	 */

	public static void testNameSection() throws Exception {
		Builder a = new Builder();
		a.setProperty("Export-Package", "org.osgi.service.event, org.osgi.service.io");
		a.addClasspath(new File("jar/osgi.jar"));
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
	}

	/**
	 * Check if calcManifest sets the version
	 */

	/**
	 * Test if mandatory attributes are augmented even when the version is not
	 * set.
	 */
	public static void testMandatoryWithoutVersion() throws Exception {
		Builder a = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "*");
		p.put("Private-Package", "org.apache.mina.management.*");
		a.setClasspath(new Jar[] {
			new Jar(new File("jar/mandatorynoversion.jar"))
		});
		a.setProperties(p);
		Jar jar = a.build();
		assertTrue(a.check());

		String imports = jar.getManifest().getMainAttributes().getValue("Import-Package");
		System.err.println(imports);
		assertTrue(imports.indexOf("x=1") >= 0);
		assertTrue(imports.indexOf("y=2") >= 0);
	}

	/**
	 * Use a private activator, check it is not imported.
	 * 
	 * @throws Exception
	 */
	public static void testPrivataBundleActivatorNotImported() throws Exception {
		Builder a = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "*");
		p.put("Private-Package", "org.objectweb.*");
		p.put("Bundle-Activator", "org.objectweb.asm.Item");
		a.setClasspath(new Jar[] {
			new Jar(new File("jar/asm.jar"))
		});
		a.setProperties(p);
		a.build();
		Manifest manifest = a.getJar().getManifest();
		System.err.println(a.getErrors());
		System.err.println(a.getWarnings());
		assertEquals(0, a.getErrors().size());
		assertEquals(0, a.getWarnings().size());
		String imports = manifest.getMainAttributes().getValue("Import-Package");
		System.err.println(imports);
		assertNull(imports);
	}

	/**
	 * Use an activator that is not in the bundle but do not allow it to be
	 * imported, this should generate an error.
	 * 
	 * @throws Exception
	 */
	public static void testBundleActivatorNotImported() throws Exception {
		Builder a = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "!org.osgi.framework,*");
		p.put("Private-Package", "org.objectweb.*");
		p.put("Bundle-Activator", "org.osgi.framework.BundleActivator");
		a.setClasspath(new Jar[] {
				new Jar(new File("jar/asm.jar")), new Jar(new File("jar/osgi.jar"))
		});
		a.setProperties(p);
		a.build();
		assertTrue(a.check("Bundle-Activator not found"));
		Manifest manifest = a.getJar().getManifest();
		String imports = manifest.getMainAttributes().getValue("Import-Package");
		assertNull(imports);
	}

	/**
	 * Use an activator that is on the class path but that is not in the bundle.
	 * 
	 * @throws Exception
	 */
	public static void testBundleActivatorImport() throws Exception {
		Builder a = new Builder();
		Properties p = new Properties();
		p.put("Private-Package", "org.objectweb.*");
		p.put("Bundle-Activator", "org.osgi.framework.BundleActivator");
		a.setClasspath(new Jar[] {
				new Jar(new File("jar/asm.jar")), new Jar(new File("jar/osgi.jar"))
		});
		a.setProperties(p);
		a.build();
		Manifest manifest = a.getJar().getManifest();
		System.err.println(a.getErrors());
		System.err.println(a.getWarnings());
		assertEquals(0, a.getErrors().size());
		assertEquals(0, a.getWarnings().size());
		String imports = manifest.getMainAttributes().getValue("Import-Package");
		assertNotNull(imports);
		assertTrue(imports.indexOf("org.osgi.framework") >= 0);
	}

	/**
	 * The -removeheaders header removes any necessary after the manifest is
	 * calculated.
	 */

	public static void testRemoveheaders() throws Exception {
		Analyzer a = new Analyzer();
		a.setJar(new File("jar/asm.jar"));
		Manifest m = a.calcManifest();
		assertNotNull(m.getMainAttributes().getValue("Implementation-Title"));
		a = new Analyzer();
		a.setJar(new File("jar/asm.jar"));
		a.setProperty("-removeheaders", "Implementation-Title");
		m = a.calcManifest();
		assertNull(m.getMainAttributes().getValue("Implementation-Title"));
	}

	/**
	 * There was an export generated for a jar file.
	 * 
	 * @throws Exception
	 */
	public static void testExportForJar() throws Exception {
		Jar jar = new Jar("dot");
		jar.putResource("target/aopalliance.jar", new FileResource(new File("jar/asm.jar")));
		Analyzer an = new Analyzer();
		an.setJar(jar);
		an.setProperty("Export-Package", "target");
		Manifest manifest = an.calcManifest();
		assertTrue(an.check());
		String exports = manifest.getMainAttributes().getValue(Analyzer.EXPORT_PACKAGE);
		Parameters map = Analyzer.parseHeader(exports, null);
		assertEquals(1, map.size());
		assertEquals("target", map.keySet().iterator().next());
	}

	/**
	 * Test if version works
	 */

	public static void testVersion() {
		Analyzer a = new Analyzer();
		String v = a.getBndVersion();
		assertNotNull(v);
	}

	/**
	 * asm is a simple library with two packages. No imports are done.
	 */
	public static void testAsm() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "*");
		base.put(Analyzer.EXPORT_PACKAGE, "*;-noimport:=true");

		Analyzer analyzer = new Analyzer();
		analyzer.setJar(new File("jar/asm.jar"));
		analyzer.setProperties(base);
		analyzer.calcManifest().write(System.err);
		assertTrue(analyzer.check());

		assertTrue(analyzer.getExports().getByFQN("org.objectweb.asm.signature") != null);
		assertTrue(analyzer.getExports().getByFQN("org.objectweb.asm") != null);
		assertFalse(analyzer.getImports().getByFQN("org.objectweb.asm.signature") != null);
		assertFalse(analyzer.getImports().getByFQN("org.objectweb.asm") != null);

		assertEquals("Expected size", 2, analyzer.getExports().size());
	}

	/**
	 * See if we set attributes on export
	 * 
	 * @throws IOException
	 */
	public static void testAsm2() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "*");
		base.put(Analyzer.EXPORT_PACKAGE, "org.objectweb.asm;name=short, org.objectweb.asm.signature;name=long");
		Analyzer h = new Analyzer();
		h.setJar(new File("jar/asm.jar"));
		h.setProperties(base);
		h.calcManifest().write(System.err);
		assertTrue(h.check());
		Packages exports = h.getExports();
		assertTrue(exports.getByFQN("org.objectweb.asm.signature") != null);
		assertTrue(exports.getByFQN("org.objectweb.asm") != null);
		assertTrue(Arrays.asList("org.objectweb.asm", "org.objectweb.asm.signature").removeAll(h.getImports().keySet()) == false);
		assertEquals("Expected size", 2, h.getExports().size());
		assertEquals("short", get(h.getExports(), h.getPackageRef("org.objectweb.asm"), "name"));
		assertEquals("long", get(h.getExports(), h.getPackageRef("org.objectweb.asm.signature"), "name"));
	}

	public static void testDs() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "*");
		base.put(Analyzer.EXPORT_PACKAGE, "*;-noimport:=true");
		File tmp = new File("jar/ds.jar");
		Analyzer analyzer = new Analyzer();
		analyzer.setJar(tmp);
		analyzer.setProperties(base);
		analyzer.calcManifest().write(System.err);
		assertTrue(analyzer.check());
		assertPresent(analyzer.getImports().keySet(), "org.osgi.service.packageadmin, "
				+ "org.xml.sax, org.osgi.service.log," + " javax.xml.parsers," + " org.xml.sax.helpers,"
				+ " org.osgi.framework," + " org.eclipse.osgi.util," + " org.osgi.util.tracker, "
				+ "org.osgi.service.component, " + "org.osgi.service.cm");
		assertPresent(analyzer.getExports().keySet(), "org.eclipse.equinox.ds.parser, "
				+ "org.eclipse.equinox.ds.tracker, " + "org.eclipse.equinox.ds, " + "org.eclipse.equinox.ds.instance, "
				+ "org.eclipse.equinox.ds.model, " + "org.eclipse.equinox.ds.resolver, "
				+ "org.eclipse.equinox.ds.workqueue");

	}

	public static void testDsSkipOsgiImport() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "!org.osgi.*, *");
		base.put(Analyzer.EXPORT_PACKAGE, "*;-noimport:=true");
		File tmp = new File("jar/ds.jar");
		Analyzer h = new Analyzer();
		h.setJar(tmp);
		h.setProperties(base);
		h.calcManifest().write(System.err);
		assertPresent(h.getImports().keySet(), "org.xml.sax, " + " javax.xml.parsers," + " org.xml.sax.helpers,"
				+ " org.eclipse.osgi.util");

		System.err.println("IMports " + h.getImports());
		assertNotPresent(h.getImports().keySet(), "org.osgi.service.packageadmin, " + "org.osgi.service.log,"
				+ " org.osgi.framework," + " org.osgi.util.tracker, " + "org.osgi.service.component, "
				+ "org.osgi.service.cm");
		assertPresent(h.getExports().keySet(), "org.eclipse.equinox.ds.parser, " + "org.eclipse.equinox.ds.tracker, "
				+ "org.eclipse.equinox.ds, " + "org.eclipse.equinox.ds.instance, " + "org.eclipse.equinox.ds.model, "
				+ "org.eclipse.equinox.ds.resolver, " + "org.eclipse.equinox.ds.workqueue");
	}

	public static void testDsNoExport() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "*");
		base.put(Analyzer.EXPORT_PACKAGE, "!*");
		File tmp = new File("jar/ds.jar");
		Analyzer h = new Analyzer();
		h.setJar(tmp);
		h.setProperties(base);
		h.calcManifest().write(System.err);
		assertPresent(h.getImports().keySet(), "org.osgi.service.packageadmin, " + "org.xml.sax, org.osgi.service.log,"
				+ " javax.xml.parsers," + " org.xml.sax.helpers," + " org.osgi.framework," + " org.eclipse.osgi.util,"
				+ " org.osgi.util.tracker, " + "org.osgi.service.component, " + "org.osgi.service.cm");
		assertNotPresent(h.getExports().keySet(), "org.eclipse.equinox.ds.parser, "
				+ "org.eclipse.equinox.ds.tracker, " + "org.eclipse.equinox.ds, " + "org.eclipse.equinox.ds.instance, "
				+ "org.eclipse.equinox.ds.model, " + "org.eclipse.equinox.ds.resolver, "
				+ "org.eclipse.equinox.ds.workqueue");
		System.err.println(h.getUnreachable());
	}

	public static void testClasspath() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "*");
		base.put(Analyzer.EXPORT_PACKAGE, "*;-noimport:=true");
		File tmp = new File("jar/ds.jar");
		File osgi = new File("jar/osgi.jar");
		Analyzer h = new Analyzer();
		h.setJar(tmp);
		h.setProperties(base);
		h.setClasspath(new File[] {
			osgi
		});
		h.calcManifest().write(System.err);
		assertEquals("Version from osgi.jar", "[1.2,2)",
				get(h.getImports(), h.getPackageRef("org.osgi.service.packageadmin"), "version"));
		assertEquals("Version from osgi.jar", "[1.3,2)",
				get(h.getImports(), h.getPackageRef("org.osgi.util.tracker"), "version"));
		assertEquals("Version from osgi.jar", null, get(h.getImports(), h.getPackageRef("org.xml.sax"), "version"));

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
	public static void testSuperfluous() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "*, =com.foo, com.foo.bar.*");
		base.put(Analyzer.EXPORT_PACKAGE, "*, com.bar, baz.*");
		File tmp = new File("jar/ds.jar");
		Analyzer h = new Analyzer();
		h.setJar(tmp);
		h.setProperties(base);
		Manifest m = h.calcManifest();
		m.write(System.err);
		assertTrue(h.check( //
				"Unused Export-Package instructions: \\[baz.*\\]", //
				"Unused Import-Package instructions: \\[com.foo.bar.*\\]"));
		assertTrue(h.getImports().getByFQN("com.foo") != null);
		assertTrue(h.getExports().getByFQN("com.bar") != null);
	}

	static void assertNotPresent(Collection< ? > map, String string) {
		Collection<String> ss = new HashSet<String>();
		for (Object o : map)
			ss.add(o + "");

		StringTokenizer st = new StringTokenizer(string, ", ");
		while (st.hasMoreTokens()) {
			String member = st.nextToken();
			assertFalse("Must not contain  " + member, map.contains(member));
		}
	}

	static void assertPresent(Collection< ? > map, String string) {
		Collection<String> ss = new HashSet<String>();
		for (Object o : map)
			ss.add(o + "");

		StringTokenizer st = new StringTokenizer(string, ", ");
		while (st.hasMoreTokens()) {
			String member = st.nextToken();
			assertTrue("Must contain  " + member, ss.contains(member));
		}
	}

	static <K, V> V get(Map<K, ? extends Map<String,V>> headers, K key, String attr) {
		Map<String,V> clauses = headers.get(key);
		if (clauses == null)
			return null;
		return clauses.get(attr);
	}
}
