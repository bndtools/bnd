package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.test.*;
import aQute.lib.collections.*;
import aQute.lib.osgi.*;
import aQute.libg.header.*;

public class BuilderTest extends BndTestCase {

	/**
	 * Test the name section
	 */

	public void testNamesection() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.setProperty(Constants.NAMESECTION,
				"org/osgi/service/event/*;MD5='${md5;${@}}';SHA1='${sha1;${@}}';MD5H='${md5;${@};hex}'");
		b.setProperty(Constants.PRIVATE_PACKAGE, "org.osgi.service.event");
		Jar build = b.build();
		assertOk(b);
		build.calcChecksums(new String[] { "MD5", "SHA1" });
		Manifest m = build.getManifest();
		m.write(System.out);

		assertNotNull(m.getAttributes("org/osgi/service/event/EventAdmin.class").getValue("MD5"));
		assertNotNull(m.getAttributes("org/osgi/service/event/EventAdmin.class").getValue("SHA1"));
		assertEquals(
				m.getAttributes("org/osgi/service/event/EventAdmin.class").getValue("MD5-Digest"),
				m.getAttributes("org/osgi/service/event/EventAdmin.class").getValue("MD5"));

	}

	/**
	 * Check of the use of x- directives are not skipped. bnd allows x-
	 * directives in the import/export clauses but strips other ones.
	 * 
	 * @throws Exception
	 */
	public void testXDirectives() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.setProperty("Export-Package", "org.osgi.framework;x-foo:=true;bar:=false");
		Jar jar = b.build();
		Manifest m = jar.getManifest();
		String s = m.getMainAttributes().getValue("Export-Package");
		assertTrue(s.contains("x-foo:"));
		assertEquals(1, b.getWarnings().size());
		assertTrue(b.getWarnings().get(0).contains("bar:"));
	}

	/**
	 * Check of SNAPSHOT is replaced with the -snapshot instr
	 * 
	 * @throws Exception
	 */
	public void testSnapshot() throws Exception {
		Builder b = new Builder();
		b.setProperty("-resourceonly", "true");
		b.setProperty("-snapshot", "TIMESTAMP");
		b.setProperty("Bundle-Version", "1.0-SNAPSHOT");
		Jar jar = b.build();
		Manifest m = jar.getManifest();
		assertEquals("1.0.0.TIMESTAMP", m.getMainAttributes().getValue("Bundle-Version"));
	}

	/**
	 * Check if do not copy works on files
	 */

	public void testDoNotCopy() throws Exception {
		Builder b = new Builder();
		b.setProperty("-resourceonly", "true");
		b.setProperty("-donotcopy", ".*\\.jar|\\..*");
		b.setProperty("Include-Resource", "jar");
		b.build();
		System.out.println(Processor.join(b.getErrors(), "\n"));
		System.out.println(Processor.join(b.getWarnings(), "\n"));
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Set<String> names = b.getJar().getResources().keySet();
		assertEquals(6, names.size());
		assertTrue(names.contains("AnnotationWithJSR14.jclass"));
		assertTrue(names.contains("mandatorynoversion.bnd"));
		assertTrue(names.contains("mina.bar"));
		assertTrue(names.contains("minax.bnd"));
		assertTrue(names.contains("rox.bnd"));
		assertTrue(names.contains("WithAnnotations.jclass"));
	}

	/**
	 * Check if do not copy works on files
	 */

	public void testDoNotCopyDS() throws Exception {
		Builder b = new Builder();
		b.setProperty("-resourceonly", "true");
		b.setProperty("Include-Resource", "jar/");
		b.build();
		System.out.println(Processor.join(b.getErrors(), "\n"));
		System.out.println(Processor.join(b.getWarnings(), "\n"));
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Set<String> names = b.getJar().getResources().keySet();
		assertFalse(names.contains(".DS_Store"));
	}

	/**
	 * No error is generated when a file is not found.
	 * 
	 */

	public void testFileNotFound() throws Exception {
		Builder b = new Builder();
		b.setPedantic(true);
		b.setProperty("-classpath", "xyz.jar");
		b.setProperty("Include-Resource", "lib=lib, jar/osgi.jar");
		b.setProperty("-resourceonly", "true");
		b.build();
		System.out.println(Processor.join(b.getErrors(), "\n"));
		System.out.println("xx");
		System.out.println(Processor.join(b.getWarnings(), "\n"));
		assertEquals(1, b.getErrors().size());
		assertEquals(1, b.getWarnings().size());
	}

	/**
	 * bnd seems to pick the wrong version if a packageinfo is available
	 * multiple times.
	 * 
	 * @throws Exception
	 */

	public void testMultiplePackageInfo() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.addClasspath(new File("jar/osgi.core.jar"));
		b.setProperty(Constants.PRIVATE_PACKAGE, "org.osgi.service.packageadmin");
		b.build();
		String version = b.getImports().get("org.osgi.framework").get(Constants.VERSION_ATTRIBUTE);
		assertEquals("[1.3,2)", version);
	}

	/**
	 * Test the from: directive on expanding packages.
	 */
	public void testFromOSGiDirective() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.addClasspath(new File("jar/org.eclipse.osgi-3.5.0.jar"));
		b.setProperty("Export-Package", "org.osgi.framework;from:=osgi");
		b.build();
		System.out.println(Processor.join(b.getErrors(), "\n"));
		System.out.println(Processor.join(b.getWarnings(), "\n"));
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		assertEquals("1.3", b.getExports().get("org.osgi.framework").get("version"));
	}

	public void testFromEclipseDirective() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.addClasspath(new File("jar/org.eclipse.osgi-3.5.0.jar"));
		b.setProperty("Export-Package", "org.osgi.framework;from:=org.eclipse.osgi-3.5.0");
		b.build();
		System.out.println(Processor.join(b.getErrors(), "\n"));
		System.out.println(Processor.join(b.getWarnings(), "\n"));
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		assertEquals("1.3", b.getExports().get("org.osgi.framework").get("version"));
	}

	/**
	 * Test the provide package
	 */
	public void testProvidedVersion() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.addClasspath(new File("bin"));
		b.setProperty(Constants.EXPORT_PACKAGE, "org.osgi.service.event;provide:=true");
		b.setProperty("Private-Package", "test.refer");
		Jar jar = b.build();
		String ip = jar.getManifest().getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
		Map<String, Map<String, String>> map = Processor.parseHeader(ip, null);
		assertEquals("[1.0,1.1)", map.get("org.osgi.service.event").get("version"));

	}

	public void testUnProvidedVersion() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.addClasspath(new File("bin"));
		b.setProperty(Constants.EXPORT_PACKAGE, "org.osgi.service.event;provide:=false");
		b.setProperty("Private-Package", "test.refer");
		Jar jar = b.build();
		String ip = jar.getManifest().getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
		Map<String, Map<String, String>> map = Processor.parseHeader(ip, null);
		assertEquals("[1.0,2)", map.get("org.osgi.service.event").get("version"));
	}

	/**
	 * Complaint that exported versions were not picked up from external bundle.
	 */

	public void testExportedVersionsNotPickedUp() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/jsr311-api-1.1.1.jar"));
		b.setProperty("Export-Package", "javax.ws.rs.core");
		Jar jar = b.build();
		String ip = jar.getManifest().getMainAttributes().getValue(Constants.EXPORT_PACKAGE);
		Map<String, Map<String, String>> map = Processor.parseHeader(ip, null);
		assertEquals("1.1.1", map.get("javax.ws.rs.core").get("version"));
	}

	/**
	 * Test where the version comes from: Manifest or packageinfo
	 * 
	 * @throws Exception
	 */
	public void testExportVersionSource() throws Exception {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("Export-Package",
				"org.osgi.service.event;version=100");

		// Remove packageinfo
		Jar manifestOnly = new Jar(new File("jar/osgi.jar"));
		manifestOnly.remove("org/osgi/service/event/packageinfo");
		manifestOnly.setManifest(manifest);

		// Remove manifest
		Jar packageInfoOnly = new Jar(new File("jar/osgi.jar"));
		packageInfoOnly.setManifest(new Manifest());

		Jar both = new Jar(new File("jar/osgi.jar"));
		both.setManifest(manifest);

		// Only version in manifest
		Builder bms = new Builder();
		bms.addClasspath(manifestOnly);
		bms.setProperty("Export-Package", "org.osgi.service.event");
		bms.build();
		String s = bms.getExports().get("org.osgi.service.event").get("version");
		assertEquals("100", s);

		// Only version in packageinfo
		Builder bpinfos = new Builder();
		bpinfos.addClasspath(packageInfoOnly);
		bpinfos.setProperty("Export-Package", "org.osgi.service.event");
		bpinfos.build();
		s = bpinfos.getExports().get("org.osgi.service.event").get("version");
		assertEquals("1.0.1", s);

		// // Version in manifest + packageinfo
		// Builder bboth = new Builder();
		// bboth.addClasspath(both);
		// bboth.setProperty("Export-Package", "org.osgi.service.event");
		// bboth.build();
		// s = bboth.getExports().get("org.osgi.service.event").get("version");
		// assertEquals("100", s);
	}

	/**
	 * Test where the version comes from: Manifest or packageinfo
	 * 
	 * @throws Exception
	 */
	public void testImportVersionSource() throws Exception {
		Jar ms = new Jar("manifestsource");
		Jar pinfos = new Jar("packageinfosource");
		Jar both = new Jar("both");

		Manifest mms = new Manifest();
		mms.getMainAttributes().putValue("Export-Package", "org.osgi.service.event; version=100");
		ms.setManifest(mms);

		pinfos.putResource("org/osgi/service/event/packageinfo",
				new EmbeddedResource("version 99".getBytes(), 0));

		Manifest mboth = new Manifest();
		mboth.getMainAttributes().putValue("Export-Package", "org.osgi.service.event; version=100");
		both.putResource("org/osgi/service/event/packageinfo",
				new EmbeddedResource("version 99".getBytes(), 0));
		both.setManifest(mboth);

		// Only version in manifest
		Builder bms = new Builder();
		bms.addClasspath(ms);
		bms.setProperty("Import-Package", "org.osgi.service.event");
		bms.build();
		String s = bms.getImports().get("org.osgi.service.event").get("version");
		assertEquals("[100.0,101)", s);

		// Only version in packageinfo
		Builder bpinfos = new Builder();
		bpinfos.addClasspath(pinfos);
		bpinfos.setProperty("Import-Package", "org.osgi.service.event");
		bpinfos.build();
		s = bpinfos.getImports().get("org.osgi.service.event").get("version");
		assertEquals("[99.0,100)", s);

		// Version in manifest + packageinfo
		Builder bboth = new Builder();
		bboth.addClasspath(both);
		bboth.setProperty("Import-Package", "org.osgi.service.event");
		bboth.build();
		s = bboth.getImports().get("org.osgi.service.event").get("version");
		assertEquals("[100.0,101)", s);

	}

	public void testNoImportDirective() throws Exception {
		Builder b = new Builder();
		b.setProperty("Export-Package",
				"org.osgi.util.measurement, org.osgi.service.http;-noimport:=true");
		b.setProperty("Private-Package", "org.osgi.framework, test.refer");
		b.addClasspath(new File("jar/osgi.jar"));
		b.addClasspath(new File("bin"));
		Jar jar = b.build();
		Manifest m = jar.getManifest();
		String imports = m.getMainAttributes().getValue("Import-Package");
		assertTrue(imports.contains("org.osgi.util.measurement")); // referred
																	// to but no
																	// private
																	// references
																	// (does not
																	// use fw).
		assertFalse(imports.contains("org.osgi.service.http")); // referred to
																// but no
																// private
																// references
																// (does not use
																// fw).

	}

	public void testNoImportDirective2() throws Exception {
		Builder b = new Builder();
		b.setProperty("Export-Package",
				"org.osgi.util.measurement;-noimport:=true, org.osgi.service.http");
		b.setProperty("Private-Package", "org.osgi.framework, test.refer");
		b.addClasspath(new File("jar/osgi.jar"));
		b.addClasspath(new File("bin"));
		Jar jar = b.build();
		Manifest m = jar.getManifest();
		String imports = m.getMainAttributes().getValue("Import-Package");
		assertFalse(imports.contains("org.osgi.util.measurement")); // referred
																	// to but no
																	// private
																	// references
																	// (does not
																	// use fw).
		assertTrue(imports.contains("org.osgi.service.http")); // referred to
																// but no
																// private
																// references
																// (does not use
																// fw).

	}

	public void testAutoNoImport() throws Exception {
		Builder b = new Builder();
		b.setProperty(
				"Export-Package",
				"org.osgi.service.event, org.osgi.service.packageadmin, org.osgi.util.measurement, org.osgi.service.http;-noimport:=true");
		b.setProperty("Private-Package", "org.osgi.framework, test.refer");
		b.addClasspath(new File("jar/osgi.jar"));
		b.addClasspath(new File("bin"));
		Jar jar = b.build();
		Manifest m = jar.getManifest();
		String imports = m.getMainAttributes().getValue("Import-Package");
		assertFalse(imports.contains("org.osgi.service.packageadmin")); // no
																		// internal
																		// references
		assertFalse(imports.contains("org.osgi.util.event")); // refers to
																// private
																// framework
		assertTrue(imports.contains("org.osgi.util.measurement")); // referred
																	// to but no
																	// private
																	// references
																	// (does not
																	// use fw).
		assertFalse(imports.contains("org.osgi.service.http")); // referred to
																// but no
																// private
																// references
																// (does not use
																// fw).
	}

	public void testWab() throws Exception {
		Builder b = new Builder();
		b.setProperty("-wablib", "jar/asm.jar, jar/easymock.jar");
		b.setProperty("-wab", "jar/osgi.jar");
		b.setProperty("-includeresource", "OSGI-INF/xml/x.xml;literal=\"text\"");
		b.setProperty("Private-Package", "org.osgi.framework");
		b.addClasspath(new File("jar/osgi.jar"));
		Jar jar = b.build();
		diagnose(b);

		Manifest m = jar.getManifest();
		assertNotNull(m);
		assertEquals("WEB-INF/classes,WEB-INF/lib/asm.jar,WEB-INF/lib/easymock.jar", m
				.getMainAttributes().getValue("Bundle-ClassPath"));
		assertNotNull(jar.getResource("WEB-INF/lib/asm.jar"));
		assertNotNull(jar.getResource("WEB-INF/classes/org/osgi/framework/BundleContext.class"));
		assertNotNull(jar.getResource("osgi.jar"));
		assertNotNull(jar.getResource("OSGI-INF/xml/x.xml"));
	}

	private void diagnose(Builder b) {
		System.out.println(Processor.join(b.getErrors(), "\n"));
		System.out.println(Processor.join(b.getWarnings(), "\n"));
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
	}

	public void testRemoveHeaders() throws Exception {
		Builder b = new Builder();
		b.setProperty("Private-Package", "org.osgi.framework");
		b.setProperty("T1", "1");
		b.setProperty("T2", "1");
		b.setProperty("T1_2", "1");
		b.setProperty("-removeheaders", "!T1_2,T1*");
		b.addClasspath(new File("jar/osgi.jar"));
		Jar jar = b.build();

		diagnose(b);

		Manifest m = jar.getManifest();
		assertNotNull(m);
		assertEquals("1", m.getMainAttributes().getValue("T2"));
		assertEquals("1", m.getMainAttributes().getValue("T1_2"));
		assertEquals(null, m.getMainAttributes().getValue("T1"));

	}

	public void testNoManifest() throws Exception {
		Builder b = new Builder();
		b.setProperty("-nomanifest", "true");
		b.setProperty("Include-Resource", "WEB-INF/classes=@jar/asm.jar");
		Jar jar = b.build();
		diagnose(b);

		File f = new File("tmp.jar");
		f.deleteOnExit();
		jar.write(f);

		JarInputStream jin = new JarInputStream(new FileInputStream(f));
		Manifest m = jin.getManifest();
		assertNull(m);

	}

	public void testClassesonNoBCP() throws Exception {
		Builder b = new Builder();
		b.setProperty("-resourceonly", "true");
		b.setProperty("Include-Resource", "WEB-INF/classes=@jar/asm.jar");
		b.setProperty("-nomanifest", "true");
		b.build();
		diagnose(b);
	}

	public void testClassesonBCP() throws Exception {
		Builder b = new Builder();
		b.setProperty("-resourceonly", "true");
		b.setProperty("Include-Resource", "WEB-INF/classes=@jar/asm.jar");
		b.setProperty("Bundle-ClassPath", "WEB-INF/classes");
		b.build();
		diagnose(b);
	}

	public void testInScopeExport() throws Exception {
		Builder b = new Builder();
		b.setProperty("Export-Package", "aQute.bnd.*");
		b.addClasspath(new File("bin"));
		List<File> project = Arrays.asList(b.getFile("bin/aQute/bnd/build/Project.class"));
		assertTrue(b.isInScope(project));
		List<File> nonexistent = Arrays.asList(b.getFile("bin/aQute/bnd/build/Abc.xyz"));
		assertTrue(b.isInScope(nonexistent));
		List<File> outside = Arrays.asList(b.getFile("bin/test/AnalyzerTest.class"));
		assertFalse(b.isInScope(outside));
	}

	public void testInScopePrivate() throws Exception {
		Builder b = new Builder();
		b.setProperty("Private-Package", "!aQute.bnd.build,aQute.bnd.*");
		b.addClasspath(new File("bin"));
		List<File> project = Arrays.asList(b.getFile("bin/aQute/bnd/build/Project.class"));
		assertFalse(b.isInScope(project));
		List<File> nonexistent = Arrays.asList(b.getFile("bin/aQute/bnd/acb/Def.xyz"));
		assertTrue(b.isInScope(nonexistent));
		List<File> outside = Arrays.asList(b.getFile("bin/test/AnalyzerTest.class"));
		assertFalse(b.isInScope(outside));
	}

	public void testExtra() throws Exception {
		Builder b = new Builder();
		b.setProperty("Include-Resource",
				"jar/osgi.jar;extra=itworks, www/xyz.jar=jar/osgi.jar;extra='italsoworks'");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		Resource r = jar.getResource("osgi.jar");
		assertNotNull(r);
		assertEquals("itworks", r.getExtra());
		Resource r2 = jar.getResource("www/xyz.jar");
		assertNotNull(r2);
		assertEquals("italsoworks", r2.getExtra());
	}

	/**
	 * Create a jar with something in the default package.
	 */

	public void testIncludeDefaultPackage() {
		Builder b = new Builder();
		b.setProperty("Private-Package", ".");
	}

	/**
	 * Got a split package warning during verify when private overlaps with
	 * export
	 */
	public void testSplitWhenPrivateOverlapsExport() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.setProperty("Private-Package", "org.osgi.service.*");
		b.setProperty("Export-Package", "org.osgi.service.event");
		b.build();
		checkok(b);
	}


	/**
	 * This test checks if
	 * 
	 * @throws Exception
	 */

	public void testMacroBasedExpansion() throws Exception {
		Processor proc = new Processor();

		Builder builder = new Builder(proc);
		builder.setProperty("Export-Package", "${spec.packages}");
		proc.setProperty("spec.packages", "${core.packages}, ${cmpn.packages}, ${mobile.packages}");
		proc.setProperty("core.specs",
				"org.osgi.service.packageadmin, org.osgi.service.permissionadmin");
		proc.setProperty("core.packages", "${replace;${core.specs};.+;$0.*}");
		proc.setProperty("cmpn.specs", "org.osgi.service.event, org.osgi.service.cu");
		proc.setProperty("cmpn.packages", "${replace;${cmpn.specs};.+;$0.*}");
		proc.setProperty("mobile.specs",
				"org.osgi.service.wireadmin, org.osgi.service.log, org.osgi.service.cu");
		proc.setProperty("mobile.packages", "${replace;${mobile.specs};.+;$0.*}");
		builder.addClasspath(new File("jar/osgi.jar"));

		builder.build();
		checkok(builder);

		Map<String, Map<String, String>> h = OSGiHeader.parseHeader( builder.getJar().getManifest().getMainAttributes().getValue(Constants.EXPORT_PACKAGE));
		SortedList<String> l = new SortedList<String>(h.keySet());
		assertEquals("[org.osgi.service.cu, org.osgi.service.cu.admin, org.osgi.service.cu.admin.spi, org.osgi.service.cu.admin.spi~, org.osgi.service.cu.admin~, org.osgi.service.cu.diag, org.osgi.service.cu.diag~, org.osgi.service.cu~, org.osgi.service.event, org.osgi.service.log, org.osgi.service.packageadmin, org.osgi.service.permissionadmin, org.osgi.service.wireadmin]",
				l.toString());
	}

	/**
	 * Make resolution dependent on the fact that a package is on the classpath
	 * or not
	 */

	public void testConditionalResolution() throws Exception {
		Builder b = new Builder();
		b.setProperty("res", "${if;${exporters;${@package}};mandatory;optional}");
		b.setProperty("Import-Package", "*;resolution:=\\${res}");
		b.setProperty("Export-Package", "org.osgi.service.io, org.osgi.service.log");
		b.addClasspath(new File("jar/osgi.jar"));
		b.build();

		Map<String, String> ioimports = b.getImports().get("javax.microedition.io");
		Map<String, String> fwimports = b.getImports().get("org.osgi.framework");

		assertNotNull(ioimports);
		assertNotNull(fwimports);
		assertTrue(ioimports.containsKey("resolution:"));
		assertTrue(fwimports.containsKey("resolution:"));
		assertEquals("optional", ioimports.get("resolution:"));
		assertEquals("mandatory", fwimports.get("resolution:"));
	}

	/**
	 * Test private imports. We first build a jar with a import:=private packge.
	 * Then place it
	 * 
	 * @throws Exception
	 */

	// public void testPrivateImport() throws Exception {
	// Builder b = new Builder();
	// b.addClasspath(new File("jar/osgi.jar"));
	// b
	// .setProperty("Export-Package",
	// "org.osgi.service.cm;-import:=private,
	// org.osgi.framework;-import:=private");
	// Jar osgi = b.build();
	// assertTrue(osgi.hasDirectory("org/osgi/framework"));
	// assertTrue(osgi.hasDirectory("org/osgi/service/cm"));
	// System.out.println("export " +
	// b.getExports().get("org.osgi.service.cm"));
	// assertTrue(b.getExports().get("org.osgi.service.cm").containsKey(Constants.IMPORT_DIRECTIVE));
	// assertTrue(b.getExports().get("org.osgi.framework").containsKey(Constants.IMPORT_DIRECTIVE));
	//
	// Builder c = new Builder();
	// c.addClasspath(osgi);
	// c.addClasspath(new File("bin"));
	// c.setProperty("Private-Package", "test.privateimport");
	// Jar pi = c.build();
	//
	// assertTrue(pi.getDirectories().containsKey("org/osgi/framework"));
	// assertTrue(pi.getDirectories().containsKey("org/osgi/service/cm"));
	// assertEquals("no error", 0, b.getErrors().size());
	// assertEquals("no error", 0, c.getErrors().size());
	// assertEquals("no warning", 0, b.getWarnings().size());
	// assertEquals("no warning", 0, c.getWarnings().size());
	// }
	public void testClassnames() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.addClasspath(new File("jar/ds.jar"));
		b.addClasspath(new File("jar/ifc112.jar"));
		b.setProperty("Export-Package", "*");
		b.setProperty("C1", "${classes;implementing;org.osgi.service.component.*}");
		b.setProperty("C2", "${classes;extending;org.xml.sax.helpers.*}");
		b.setProperty("C3", "${classes;importing;org.xml.sax}");
		b.setProperty("C3", "${classes;importing;org.xml.sax}");
		b.setProperty("C4", "${classes;named;*Parser*}");
		b.setProperty("C5", "${classes;named;*Parser*;version;45.*}");
		Jar jar = b.build();
		Manifest m = jar.getManifest();
		m.write(System.out);
		Attributes main = m.getMainAttributes();
		assertList(
				asl("org.eclipse.equinox.ds.service.ComponentContextImpl,org.eclipse.equinox.ds.service.ComponentFactoryImpl,org.eclipse.equinox.ds.service.ComponentInstanceImpl"),
				asl(main.getValue("C1")));
		assertList(asl("org.eclipse.equinox.ds.parser.ElementHandler, "
				+ "org.eclipse.equinox.ds.parser.IgnoredElement,"
				+ "org.eclipse.equinox.ds.parser.ImplementationElement,"
				+ "org.eclipse.equinox.ds.parser.ParserHandler, "
				+ "org.eclipse.equinox.ds.parser.PropertiesElement,"
				+ "org.eclipse.equinox.ds.parser.PropertyElement, "
				+ "org.eclipse.equinox.ds.parser.ProvideElement, "
				+ "org.eclipse.equinox.ds.parser.ReferenceElement, "
				+ "org.eclipse.equinox.ds.parser.ServiceElement,"
				+ "org.eclipse.equinox.ds.parser.ComponentElement"), asl(main.getValue("C2")));
		assertList(
				asl("org.eclipse.equinox.ds.parser.ComponentElement,org.eclipse.equinox.ds.parser.ElementHandler,org.eclipse.equinox.ds.parser.IgnoredElement,org.eclipse.equinox.ds.parser.ImplementationElement,org.eclipse.equinox.ds.parser.Parser,org.eclipse.equinox.ds.parser.ParserHandler,org.eclipse.equinox.ds.parser.PropertiesElement,org.eclipse.equinox.ds.parser.PropertyElement,org.eclipse.equinox.ds.parser.ProvideElement,org.eclipse.equinox.ds.parser.ReferenceElement,org.eclipse.equinox.ds.parser.ServiceElement"),
				asl(main.getValue("C3")));
		assertList(
				asl("org.eclipse.equinox.ds.parser.XMLParserNotAvailableException,org.eclipse.equinox.ds.parser.Parser,org.eclipse.equinox.ds.parser.ParserHandler,netscape.application.HTMLParser,org.eclipse.equinox.ds.parser.ParserConstants,org.osgi.util.xml.XMLParserActivator"),
				asl(main.getValue("C4")));
		assertEquals("netscape.application.HTMLParser", main.getValue("C5"));
	}

	void assertList(Collection<String> a, Collection<String> b) {
		List<String> onlyInA = new ArrayList<String>();
		onlyInA.addAll(a);
		onlyInA.removeAll(b);

		List<String> onlyInB = new ArrayList<String>();
		onlyInB.addAll(b);
		onlyInB.removeAll(a);

		if (onlyInA.isEmpty() && onlyInB.isEmpty())
			return;

		fail("Lists are not equal, only in A: " + onlyInA + ",\n   and only in B: " + onlyInB);
	}

	Collection<String> asl(String s) {
		return new TreeSet<String>(Processor.split(s));
	}

	public void testImportMicroNotTruncated() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.setProperty("Import-Package", "org.osgi.service.event;version=${@}");
		b.build();
		String s = b.getImports().get("org.osgi.service.event").get("version");
		assertEquals("1.0.1", s);
	}

	public void testImportMicroTruncated() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.setProperty("Import-Package", "org.osgi.service.event");
		b.build();
		String s = b.getImports().get("org.osgi.service.event").get("version");
		assertEquals("[1.0,2)", s);
	}

	/*
	 * Bnd must expand the bnd.info file in a package.
	 */
	public void testBndInfo() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test");
		b.setProperty("a", "aaa");
		Jar jar = b.build();

		Resource r = jar.getResource("test/bnd.info");
		Properties bndinfo = new Properties();
		InputStream in = r.openInputStream();
		bndinfo.load(in);
		in.close();
		assertEquals("aaa", bndinfo.getProperty("a"));
		assertEquals("${b}", bndinfo.getProperty("b"));
	}

	public void testMultipleExport2() throws Exception {
		File cp[] = { new File("jar/asm.jar") };
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("Import-Package", "*");
		p.setProperty("Export-Package",
				"org.objectweb.asm;version=1.1, org.objectweb.asm~;version=1.2, org.objectweb.asm~~;version=1.3");
		bmaker.setProperties(p);
		bmaker.setClasspath(cp);
		Jar jar = bmaker.build();
		checkok(bmaker);
		jar.getManifest().write(System.out);
		Manifest m = jar.getManifest();
		String ip = m.getMainAttributes().getValue("Export-Package");
		assertTrue(ip.indexOf("org.objectweb.asm;version=\"1.1\"") >= 0);
		assertTrue(ip.indexOf("org.objectweb.asm;version=\"1.2\"") >= 0);
		assertTrue(ip.indexOf("org.objectweb.asm;version=\"1.3\"") >= 0);
	}

	public void testBsnAssignmentNoFile() throws Exception {
		Properties p = new Properties();
		p.setProperty("Private-Package", "org.objectweb.asm");
		Attributes m = setup(p, null).getMainAttributes();

		// We use properties so the default BSN is then the project name
		// because that is the base directory
		assertEquals(m.getValue("Bundle-SymbolicName"), "biz.aQute.bndlib");

		// The file name for the properties is not bnd.bnd, so the
		// name of the properties file is the default bsn
		m = setup(null, new File("src/test/com.acme/defaultbsn.bnd")).getMainAttributes();
		assertEquals("com.acme.defaultbsn", m.getValue("Bundle-SymbolicName"));

		// If the file is called bnd.bnd, then we take the parent directory
		m = setup(null, new File("src/test/com.acme/bnd.bnd")).getMainAttributes();
		assertEquals("com.acme", m.getValue("Bundle-SymbolicName"));

		// If the file is called bnd.bnd, then we take the parent directory
		m = setup(null, new File("src/test/com.acme/setsbsn.bnd")).getMainAttributes();
		assertEquals("is.a.set.bsn", m.getValue("Bundle-SymbolicName"));

		// This sets the bsn, se we should see it back
		p.setProperty("Bundle-SymbolicName", "this.is.my.test");
		m = setup(p, null).getMainAttributes();
		assertEquals(m.getValue("Bundle-SymbolicName"), "this.is.my.test");
	}

	public Manifest setup(Properties p, File f) throws Exception {
		File cp[] = { new File("jar/asm.jar") };
		Builder bmaker = new Builder();
		if (f != null)
			bmaker.setProperties(f);
		else
			bmaker.setProperties(p);
		bmaker.setClasspath(cp);
		Jar jar = bmaker.build();
		System.out.println(jar.getResources());
		// System.out.println(bmaker.getExports());
		System.out.println("Warnings: " + bmaker.getWarnings());
		System.out.println("Errors  : " + bmaker.getErrors());
		jar.getManifest().write(System.out);
		assertEquals(0, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
		Manifest m = jar.getManifest();
		return m;
	}

	public void testDuplicateExport() throws Exception {
		File cp[] = { new File("jar/asm.jar") };
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("Import-Package", "*");
		p.setProperty("Export-Package", "org.*;version=1.2,org.objectweb.asm~;version=1.3");
		bmaker.setProperties(p);
		bmaker.setClasspath(cp);
		Jar jar = bmaker.build();
		System.out.println(jar.getResources());
		// System.out.println(bmaker.getExports());
		System.out.println("Warnings: " + bmaker.getWarnings());
		System.out.println("Errors  : " + bmaker.getErrors());
		jar.getManifest().write(System.out);
		assertEquals(0, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
		Manifest m = jar.getManifest();
		String ip = m.getMainAttributes().getValue("Export-Package");
		assertTrue(ip.indexOf("org.objectweb.asm;version=\"1.2\"") >= 0);
	}

	public void testNoExport() throws Exception {
		File cp[] = { new File("jar/asm.jar") };
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("Import-Package", "*");
		p.setProperty("Export-Package", "org.*'");
		bmaker.setProperties(p);
		bmaker.setClasspath(cp);
		Jar jar = bmaker.build();
		System.out.println(jar.getResources());
		// System.out.println(bmaker.getExports());
		System.out.println("Warnings: " + bmaker.getWarnings());
		System.out.println("Errors  : " + bmaker.getErrors());
		jar.getManifest().write(System.out);
		assertEquals(0, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
		Manifest m = jar.getManifest();
		String ip = m.getMainAttributes().getValue("Export-Package");
		assertTrue(ip.indexOf("org.objectweb.asm") >= 0);
	}

	public void testHardcodedImport() throws Exception {
		File cp[] = { new File("jar/asm.jar") };
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("Import-Package", "whatever,*");
		p.setProperty("Export-Package", "org.*");
		bmaker.setProperties(p);
		bmaker.setClasspath(cp);
		Jar jar = bmaker.build();
		System.out.println(jar.getResources());
		// System.out.println(bmaker.getExports());
		System.out.println("Warnings: " + bmaker.getWarnings());
		System.out.println("Errors  : " + bmaker.getErrors());
		jar.getManifest().write(System.out);
		assertEquals(0, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
		Manifest m = jar.getManifest();
		String ip = m.getMainAttributes().getValue("Import-Package");
		assertTrue(ip.indexOf("whatever") >= 0);
	}

	public void testCopyDirectory() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-resourceonly", "true");
		p.setProperty("Include-Resource", "bnd=bnd");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		Map<String, Resource> map = jar.getDirectories().get("bnd");
		assertNotNull(map);
		assertEquals(2, map.size());
		assertEquals(0, bmaker.getErrors().size());
		assertEquals(0, bmaker.getWarnings().size());
	}

	/**
	 * There is an error that gives a split package when you export a package
	 * that is also private I think.
	 * 
	 * @throws Exception
	 */
	public void testSplitOnExportAndPrivate() throws Exception {
		File cp[] = { new File("jar/asm.jar") };
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("Export-Package", "org.objectweb.asm.signature");
		p.setProperty("Private-Package", "org.objectweb.asm");
		bmaker.setProperties(p);
		bmaker.setClasspath(cp);
		bmaker.build();
		System.out.println("Warnings: " + bmaker.getWarnings());
		System.out.println("Errors  : " + bmaker.getErrors());
		assertEquals(0, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
	}

	public void testConduit() throws Exception {
		Properties p = new Properties();
		p.setProperty("-conduit", "jar/asm.jar");
		Builder b = new Builder();
		b.setProperties(p);
		Jar jars[] = b.builds();
		assertNotNull(jars);
		assertEquals(1, jars.length);
		assertEquals("ASM",
				jars[0].getManifest().getMainAttributes().getValue("Implementation-Title"));
	}

	/**
	 * Export a package that was loaded with resources
	 * 
	 * @throws Exception
	 */
	public void testExportSyntheticPackage() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-resourceonly", "true");
		p.setProperty("Include-Resource", "resources=jar");
		p.setProperty("-exportcontents", "resources");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		System.out.println(jar.getResources());
		// System.out.println(bmaker.getExports());
		System.out.println("Errors  : " + bmaker.getErrors());
		System.out.println("Warnings  : " + bmaker.getWarnings());
		assertEquals("Warnings: ", 0, bmaker.getWarnings().size());
		assertEquals("Errors  : ", 0, bmaker.getErrors().size());
		Manifest manifest = jar.getManifest();
		String header = manifest.getMainAttributes().getValue("Export-Package");
		System.out.println(header);
		assertTrue(header.indexOf("resources") >= 0);
	}

	/**
	 * Exporting packages in META-INF
	 * 
	 * @throws Exception
	 */
	public void testMETAINF() throws Exception {
		File cp[] = { new File("src"), new File("jar/asm.jar") };
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("Include-Resource", "META-INF/xyz/asm.jar=jar/asm.jar");
		p.setProperty("Export-Package", "META-INF/xyz, org.*");
		bmaker.setProperties(p);
		bmaker.setClasspath(cp);
		Jar jar = bmaker.build();
		System.out.println(jar.getResources());
		// System.out.println(bmaker.getExports());
		System.out.println("Warnings: " + bmaker.getWarnings());
		System.out.println("Errors  : " + bmaker.getErrors());
		jar.getManifest().write(System.out);
		Manifest manifest = jar.getManifest();
		String header = manifest.getMainAttributes().getValue("Export-Package");
		assertTrue(header.indexOf("META-INF") > 0);
	}

	/**
	 * Bnd cleans up versions if they do not follow the OSGi rule. Check a
	 * number of those versions.
	 * 
	 * @throws Exception
	 */
	public void testVersionCleanup() throws Exception {
		assertVersion("000001.0003.00000-SNAPSHOT", "1.3.0.SNAPSHOT");
		assertVersion("000000.0000.00000-SNAPSHOT", "0.0.0.SNAPSHOT");
		assertVersion("0-SNAPSHOT", "0.0.0.SNAPSHOT");
		assertVersion("1.3.0.0-0-01-0-SNAPSHOT", "1.3.0.0-0-01-0-SNAPSHOT");
		assertVersion("1.3.0.0-0-01-0", "1.3.0.0-0-01-0");
		assertVersion("0.9.0.1.2.3.4.5-incubator-SNAPSHOT", "0.9.0.incubator-SNAPSHOT");
		assertVersion("0.4aug123", "0.0.0.4aug123");
		assertVersion("0.9.4aug123", "0.9.0.4aug123");
		assertVersion("0.9.0.4aug123", "0.9.0.4aug123");

		assertVersion("1.2.3", "1.2.3");
		assertVersion("1.2.3-123", "1.2.3.123");
		assertVersion("1.2.3.123", "1.2.3.123");
		assertVersion("1.2.3.123x", "1.2.3.123x");
		assertVersion("1.123x", "1.0.0.123x");

		assertVersion("0.9.0.4.3.2.1.0.4aug123", "0.9.0.4aug123");
		assertVersion("0.9.0.4aug123", "0.9.0.4aug123");

		assertVersion("0.9.0.4.3.4.5.6.6", "0.9.0.6");

		assertVersion("0.9.0-incubator-SNAPSHOT", "0.9.0.incubator-SNAPSHOT");
		assertVersion("1.2.3.x", "1.2.3.x");
		assertVersion("1.2.3", "1.2.3");
		assertVersion("1.2", "1.2");
		assertVersion("1", "1");
		assertVersion("1.2.x", "1.2.0.x");
		assertVersion("1.x", "1.0.0.x");
		assertVersion("1.2.3-x", "1.2.3.x");
		assertVersion("1.2:x", "1.2.0.x");
		assertVersion("1.2-snapshot", "1.2.0.snapshot");
		assertVersion("1#x", "1.0.0.x");
		assertVersion("1.&^%$#date2007/03/04", "1.0.0.date20070304");
	}

	void assertVersion(String input, String expected) {
		assertEquals(expected, Builder.cleanupVersion(input));
	}

	/**
	 * -exportcontents provides a header that is only relevant in the analyze
	 * phase, it augments the Export-Package header.
	 */

	public void testExportContents() throws Exception {
		Builder builder = new Builder();
		builder.setProperty(Analyzer.INCLUDE_RESOURCE,
				"test/activator/inherits=src/test/activator/inherits");
		builder.setProperty("-exportcontents", "*;x=true;version=1");
		builder.build();
		Manifest manifest = builder.calcManifest();
		Attributes main = manifest.getMainAttributes();
		Map<String, Map<String, String>> map = OSGiHeader.parseHeader(main
				.getValue("Export-Package"));
		Map<String, String> export = map.get("test.activator.inherits");
		assertNotNull(export);
		assertEquals("1", export.get("version"));
		assertEquals("true", export.get("x"));
	}

	/**
	 * I am having some problems with split packages in 170. I get this output:
	 * 
	 * [java] 1 : There are split packages, use directive split-package:=merge
	 * on instruction to get rid of this warning: my.package
	 * 
	 * First of all, this warning is falsely occurring. The classpath to the bnd
	 * task contains something like:
	 * 
	 * my/package/foo.class my/package/sub/bar.class
	 * 
	 * and the export is:
	 * 
	 * Export-Package: my.package*;version=${version}
	 * 
	 * so my.package and my.package.sub are being incorrectly considered as the
	 * same package by bnd.
	 * 
	 */
	public void testSplitOverlappingPackages() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		Properties p = new Properties();
		p.put("build", "xyz");
		p.put("Export-Package", "test*;version=3.1");
		b.setProperties(p);
		b.setPedantic(true);
		b.build();
		checkok(b,1,0);
	}

	/**
	 * Check Conditional package. First import a subpackage then let the
	 * subpackage import a super package. This went wrong in the OSGi build. We
	 * see such a pattern in the Spring jar. The package
	 * org.springframework.beans.factory.access refers to
	 * org.springframework.beans.factory and org.springframework.beans. The
	 */
	public void testConditionalBaseSuper() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.CONDITIONAL_PACKAGE, "test.top.*");
		b.setProperty(Constants.PRIVATE_PACKAGE, "test.top.middle.bottom");
		b.addClasspath(new File("bin"));
		Jar dot = b.build();
		System.out.println("Errors: " + b.getErrors());
		System.out.println("Warnings: " + b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		assertNotNull(dot.getResource("test/top/middle/bottom/Bottom.class"));
		assertNotNull(dot.getResource("test/top/middle/Middle.class"));
		assertNotNull(dot.getResource("test/top/Top.class"));

		assertFalse(b.getImports().containsKey("test.top"));
		assertFalse(b.getImports().containsKey("test.top.middle"));
		assertFalse(b.getImports().containsKey("test.top.middle.bottom"));
	}

	/**
	 * It looks like Conditional-Package can add the same package multiple
	 * times. So lets test this.
	 */
	public void testConditional2() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.EXPORT_PACKAGE, "org.osgi.service.log");
		base.put(Analyzer.CONDITIONAL_PACKAGE, "org.osgi.*");
		Builder analyzer = new Builder();
		analyzer.setProperties(base);
		analyzer.setClasspath(new File[] { new File("jar/osgi.jar") });
		analyzer.build();
		Jar jar = analyzer.getJar();
		assertNotNull(jar.getDirectories().get("org/osgi/framework"));
		System.out.println("Errors: " + analyzer.getErrors());
		System.out.println("Warnings: " + analyzer.getWarnings());
		assertEquals(0, analyzer.getErrors().size());
		assertEquals(0, analyzer.getWarnings().size());
	}

	/**
	 * Test the strategy: error
	 */
	public void testStrategyError() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.EXPORT_PACKAGE, "*;-split-package:=error");
		Builder analyzer = new Builder();
		analyzer.setClasspath(new File[] { new File("jar/asm.jar"), new File("jar/asm.jar") });
		analyzer.setProperties(base);
		analyzer.build();
		System.out.println("Errors: " + analyzer.getErrors());
		assertEquals(3, analyzer.getErrors().size());
		assertEquals(0, analyzer.getWarnings().size());
		assertTrue(((String) analyzer.getErrors().get(0)).indexOf("Split package") >= 0);
	}

	/**
	 * Test the strategy: default
	 */
	public void testStrategyDefault() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.EXPORT_PACKAGE, "*");
		Builder analyzer = new Builder();
		analyzer.setClasspath(new File[] { new File("jar/asm.jar"), new File("jar/asm.jar") });
		analyzer.setProperties(base);
		analyzer.build();
		System.out.println("Warnings: " + analyzer.getWarnings());
		assertEquals(0, analyzer.getErrors().size());
		assertEquals(3, analyzer.getWarnings().size());
		assertTrue(((String) analyzer.getWarnings().get(0)).indexOf("split-package") >= 0);
	}

	/**
	 * Test the strategy: merge-first
	 */
	public void testStrategyMergeFirst() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.EXPORT_PACKAGE, "*;-split-package:=merge-first");
		Builder analyzer = new Builder();
		analyzer.setClasspath(new File[] { new File("jar/asm.jar"), new File("jar/asm.jar") });
		analyzer.setProperties(base);
		analyzer.build();
		System.out.println("Errors: " + analyzer.getErrors());
		assertEquals(0, analyzer.getErrors().size());
		assertEquals(0, analyzer.getWarnings().size());
		// assertTrue( ((String)analyzer.getErrors().get(0)).contains("file does
		// not exist"));
	}

	/**
	 * Test the strategy: merge-last
	 */
	public void testStrategyMergeLast() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.EXPORT_PACKAGE, "*;-split-package:=merge-last");
		Builder analyzer = new Builder();
		analyzer.setClasspath(new File[] { new File("jar/asm.jar"), new File("jar/asm.jar") });
		analyzer.setProperties(base);
		analyzer.build();
		System.out.println("Errors: " + analyzer.getErrors());
		assertEquals(0, analyzer.getErrors().size());
		assertEquals(0, analyzer.getWarnings().size());
		// assertTrue( ((String)analyzer.getErrors().get(0)).contains("file does
		// not exist"));
	}

	/**
	 * Test Resource inclusion that do not exist
	 * 
	 * @throws Exception
	 */
	public void testResourceNotFound() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.EXPORT_PACKAGE, "*;test:=true");
		base.put(Analyzer.INCLUDE_RESOURCE, "does_not_exist");
		Builder analyzer = new Builder();
		analyzer.setClasspath(new File[] { new File("jar/asm.jar") });
		analyzer.setProperties(base);
		analyzer.build();
		System.out.println("Errors: " + analyzer.getErrors());
		assertEquals(1, analyzer.getErrors().size());
		assertTrue(((String) analyzer.getErrors().get(0)).indexOf("file does not exist") >= 0);
	}

	/**
	 * Spaces at the end of a clause cause the preprocess to fail.
	 * 
	 * @throws Exception
	 */
	public void testPreProcess() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.INCLUDE_RESOURCE, "{src/test/top.mf}     ");
		Builder analyzer = new Builder();
		analyzer.setProperties(base);
		analyzer.build();
		Jar jar = analyzer.getJar();
		assertTrue(jar.getResource("top.mf") != null);
	}

	/**
	 * Check if we can use findpath to build the Bundle-Classpath.
	 */

	public void testFindPathInBundleClasspath() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.INCLUDE_RESOURCE, "jar=jar");
		base.put(Analyzer.BUNDLE_CLASSPATH, "${findpath;jar/.{1,4}\\.jar}");
		Builder analyzer = new Builder();
		analyzer.setProperties(base);
		analyzer.build();

		Manifest manifest = analyzer.getJar().getManifest();
		String bcp = manifest.getMainAttributes().getValue("Bundle-Classpath");

		assertTrue(bcp.indexOf("ds.jar") >= 0);
		assertTrue(bcp.indexOf("asm.jar") >= 0);
		assertTrue(bcp.indexOf("bcel.jar") >= 0);
		assertTrue(bcp.indexOf("mina.jar") >= 0);
		assertTrue(bcp.indexOf("rox.jar") >= 0);
		assertTrue(bcp.indexOf("osgi.jar") >= 0);
	}

	/**
	 * Check if we export META-INF when we export the complete classpath.
	 */

	public void testVersionCleanupAll() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.EXPORT_PACKAGE, "*");
		base.put(Analyzer.BUNDLE_VERSION, "0.9.0-incubator-SNAPSHOT");
		Builder analyzer = new Builder();
		analyzer.setClasspath(new File[] { new File("jar/asm.jar") });
		analyzer.setProperties(base);
		analyzer.build();
		Manifest manifest = analyzer.getJar().getManifest();
		String version = manifest.getMainAttributes().getValue(Analyzer.BUNDLE_VERSION);
		assertEquals("0.9.0.incubator-SNAPSHOT", version);
	}

	/**
	 * We are only adding privately the core equinox ds package. We then add
	 * conditionally all packages that should belong to this as well as any OSGi
	 * interfaces.
	 * 
	 * @throws Exception
	 */
	public void testConditional() throws Exception {
		File cp[] = { new File("jar/osgi.jar"), new File("jar/ds.jar") };
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "*");
		p.put("Private-Package", "org.eclipse.equinox.ds");
		p.put("Conditional-Package", "org.eclipse.equinox.ds.*, org.osgi.service.*");
		bmaker.setProperties(p);
		bmaker.setClasspath(cp);
		bmaker.build();
		assertTrue(bmaker.getContained().containsKey("org.eclipse.equinox.ds.instance"));
		assertTrue(bmaker.getContained().containsKey("org.eclipse.equinox.ds.model"));
		assertTrue(bmaker.getContained().containsKey("org.eclipse.equinox.ds.parser"));
		assertTrue(bmaker.getContained().containsKey("org.osgi.service.cm"));
		assertTrue(bmaker.getContained().containsKey("org.osgi.service.component"));
		assertFalse(bmaker.getContained().containsKey("org.osgi.service.wireadmin"));
	}

	/**
	 * Check if we export META-INF when we export the complete classpath.
	 */

	public void testMetaInfExport() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.EXPORT_PACKAGE, "*");
		Builder analyzer = new Builder();
		analyzer.setClasspath(new File[] { new File("jar/asm.jar") });
		analyzer.setProperties(base);
		analyzer.build();
		assertFalse(analyzer.getExports().containsKey("META-INF"));
		assertTrue(analyzer.getExports().containsKey("org.objectweb.asm"));
	}

	/**
	 * Check that the activator is found.
	 * 
	 * @throws Exception
	 */
	public void testFindActivator() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Bundle-Activator", "test.activator.Activator");
		p.put("build", "xyz"); // for @Version annotation
		p.put("Private-Package", "test.*");
		bmaker.setProperties(p);
		bmaker.setClasspath(new File[] { new File("bin") });
		Jar jar = bmaker.build();
		report("testFindActivator", bmaker, jar);
		assertEquals(0, bmaker.getErrors().size());
		assertEquals(0, bmaker.getWarnings().size());
	}

	public void testImportVersionRange() throws Exception {
		assertVersionEquals("[1.1,2.0)", "[1.1,2.0)");
		assertVersionEquals("[${@},2.0)", "[1.3,2.0)");
		assertVersionEquals("[${@},${@}]", "[1.3,1.3]");
	}

	void assertVersionEquals(String input, String output) throws Exception {
		File cp[] = { new File("jar/osgi.jar") };
		Builder bmaker = new Builder();
		bmaker.setClasspath(cp);
		Properties p = new Properties();
		p.put(Analyzer.EXPORT_PACKAGE, "test.activator");
		p.put(Analyzer.IMPORT_PACKAGE, "org.osgi.framework;version=\"" + input + "\"");
		bmaker.setProperties(p);
		bmaker.build();
		Map<String, Map<String, String>> imports = bmaker.getImports();
		Map<String, String> framework = imports.get("org.osgi.framework");
		assertEquals(output, framework.get("version"));
	}

	public void testImportExportBadVersion() throws Exception {
		File cp[] = { new File("jar/asm.jar") };
		Builder bmaker = new Builder();
		bmaker.setClasspath(cp);
		Properties p = new Properties();
		p.put(Analyzer.BUNDLE_VERSION, "0.9.5-@#SNAPSHOT");
		p.put(Analyzer.EXPORT_PACKAGE, "*;version=0.9.5-@#SNAPSHOT");
		p.put(Analyzer.IMPORT_PACKAGE, "*;version=0.9.5-@#SNAPSHOT");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		assertEquals(jar.getManifest().getMainAttributes().getValue("Bundle-Version"),
				"0.9.5.SNAPSHOT");
		Map<String, String> map = bmaker.getExports().get("org.objectweb.asm");
		assertEquals(map.get("version"), "0.9.5.SNAPSHOT");
		map = bmaker.getImports().get("java.lang.reflect");
		assertEquals(map.get("version"), "0.9.5.SNAPSHOT");
	}

	/**
	 * Check if can find an activator in the bundle while using a complex bundle
	 * classpath.
	 * 
	 * @throws Exception
	 */
	public void testBundleClasspath3() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Export-Package", "test.activator;-split-package:=merge-first");
		p.put("Bundle-Activator", "test.activator.Activator");
		p.put("Import-Package", "*");
		p.put("Include-Resource", "ds.jar=jar/ds.jar");
		p.put("Bundle-ClassPath", ".,ds.jar");
		bmaker.setProperties(p);
		bmaker.setClasspath(new File[] { new File("bin"), new File("src") });
		Jar jar = bmaker.build();
		report("testBundleClasspath3", bmaker, jar);
		assertEquals(0, bmaker.getErrors().size());
		assertEquals(0, bmaker.getWarnings().size());
	}

	/**
	 * Check if can find an activator in a embedded jar while using a complex
	 * bundle classpath.
	 * 
	 * @throws Exception
	 */
	public void testBundleClasspath2() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Bundle-Activator", "org.eclipse.equinox.ds.Activator");
		p.put("Private-Package", "test.activator;-split-package:=merge-first");
		p.put("Import-Package", "*");
		p.put("Include-Resource", "ds.jar=jar/ds.jar");
		p.put("Bundle-ClassPath", ".,ds.jar");
		bmaker.setProperties(p);
		bmaker.setClasspath(new File[] { new File("bin"), new File("src") });
		Jar jar = bmaker.build();
		report("testBundleClasspath2", bmaker, jar);
		assertEquals(bmaker.getErrors().size(), 0);
		assertEquals(bmaker.getWarnings().size(), 0);
	}

	public void testBundleClasspath() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Export-Package", "test.activator;-split-package:=merge-first");
		p.put("Bundle-Activator", "test.activator.Activator");
		p.put("Import-Package", "*");
		p.put("Bundle-ClassPath", ".");
		bmaker.setProperties(p);
		bmaker.setClasspath(new File[] { new File("bin"), new File("src") });
		Jar jar = bmaker.build();
		report("testBundleClasspath", bmaker, jar);
		jar.exists("test/activator/Activator.class");
		assertEquals(bmaker.getErrors().size(), 0);
		assertEquals(bmaker.getWarnings().size(), 0);
	}

	public void testUnreferredImport() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();

		p.put("-classpath", "jar/mina.jar");
		p.put("Export-Package", "!META-INF.*,*");
		p.put("Import-Package", "org.apache.commons.collections.map,*");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		report("testUnreferredImport", bmaker, jar);

	}

	public void testIncludeResourceResourcesOnlyJar2() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();

		p.put("-classpath", "jar/ro.jar");
		p.put("Export-Package", "*");
		p.put("Import-Package", "");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		report("testIncludeResourceResourcesOnlyJar2", bmaker, jar);
		assertTrue(bmaker.getExports().containsKey("ro"));
		assertFalse(bmaker.getExports().containsKey("META-INF"));

		assertEquals(0, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
		assertEquals(3, jar.getResources().size());

	}

	public void testClasspathFileNotExist() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		File cp[] = new File[] { new File("jar/idonotexist.jar") };

		bmaker.setProperties(p);
		bmaker.setClasspath(cp);
		bmaker.build();
		assertEquals(2, bmaker.getErrors().size());
	}

	public void testExpandWithNegate() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		File cp[] = new File[] { new File("jar/asm.jar") };

		p.put("Export-Package", "!org.objectweb.asm,*");
		bmaker.setProperties(p);
		bmaker.setClasspath(cp);
		Jar jar = bmaker.build();
		assertNull(jar.getDirectories().get("org/objectweb/asm"));
		assertNotNull(jar.getDirectories().get("org/objectweb/asm/signature"));
		assertEquals(0, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
		assertEquals(3, jar.getResources().size());
	}

	public void testIncludeResourceResourcesOnlyJar() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		File cp[] = new File[] { new File("jar/ro.jar") };

		p.put("Export-Package", "*");
		p.put("Import-Package", "");
		bmaker.setProperties(p);
		bmaker.setClasspath(cp);
		Jar jar = bmaker.build();
		assertEquals(0, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
		assertEquals(3, jar.getResources().size());

	}

	public void testIncludeResourceResourcesOnly() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		File cp[] = new File[] { new File("src") };

		p.put("Import-Package", "");
		p.put("Private-Package", "test.resourcesonly");
		bmaker.setProperties(p);
		bmaker.setClasspath(cp);
		Jar jar = bmaker.build();
		assertEquals(0, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
		assertEquals(4, jar.getResources().size());

	}

	public void testIncludeResourceFromZipDefault() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Include-Resource", "@jar/easymock.jar");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();

		assertEquals(1, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
		assertEquals(59, jar.getResources().size());

	}

	public void testIncludeResourceFromZipDeep() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Include-Resource", "@jar/easymock.jar!/**");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();

		assertEquals(1, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
		assertEquals(59, jar.getResources().size());

	}

	public void testIncludeResourceFromZipOneDirectory() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "!*");
		p.put("Include-Resource", "@jar/easymock.jar!/org/easymock/**");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		assertEquals(1, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
		assertEquals(59, jar.getResources().size());
		assertNotNull(jar.getResource("org/easymock/AbstractMatcher.class"));
	}

	public void testIncludeResourceFromZipOneDirectoryOther() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "!*");
		p.put("-resourceonly", "true");
		p.put("Include-Resource", "OPT-INF/test=@jar/osgi.jar!/org/osgi/service/event/**");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		System.out.println("warnings: " + Processor.join(bmaker.getWarnings(), "\n"));
		System.out.println("errors: " + Processor.join(bmaker.getErrors(), "\n"));
		assertEquals(0, bmaker.getWarnings().size());

		assertEquals(0, bmaker.getErrors().size());
		assertEquals(7, jar.getResources().size());
		System.out.println(jar.getResources().keySet());
		assertNotNull(jar.getResource("OPT-INF/test/org/osgi/service/event/EventAdmin.class"));
	}

	public void testIncludeResourceFromZipRecurseDirectory() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "!*");
		p.put("Include-Resource", "@jar/easymock.jar!/org/easymock/**");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();

		assertEquals(1, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
		assertEquals(59, jar.getResources().size());
	}

	public void testIncludeLicenseFromZip() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "");
		p.put("Include-Resource", "@jar/osgi.jar!/LICENSE");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		assertEquals(1, jar.getResources().size());
		assertNotNull(jar.getResource("LICENSE"));
		assertEquals(1, bmaker.getWarnings().size());
		assertEquals(0, bmaker.getErrors().size());
	}

	public void testEasymock() throws Exception {
		File cp[] = { new File("jar/easymock.jar") };
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "*");
		p.put("Export-Package", "*");
		p.put("Bundle-SymbolicName", "easymock");
		p.put("Bundle-Version", "2.2");
		bmaker.setProperties(p);
		bmaker.setClasspath(cp);
		Jar jar = bmaker.build();
		checkok(bmaker);
		jar.getManifest().write(System.out);
	}

	public void testSources() throws Exception {
		Builder bmaker = new Builder();
		bmaker.addClasspath(new File("bin"));
		bmaker.setSourcepath(new File[] { new File("src") });
		bmaker.setProperty("-sources", "true");
		bmaker.setProperty("Export-Package", "test.activator");
		Jar jar = bmaker.build();
		checkok(bmaker);
		assertEquals("[test/activator/Activator.class]", new SortedList<String>(jar.getDirectories().get("test/activator").keySet()).toString());
	}

	public void testVerify() throws Exception {
		System.out.println("Erroneous bundle: tb1.jar");
		Jar jar = new Jar("test", getClass().getResourceAsStream("tb1.jar"));
		Verifier verifier = new Verifier(jar);
		verifier.verify();
		checkok(verifier);
	}

	public void report(String title, Analyzer builder, Jar jar) {
		System.out.println("Directories " + jar.getDirectories().keySet());
		System.out.println("Warnings    " + builder.getWarnings());
		System.out.println("Errors      " + builder.getErrors());
		System.out.println("Exports     " + builder.getExports());
		System.out.println("Imports     " + builder.getImports());
	}

	public void checkok(Processor p) {
		checkok(p,0,0);
	}
	public void checkok(Processor p,int errors, int warnings) {
		System.out.println("Warnings: " + p.getWarnings());
		System.out.println("Errors  : " + p.getErrors());
		assertEquals(errors, p.getErrors().size());
		assertEquals(warnings, p.getWarnings().size());
	}

}
