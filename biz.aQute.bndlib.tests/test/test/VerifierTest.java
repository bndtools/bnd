package test;

import java.io.File;
import java.util.Properties;
import java.util.jar.Manifest;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Verifier;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class VerifierTest extends TestCase {

	public void testExports() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(b.getFile("bin_test"));
			b.setExportPackage("test,\u00A0bar.foo");
			b.setImportPackage("\u2007bar.foo, \u2007bar.foo, \u202F bar.foo");
			b.build();
			assertTrue(b.check("Invalid package name: '\\\\u00A0bar.foo' in Export-Package",
				"Invalid package name: '\\\\u2007bar.foo' in Import-Package",
				"Invalid package name: '\\\\u202F bar.foo' in Import-Package"));
		}
	}

	public void testFQN() {
		assertTrue(Verifier.isFQN("a"));
		assertTrue(Verifier.isFQN("a.b"));
		assertTrue(Verifier.isFQN("_"));
		assertTrue(Verifier.isFQN("_._"));
		assertTrue(Verifier.isFQN("x\u0013o"));
		assertTrue(Verifier.isFQN("x\u0013o.asdasdasd.adasdasdads.asdasdasda"));
		assertTrue(Verifier.isFQN("_._$_.foo"));
		assertTrue(Verifier.isFQN("Foo.Bar_._$_.foo"));
		assertTrue(Verifier.isFQN("$"));

		assertFalse(Verifier.isFQN("Foo.Bar_._$_.0foo"));
		assertFalse(Verifier.isFQN("foo..bar"));
		assertFalse(Verifier.isFQN("foo.\u00A0.bar"));
		assertFalse(Verifier.isFQN("\\u00A0foo.bar"));
		assertFalse(Verifier.isFQN("0"));
	}

	/**
	 * Verify that an invalid namespace error is actually an error
	 */
	public void verifyNamespace() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("Require-Capability", "+++,bla.bla");
			b.setProperty("Provide-Capability", "===,bla.bla");
			b.setIncludeResource("foo;literal='foo'");
			Jar inner = b.build();
			assertTrue(b.check("The Require-Capability with namespace \\+\\+\\+ is not a symbolic name",
				"The Provide-Capability with namespace === is not a symbolic name"));
		}
	}

	/**
	 * Verify that the Meta-Persistence header is correctly verified
	 *
	 * @throws Exception
	 */

	public void verifyMetaPersistence() throws Exception {
		try (Builder b = new Builder()) {
			b.setIncludeResource("foo.xml;literal='I exist'");
			Jar inner = b.build();
			assertTrue(b.check());

			try (Jar outer = new Jar("x")) {
				outer.putResource("foo.jar", new JarResource(inner));
				Manifest m = new Manifest();
				m.getMainAttributes()
					.putValue(Constants.META_PERSISTENCE, "foo.jar, foo.jar!/foo.xml, absent.xml");
				outer.setManifest(m);
				try (Verifier v = new Verifier(outer)) {
					v.verifyMetaPersistence();
					assertTrue(v.check("Meta-Persistence refers to resources not in the bundle: \\[absent.xml\\]"));
				}
			}
		}
	}

	/**
	 * Check for reserved file names (INVALIDFILENAMES)
	 *
	 * @throws Exception
	 */
	public void testInvalidFileNames() throws Exception {
		testFileName("0ABC", null, true);
		testFileName("0ABC", "[0-9].*|${@}", false);
		testFileName("com1", null, false);
		testFileName("COM1", null, false);
		testFileName("cOm1", null, false);
		testFileName("aux", null, false);
		testFileName("AUX", null, false);
		testFileName("XYZ", null, true);
		testFileName("XYZ", "XYZ|${@}", false);
		testFileName("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", ".{33,}|${@}", false);
		testFileName("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", null, true);
		testFileName("clock$", null, false);
		testFileName("clock", null, true);
	}

	private void testFileName(String segment, String pattern, boolean answer) throws Exception {
		testFilePath(segment, pattern, answer);
		testFilePath("abc/" + segment, pattern, answer);
		testFilePath("abc/" + segment + "/def", pattern, answer);
		testFilePath(segment + "/def", pattern, answer);
	}

	private void testFilePath(String path, String pattern, boolean good) throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("-includeresource", path + ";literal='x'");
			if (pattern != null)
				b.setProperty(Constants.INVALIDFILENAMES, pattern);

			b.build();
			if (good)
				assertTrue(b.check());
			else
				assertTrue(b.check("Invalid file/directory"));
		}
	}

	/**
	 * Create a require capality filter verification test
	 *
	 * @throws Exception
	 */

	public void testInvalidFilterOnRequirement() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("jar/osgi.jar"));
			b.setExportPackage("org.osgi.framework");
			b.setProperty("Require-Capability", "test; filter:=\"(&(test=aName)(version>=1.1.0))\", "
				+ " test; filter:=\"(&(version>=1.1)(string~=astring))\", "
				+ " test; filter:=\"(&(version>=1.1)(long>=99))\", "
				+ " test; filter:=\"(&(version>=1.1)(double>=1.0))\",  "
				+ " test; filter:=\"(&(version>=1.1)(version.list=1.0)(version.list=1.1)(version.list=1.2))\", "
				+ " test; filter:=\"(&(version>=1.1)(long.list=1)(long.list=2)(long.list=3)(long.list=4))\", "
				+ " test; filter:=\"(&(version>=1.1)(double.list=1.001)(double.list=1.002)(double.list=1.003)(double.list<=1.3))\", "
				+ " test; filter:=\"(&(version>=1.1)(string.list~=astring)(string.list~=bstring)(string.list=cString))\", "
				+ " test; filter:=\"(&(version>=1.1)(string.list2=a\\\"quote)(string.list2=a\\,comma)(string.list2= aSpace )(string.list2=\\\"start)(string.list2=\\,start)(string.list2=end\\\")(string.list2=end\\,))\", "
				+ " test; filter:=\"(&(version>=1.1)(string.list3= aString )(string.list3= bString )(string.list3= cString ))\", "
				+ " test.effective; effective:=\"active\"; filter:=\"(willResolve=false)\", test.no.attrs");

			b.build();
			assertTrue(b.check());
		}
	}

	/**
	 * Create a require capality directive test
	 *
	 * @throws Exception
	 */

	public void testValidDirectivesOnRequirement() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("jar/osgi.jar"));
			b.setExportPackage("org.osgi.framework");
			b.setProperty("Require-Capability",
				"test; resolution:=mandatory, " + " test; resolution:=optional, " + " test; cardinality:=single, "
					+ " test; cardinality:=multiple, " + " test; effective:=foo, "
					+ " test; filter:=\"(&(version>=1.1) (long.list=1)(long.list=2) )\", " + " test; x-custom:=bar, ");

			b.build();
			assertTrue(b.check());
		}
	}

	/**
	 * Test the strict flag
	 */
	public void testStrict() throws Exception {
		try (Builder bmaker = new Builder()) {
			bmaker.addClasspath(IO.getFile("jar/osgi.jar"));
			bmaker.addClasspath(new File("bin_test"));
			bmaker.setProperty("Export-Package",
				"org.osgi.service.eventadmin;version='[1,2)',org.osgi.framework;version=x13,test;-remove-attribute:=version,test.lib;specification-version=12,test.split");
			bmaker.setProperty("Import-Package",
				"foo;version=1,bar;version='[1,x2)',baz;version='[2,1)',baz2;version='(1,1)',*");
			bmaker.setProperty("-strict", "true");
			bmaker.setProperty("-fixupmessages", "^Exception: ");
			Jar jar = bmaker.build();
			assertTrue(bmaker.check("\\QInvalid syntax for version: x13, for cmd: range, arguments; [range, [==,+)]\\E",
				"\\QImport Package org.osgi.framework has an invalid version range syntax ${range;[==,+)}\\E",
				"\\QNo translation found for macro: range;[==,+)\\E",
				"\\QExport-Package or -exportcontents refers to missing package 'org.osgi.service.eventadmin'\\E",
				"Import Package clauses without version range \\(excluding javax\\.\\*\\):",
				"Import Package bar has an invalid version range syntax \\[1,x2\\)",
				"Import Package baz2 has an empty version range syntax \\(1,1\\), likely want to use \\[1.0.0,1.0.0\\]",
				"Import Package baz has an invalid version range syntax \\[2,1\\): Low Range is higher than High Range: 2.0.0-1.0.0",
				"Import Package clauses which use a version instead of a version range. This imports EVERY later package and not as many expect until the next major number: \\[foo\\]",
				"Export Package org.osgi.framework version has invalid syntax: x13",
				"Export Package test.lib uses deprecated specification-version instead of version",
				"Export Package org.osgi.service.eventadmin version is a range: \\[1,2\\); Exports do not allow for ranges."));
		}
	}

	public void testCapability() throws Exception {

		Parameters h = OSGiHeader.parseHeader(
			"test; version.list:List < Version > = \"1.0, 1.1, 1.2\"; effective:=\"resolve\"; test =\"aName\";version : Version=\"1.0\"; long :Long=\"100\"; "
				+ "double: Double=\"1.001\"; string:String =\"aString\";   "
				+ "long.list : List <Long >=\"1, 2, 3, 4\";double.list: List< Double>= \"1.001, 1.002, 1.003\"; "
				+ "string.list :List<String >= \"aString,bString,cString\"");

		assertEquals(Attrs.Type.VERSION, h.get("test")
			.getType("version"));
		assertEquals(Attrs.Type.LONG, h.get("test")
			.getType("long"));
		assertEquals(Attrs.Type.DOUBLE, h.get("test")
			.getType("double"));
		assertEquals(Attrs.Type.STRING, h.get("test")
			.getType("string"));
		assertEquals(Attrs.Type.STRING, h.get("test")
			.getType("test"));
		assertEquals(Attrs.Type.LONGS, h.get("test")
			.getType("long.list"));
		assertEquals(Attrs.Type.DOUBLES, h.get("test")
			.getType("double.list"));
		assertEquals(Attrs.Type.STRINGS, h.get("test")
			.getType("string.list"));
		assertEquals(Attrs.Type.VERSIONS, h.get("test")
			.getType("version.list"));
	}

	public void testFailedOSGiJar() throws Exception {
		try (Jar jar = new Jar("jar/osgi.residential-4.3.0.jar"); Verifier v = new Verifier(jar)) {
			assertTrue(v.check());
		}
	}

	public void testnativeCode() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(new File("bin_test"));
			b.setProperty("-resourceonly", "true");
			b.setProperty("Include-Resource", "native/win32/NTEventLogAppender-1.2.dll;literal='abc'");
			b.setProperty("Bundle-NativeCode", "native/win32/NTEventLogAppender-1.2.dll; osname=Win32; processor=x86");
			b.build();
			try (Verifier v = new Verifier(b)) {
				v.verifyNative();
				System.err.println(v.getErrors());
				assertEquals(0, v.getErrors()
					.size());
			}
		}
	}

	public void testFilter() {

		testFilter("(&(a=b)(c=1))");
		testFilter("(&(a=b)(!(c=1))(&(c=1))(c=1)(c=1)(c=1)(c=1)(c=1)(c=1)(c=1)(c=1))");
		testFilter("(!(a=b))");
		testInvalidFilter("(!(a=b)(c=2))");
		testInvalidFilter("(axb)");
		testInvalidFilter("(a=3 ");
		testFilter("(room=*)");
		testFilter("(room=bedroom)");
		testFilter("(room~= B E D R O O M )");
		testFilter("(room=abc)");
		testFilter(" ( room >=aaaa)");
		testFilter("(room <=aaaa)");
		testFilter("  ( room =b*) ");
		testFilter("  ( room =*m) ");
		testFilter("(room=bed*room)");
		testFilter("  ( room =b*oo*m) ");
		testFilter("  ( room =*b*oo*m*) ");
		testFilter("  ( room =b*b*  *m*) ");
		testFilter("  (& (room =bedroom) (channel ~=34))");
		testFilter("  (&  (room =b*)  (room =*x) (channel=34))");
		testFilter("(| (room =bed*)(channel=222)) ");
		testFilter("(| (room =boom*)(channel=101)) ");
		testFilter("  (! (room =ab*b*oo*m*) ) ");
		testFilter("  (status =\\(o*\\\\\\)\\*) ");
		testFilter("  (canRecord =true\\(x\\)) ");
		testFilter("(max Record Time <=140) ");
		testFilter("(shortValue >=100) ");
		testFilter("(intValue <=100001) ");
		testFilter("(longValue >=10000000000) ");
		testFilter("  (  &  (  byteValue <=100)  (  byteValue >=10)  )  ");
		testFilter("(weirdValue =100) ");
		testFilter("(bigIntValue =4123456) ");
		testFilter("(bigDecValue =4.123456) ");
		testFilter("(floatValue >=1.0) ");
		testFilter("(doubleValue <=2.011) ");
		testFilter("(charValue ~=a) ");
		testFilter("(booleanValue =true) ");
		testFilter("(primIntArrayValue =1) ");
		testFilter("(primLongArrayValue =2) ");
		testFilter("(primByteArrayValue =3) ");
		testFilter("(primShortArrayValue =1) ");
		testFilter("(primFloatArrayValue =1.1) ");
		testFilter("(primDoubleArrayValue =2.2) ");
		testFilter("(primCharArrayValue ~=D) ");
		testFilter("(primBooleanArrayValue =false) ");
		testFilter("(& (| (room =d*m) (room =bed*) (room=abc)) (! (channel=999)))");
		testFilter("(room=bedroom)");
		testFilter("(*=foo)");
		testInvalidFilter("(!  ab=b)");
		testInvalidFilter("(|   ab=b)");
		testInvalidFilter("(&=c)");
		testInvalidFilter("(!=c)");
		testInvalidFilter("(|=c)");
		testInvalidFilter("(&    ab=b)");
		testInvalidFilter("(!ab=*)");
		testInvalidFilter("(|ab=*)");
		testInvalidFilter("(&ab=*)");
		testFilter("(empty=)");
		testFilter("(empty=*)");
		testFilter("(space= )");
		testFilter("(space=*)");
		testFilter("(intvalue=*)");
		testFilter("(intvalue=b)");
		testFilter("(intvalue=)");
		testFilter("(longvalue=*)");
		testFilter("(longvalue=b)");
		testFilter("(longvalue=)");
		testFilter("(shortvalue=*)");
		testFilter("(shortvalue=b)");
		testFilter("(shortvalue=)");
		testFilter("(bytevalue=*)");
		testFilter("(bytevalue=b)");
		testFilter("(bytevalue=)");
		testFilter("(charvalue=*)");
		testFilter("(charvalue=)");
		testFilter("(floatvalue=*)");
		testFilter("(floatvalue=b)");
		testFilter("(floatvalue=)");
		testFilter("(doublevalue=*)");
		testFilter("(doublevalue=b)");
		testFilter("(doublevalue=)");
		testFilter("(booleanvalue=*)");
		testFilter("(booleanvalue=b)");
		testFilter("(booleanvalue=)");

		testInvalidFilter("");
		testInvalidFilter("()");
		testInvalidFilter("(=foo)");
		testInvalidFilter("(");
		testInvalidFilter("(abc = ))");
		testInvalidFilter("(& (abc = xyz) (& (345))");
		testInvalidFilter("  (room = b**oo!*m*) ) ");
		testInvalidFilter("  (room = b**oo)*m*) ) ");
		testInvalidFilter("  (room = *=b**oo*m*) ) ");
		testInvalidFilter("  (room = =b**oo*m*) ) ");
		testFilter("(shortValue =100*) ");
		testFilter("(intValue =100*) ");
		testFilter("(longValue =100*) ");
		testFilter("(  byteValue =1*00  )");
		testFilter("(bigIntValue =4*23456) ");
		testFilter("(bigDecValue =4*123456) ");
		testFilter("(floatValue =1*0) ");
		testFilter("(doubleValue =2*011) ");
		testFilter("(charValue =a*) ");
		testFilter("(booleanValue =t*ue) ");
	}

	private void testFilter(String string) {
		int index = Verifier.verifyFilter(string, 0);
		while (index < string.length() && Character.isWhitespace(string.charAt(index)))
			index++;

		if (index != string.length())
			throw new IllegalArgumentException("Characters after filter");
	}

	private void testInvalidFilter(String string) {
		try {
			testFilter(string);
			fail("Invalid filter");
		} catch (Exception e) {}
	}

	public void testBundleActivationPolicyNone() throws Exception {
		try (Builder v = new Builder()) {
			v.setProperty("Private-Package", "test.activator");
			v.addClasspath(new File("bin_test"));
			v.build();
			assertTrue(v.check());
		}
	}

	public void testBundleActivationPolicyBad() throws Exception {
		try (Builder v = new Builder()) {
			v.setProperty("Private-Package", "test.activator");
			v.addClasspath(new File("bin_test"));
			v.setProperty(Constants.BUNDLE_ACTIVATIONPOLICY, "eager");
			v.build();
			assertTrue(v.check("Bundle-ActivationPolicy set but is not set to lazy: eager"));
		}
	}

	public void testBundleActivationPolicyGood() throws Exception {
		try (Builder v = new Builder()) {
			v.setProperty("Private-Package", "test.activator");
			v.addClasspath(new File("bin_test"));
			v.setProperty(Constants.BUNDLE_ACTIVATIONPOLICY, "lazy   ;   hello:=1");
			v.build();
			assertTrue(v.check());
		}
	}

	public void testBundleActivationPolicyMultiple() throws Exception {
		try (Builder v = new Builder()) {
			v.setProperty("Private-Package", "test.activator");
			v.addClasspath(new File("bin_test"));
			v.setProperty(Constants.BUNDLE_ACTIVATIONPOLICY, "lazy;hello:=1,2");
			v.build();
			assertTrue(v.check("Bundle-ActivationPolicy has too many arguments lazy;hello:=1,2"));
		}
	}

	public void testInvalidCaseForHeader() throws Exception {
		Properties p = new Properties();
		p.put("Export-package", "org.apache.mina.*");
		p.put("Bundle-Classpath", ".");
		try (Analyzer analyzer = new Analyzer()) {
			analyzer.setProperties(p);
			analyzer.getProperties();
			System.err.println("Errors   " + analyzer.getErrors());
			System.err.println("Warnings " + analyzer.getWarnings());
			assertEquals(0, analyzer.getErrors()
				.size());
			assertEquals(2, analyzer.getWarnings()
				.size());
		}
	}

	public void testSimple() throws Exception {
		try (Builder bmaker = new Builder()) {
			bmaker.addClasspath(IO.getFile("jar/mina.jar"));
			bmaker.setProperty("Export-Package", "org.apache.mina.*;version=1");
			bmaker.setProperty("DynamicImport-Package", "org.slf4j");
			Jar jar = bmaker.build();
			assertTrue(bmaker.check());

			Manifest m = jar.getManifest();
			m.write(System.err);
			assertTrue(m.getMainAttributes()
				.getValue("Import-Package")
				.contains("org.slf4j"));
			assertTrue(m.getMainAttributes()
				.getValue("DynamicImport-Package")
				.contains("org.slf4j"));
		}
	}

}
