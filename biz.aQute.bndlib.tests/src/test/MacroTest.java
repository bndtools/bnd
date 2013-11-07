package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.bnd.osgi.*;

@SuppressWarnings("resource")
public class MacroTest extends TestCase {
	/**
	 * Test the custom macros
	 */
	
	public void testCustomMacros() {
		assertTemplate("this is 1 abc, and this is def", "this is 1 ${1}, and this is ${2}", "abc;def");
		assertTemplate("abc,def", "${*}", "abc;def");
		
	}
	
	void assertTemplate(String result, String template, String params) {
		Processor top = new Processor();
		top.setProperty("template", template);
		top.setProperty("macro", "${template;"+params+"}");
		String expanded = top.getProperty("macro");
		assertEquals( result, expanded);
	}

	/**
	 * Test replacement of ./ with cwd
	 */

	public static void testCurrentWorkingDirectory() {
		Processor top = new Processor();
		top.setProperty("cwd.1", "./"); // empty
		top.setProperty("cwd.2", " ./"); // empty
		top.setProperty("cwd.3", "./ "); // empty
		top.setProperty("cwd.4", " ./ "); // empty
		top.setProperty("cwd.5", "|./|"); // empty
		top.setProperty("cwd.6", "/.//"); // empty
		top.setProperty("cwd.7", "."); // empty
		top.setProperty("cwd.8", " . "); // empty
		top.setProperty("cwd.9", " . /"); // empty
		top.setProperty("cwd.10", " ."); // empty
		top.setProperty("cwd.11", "| ./|"); // empty
		top.setProperty("cwd.12", "|\t./|"); // empty
		top.setProperty("cwd.13", "|\r./|"); // empty
		top.setProperty("cwd.14", "|\n./|"); // empty

		String cwd = top.getBase().getAbsolutePath() + "/";

		assertEquals(" . ", top.getProperty("cwd.8"));
		assertEquals(cwd, top.getProperty("cwd.1"));
		assertEquals(" " + cwd, top.getProperty("cwd.2"));
		assertEquals(cwd + " ", top.getProperty("cwd.3"));
		assertEquals(" " + cwd + " ", top.getProperty("cwd.4"));
		assertEquals("|./|", top.getProperty("cwd.5"));
		assertEquals("/.//", top.getProperty("cwd.6"));
		assertEquals(".", top.getProperty("cwd.7"));
		assertEquals(" . /", top.getProperty("cwd.9"));
		assertEquals(" .", top.getProperty("cwd.10"));
		assertEquals("| " + cwd + "|", top.getProperty("cwd.11"));
		assertEquals("|\t" + cwd + "|", top.getProperty("cwd.12"));
		assertEquals("|\r" + cwd + "|", top.getProperty("cwd.13"));
		assertEquals("|\n" + cwd + "|", top.getProperty("cwd.14"));
	}

	/**
	 * Test if $if accepts isdir
	 */

	public static void testifDir() {
		Processor top = new Processor();
		top.setProperty("presentd", "${if;${isdir;jar};YES;NO}");
		top.setProperty("absentd", "${if;${isdir;xxx};YES;NO}");
		top.setProperty("wrongd", "${if;${isdir;bnd.bnd};YES;NO}");
		assertEquals("YES", top.getProperty("presentd"));
		assertEquals("NO", top.getProperty("wrongd"));
		assertEquals("NO", top.getProperty("absentd"));
		top.setProperty("presentf", "${if;${isfile;bnd.bnd};YES;NO}");
		top.setProperty("absentf", "${if;${isfile;xxx};YES;NO}");
		top.setProperty("wrongf", "${if;${isfile;jar};YES;NO}");
		assertEquals("YES", top.getProperty("presentf"));
		assertEquals("NO", top.getProperty("absentf"));
		assertEquals("NO", top.getProperty("wrongf"));
	}

	/**
	 * Test the combine macro that groups properties
	 */

	public static void testWildcardKeys() {
		Processor top = new Processor();
		top.setProperty("a.3", "a.3");
		top.setProperty("a.1", "a.1");
		top.setProperty("a.2", "a.2");
		top.setProperty("a.4", "a.4");
		top.setProperty("aa", "${a.*}");
		assertEquals("a.1,a.2,a.3,a.4", top.getProperty("a.*"));
		assertEquals("a.1,a.2,a.3,a.4", top.getProperty("aa"));

	}

	public static void testEnv() {
		Processor proc = new Processor();
		String s = proc.getReplacer().process("${env;USER}");
		assertNotNull(s);
	}

	/**
	 * Test the random macro
	 */
	public static void testRandom() {
		Processor top = new Processor();
		top.setProperty("a", "${random}");
		top.setProperty("a12", "${random;12}");
		String a = top.getProperty("a");
		System.err.println(a);
		assertEquals(8, a.length());
		String a12 = top.getProperty("a12");
		System.err.println(a12);
		assertEquals(12, a12.length());
		assertNotSame(a, a12);
	}
	
	/**
	 * Test the native macro
	 */
	public static void testNativeCapabilityMacro() {
		Processor p = new Processor();
		p.setProperty("a", "${native_capability}");
		
		String origOsName = System.getProperty("os.name");
		String origOsVersion = System.getProperty("os.version");
		String origOsArch = System.getProperty("os.arch");
		String processed;
		try {
			System.setProperty("os.name", "Mac OS X");
			System.setProperty("os.version", "10.8.2");
			System.setProperty("os.arch", "x86_64");
			processed = p.getProperty("a");
			assertEquals(0, p.getErrors().size());
			assertEquals("osgi.native;osgi.native.osname:List<String>=\"MacOSX,Mac OS X\";osgi.native.osversion:Version=10.8.2;osgi.native.processor:List<String>=\"x86-64,amd64,em64t,x86_64\"", processed);

			System.setProperty("os.name", "Linux");
			System.setProperty("os.arch", "amd64");
			System.setProperty("os.version", "3.8.8-202.fc18.x86_64");
			processed = p.getProperty("a");
			assertEquals(0, p.getErrors().size());
			assertEquals("osgi.native;osgi.native.osname:List<String>=\"Linux\";osgi.native.osversion:Version=3.8.8.-202_fc18_x86_64;osgi.native.processor:List<String>=\"x86-64,amd64,em64t,x86_64\"", processed);

			System.setProperty("os.name", "Linux");
			System.setProperty("os.arch", "em64t");
			System.setProperty("os.version", "3.8.8-202.fc18.x86_64");
			processed = p.getProperty("a");
			assertEquals(0, p.getErrors().size());
			assertEquals("osgi.native;osgi.native.osname:List<String>=\"Linux\";osgi.native.osversion:Version=3.8.8.-202_fc18_x86_64;osgi.native.processor:List<String>=\"x86-64,amd64,em64t,x86_64\"", processed);

			System.setProperty("os.name", "Windows XP");
			System.setProperty("os.version", "5.1.7601.17514");
			System.setProperty("os.arch", "x86");
			processed = p.getProperty("a");
			assertEquals(0, p.getErrors().size());
			assertEquals("osgi.native;osgi.native.osname:List<String>=\"WindowsXP,WinXP,Windows XP,Win32\";osgi.native.osversion:Version=5.1.0;osgi.native.processor:List<String>=\"x86,pentium,i386,i486,i586,i686\"", processed);

			System.setProperty("os.name", "Windows Vista");
			System.setProperty("os.version", "6.0.7601.17514");
			System.setProperty("os.arch", "x86");
			processed = p.getProperty("a");
			assertEquals(0, p.getErrors().size());
			assertEquals("osgi.native;osgi.native.osname:List<String>=\"WindowsVista,WinVista,Windows Vista,Win32\";osgi.native.osversion:Version=6.0.0;osgi.native.processor:List<String>=\"x86,pentium,i386,i486,i586,i686\"", processed);

			System.setProperty("os.name", "Windows 7");
			System.setProperty("os.version", "6.1.7601.17514");
			System.setProperty("os.arch", "x86");
			processed = p.getProperty("a");
			assertEquals(0, p.getErrors().size());
			assertEquals("osgi.native;osgi.native.osname:List<String>=\"Windows7,Windows 7,Win32\";osgi.native.osversion:Version=6.1.0;osgi.native.processor:List<String>=\"x86,pentium,i386,i486,i586,i686\"", processed);

			System.setProperty("os.name", "Windows 8");
			System.setProperty("os.version", "6.2.7601.17514");
			System.setProperty("os.arch", "x86");
			processed = p.getProperty("a");
			assertEquals(0, p.getErrors().size());
			assertEquals("osgi.native;osgi.native.osname:List<String>=\"Windows8,Windows 8,Win32\";osgi.native.osversion:Version=6.2.0;osgi.native.processor:List<String>=\"x86,pentium,i386,i486,i586,i686\"", processed);

			System.setProperty("os.name", "Windows 3.1");
			System.setProperty("os.version", "3.1.7601.17514");
			System.setProperty("os.arch", "x86");
			processed = p.getProperty("a");
			assertEquals(1, p.getErrors().size());
			p.getErrors().clear();

			System.setProperty("os.name", "Solaris");
			System.setProperty("os.arch", "amd64");
			System.setProperty("os.version", "3.8");
			processed = p.getProperty("a");
			assertEquals(0, p.getErrors().size());
			assertEquals("osgi.native;osgi.native.osname:List<String>=\"Solaris\";osgi.native.osversion:Version=3.8.0;osgi.native.processor:List<String>=\"x86-64,amd64,em64t,x86_64\"", processed);

			System.setProperty("os.name", "AIX");
			System.setProperty("os.arch", "amd64");
			System.setProperty("os.version", "3.8-202.x86_64");
			processed = p.getProperty("a");
			assertEquals(0, p.getErrors().size());
			assertEquals("osgi.native;osgi.native.osname:List<String>=\"AIX\";osgi.native.osversion:Version=3.8.0.-202_x86_64;osgi.native.processor:List<String>=\"x86-64,amd64,em64t,x86_64\"", processed);

			System.setProperty("os.name", "HP-UX");
			System.setProperty("os.arch", "amd64");
			System.setProperty("os.version", "3.8.8-202.fc18.x86_64");
			processed = p.getProperty("a");
			assertEquals(0, p.getErrors().size());
			assertEquals("osgi.native;osgi.native.osname:List<String>=\"HPUX,hp-ux\";osgi.native.osversion:Version=3.8.8.-202_fc18_x86_64;osgi.native.processor:List<String>=\"x86-64,amd64,em64t,x86_64\"", processed);

			/* unknown processor */
			System.setProperty("os.name", "Linux");
			System.setProperty("os.arch", "mips12345");
			System.setProperty("os.version", "3.8.8-202.fc18.x86_64");
			processed = p.getProperty("a");
			assertEquals(1, p.getErrors().size());
			p.getErrors().clear();

			/* unknown OS */
			System.setProperty("os.name", "Some Very Cool OS");
			System.setProperty("os.arch", "mips12345");
			System.setProperty("os.version", "3.8.8-202.fc18.x86_64");
			processed = p.getProperty("a");
			assertEquals(1, p.getErrors().size());
			p.getErrors().clear();

			/*
			 * overrides
			 */

			System.setProperty("os.name", "Linux");
			System.setProperty("os.arch", "amd64");
			System.setProperty("os.version", "3.8.8-202.fc18.x86_64");

			p.setProperty("a", "${native_capability;osname=Some Very Cool OS;}");
			processed = p.getProperty("a");
			assertEquals(0, p.getErrors().size());
			assertEquals("osgi.native;osgi.native.osname:List<String>=\"Some Very Cool OS\";osgi.native.osversion:Version=3.8.8.-202_fc18_x86_64;osgi.native.processor:List<String>=\"x86-64,amd64,em64t,x86_64\"", processed);

			p.setProperty("a", "${native_capability;osversion=3.2.0.qualifier;}");
			processed = p.getProperty("a");
			assertEquals(0, p.getErrors().size());
			assertEquals("osgi.native;osgi.native.osname:List<String>=\"Linux\";osgi.native.osversion:Version=3.2.0.qualifier;osgi.native.processor:List<String>=\"x86-64,amd64,em64t,x86_64\"", processed);

			p.setProperty("a", "${native_capability;processor=mips12345;}");
			processed = p.getProperty("a");
			assertEquals(0, p.getErrors().size());
			assertEquals("osgi.native;osgi.native.osname:List<String>=\"Linux\";osgi.native.osversion:Version=3.8.8.-202_fc18_x86_64;osgi.native.processor:List<String>=\"mips12345\"", processed);

			/* invalid override field */
			p.setProperty("a", "${native_capability;invalidoverridefield=value}");
			processed = p.getProperty("a");
			assertEquals(1, p.getErrors().size());
			p.getErrors().clear();

			/* invalid override format */
			p.setProperty("a", "${native_capability;processor}");
			processed = p.getProperty("a");
			assertEquals(1, p.getErrors().size());
			p.getErrors().clear();

			/* no os name */
			System.clearProperty("os.name");
			System.setProperty("os.arch", "amd64");
			System.setProperty("os.version", "3.8.8-202.fc18.x86_64");
			processed = p.getProperty("a");
			assertEquals(1, p.getErrors().size());
			p.getErrors().clear();

			System.setProperty("os.name", "");
			System.setProperty("os.arch", "amd64");
			System.setProperty("os.version", "3.8.8-202.fc18.x86_64");
			processed = p.getProperty("a");
			assertEquals(1, p.getErrors().size());
			p.getErrors().clear();

			/* no os version */
			System.setProperty("os.name", "Linux");
			System.setProperty("os.arch", "amd64");
			System.clearProperty("os.version");
			processed = p.getProperty("a");
			assertEquals(1, p.getErrors().size());
			p.getErrors().clear();

			System.setProperty("os.name", "Linux");
			System.setProperty("os.arch", "amd64");
			System.setProperty("os.version", "");
			processed = p.getProperty("a");
			assertEquals(1, p.getErrors().size());
			p.getErrors().clear();

			/* no processor */
			System.setProperty("os.name", "Linux");
			System.clearProperty("os.arch");
			System.setProperty("os.version", "3.8.8-202.fc18.x86_64");
			processed = p.getProperty("a");
			assertEquals(1, p.getErrors().size());
			p.getErrors().clear();
		} finally {
			System.setProperty("os.name", origOsName);
			System.setProperty("os.version", origOsVersion);
			System.setProperty("os.arch", origOsArch);
		}
	}

	/**
	 * Testing an example with nesting that was supposd not to work
	 */

	public static void testSuper() {
		Processor top = new Processor();
		Processor middle = new Processor(top);
		Processor bottom = new Processor(middle);

		top.setProperty("a", "top.a");
		top.setProperty("b", "top.b");
		top.setProperty("c", "top.c");
		top.setProperty("Bundle-Version", "0.0.0");
		middle.setProperty("a", "middle.a");
		middle.setProperty("b", "${^a}");
		middle.setProperty("c", "-${^c}-");
		middle.setProperty("Bundle-Version", "${^Bundle-Version}");
		assertEquals("middle.a", bottom.getProperty("a"));
		assertEquals("top.a", bottom.getProperty("b"));
		assertEquals("-top.c-", bottom.getProperty("c"));
		assertEquals("0.0.0", bottom.getProperty("Bundle-Version"));
	}

	/**
	 * Testing an example with nesting that was supposd not to work
	 */

	public static void testNesting2() {
		Processor p = new Processor();
		p.setProperty("groupId", "com.trivadis.tomas");
		p.setProperty("artifactId", "common");
		p.setProperty("bsn", "${if;${symbolicName};${symbolicName};${groupId}.${artifactId}}");
		p.setProperty("Bundle-SymbolicName", "${bsn}");
		p.setProperty("symbolicName", "");

		// Not set, so get the maven name
		assertEquals("com.trivadis.tomas.common", p.getProperty("Bundle-SymbolicName"));

		// Set it
		p.setProperty("symbolicName", "testing");
		assertEquals("testing", p.getProperty("Bundle-SymbolicName"));

		// And remove it
		p.setProperty("symbolicName", "");
		assertEquals("com.trivadis.tomas.common", p.getProperty("Bundle-SymbolicName"));
	}

	/**
	 * Verify system command
	 */

	public static void testSystem() throws Exception {
		// disable this test on windows
		if (!"/".equals(File.separator)) return;
		
		Processor p = new Processor();
		Macro macro = new Macro(p);
		assertEquals("Hello World", macro.process("${system;echo Hello World}"));
		assertTrue(macro.process("${system;wc;Hello World}").matches("\\s*[0-9]+\\s+[0-9]+\\s+[0-9]+\\s*"));
	}

	public static void testSystemFail() throws Exception {
		Processor p = new Processor();
		Macro macro = new Macro(p);
		String cmd = "${system;mostidioticcommandthatwillsurelyfail}";
		assertTrue(macro.process(cmd).startsWith("${system;"));
	}

	/**
	 * Verify system-allow-fail command
	 */

	public static void testSystemAllowFail() throws Exception {
		Processor p = new Processor();
		Macro macro = new Macro(p);
		assertEquals("", macro.process("${system-allow-fail;mostidioticcommandthatwillsurelyfail}"));
	}

	/**
	 * Check that variables override macros.
	 */
	public static void testPriority() {
		Processor p = new Processor();
		p.setProperty("now", "not set");
		Macro macro = new Macro(p);
		assertEquals("not set", macro.process("${now}"));

	}

	public static void testNames() {
		Processor p = new Processor();
		p.setProperty("a", "a");
		p.setProperty("aa", "aa");
		Macro macro = new Macro(p);

		assertEquals("aa", macro.process("${${a}${a}}"));
	}

	public static void testVersion() throws Exception {
		Processor proc = new Processor();
		Macro macro = new Macro(proc);
		assertEquals("1.0.0", macro.process("${version;===;1.0.0}"));
		assertEquals("1.0.1", macro.process("${version;==+;1.0.0}"));
		assertEquals("1.1.1", macro.process("${version;=++;1.0.0}"));
		assertEquals("2.1.1", macro.process("${version;+++;1.0.0}"));
		assertEquals("0.1.1", macro.process("${version;-++;1.0.0}"));
		assertEquals("0.1.1", macro.process("${version;-++;1.0.0}"));
		assertEquals("0.0.0", macro.process("${version;---;1.1.1}"));
		assertEquals("0.0", macro.process("${version;--;1.1.1}"));
		assertEquals("1", macro.process("${version;=;1.1.1}"));
		assertEquals("[1.1,1.2)", macro.process("[${version;==;1.1.1},${version;=+;1.1.1})"));
		assertEquals("1.1", macro.process("${version;==;1.1.1}"));
		assertEquals("0.1.0", macro.process("${version;=+0;0.0.1}"));
		assertEquals("1.0.0", macro.process("${version;+00;0.1.1}"));

		// Test implicit version
		proc.setProperty("@", "1.2.3");
		assertEquals("[1.2,1.3)", macro.process("[${version;==},${version;=+})"));
		assertEquals("1.2", macro.process("${version;==}"));
		assertEquals("1.3.0", macro.process("${version;=+0}"));
		assertEquals("2.0.0", macro.process("${version;+00}"));

		assertEquals(0, proc.getErrors().size());
		assertEquals(0, proc.getWarnings().size());

	}

	public static void testRange() throws Exception {
		Processor proc = new Processor();
		Macro macro = new Macro(proc);
		assertEquals("[1.0,1.0]", macro.process("${range;[==,==];1.0.0}"));
		assertEquals("[1.0.0,1.0.1]", macro.process("${range;[===,==+];1.0.0}"));
		assertEquals("[0.1.0,0.1.2)", macro.process("${range;[=+0,=++);0.0.1}"));
		assertEquals("[0.0.9,0.1.2)", macro.process("${range;[==9,=++);0.0.1}"));

		assertEquals(0, proc.getErrors().size());
		assertEquals(0, proc.getWarnings().size());

		proc.setProperty("@", "1.2.3");
		assertEquals("[1.0.0,2)", macro.process("${range;[=00,+)}"));

		proc.clear();
		macro.process("${range;=+0,=++;0.0.1}");
		assertEquals(1, proc.getErrors().size());
		assertEquals(1, proc.getWarnings().size());

		proc.clear();
		macro.process("${range;[+,=)}");
		assertEquals(1, proc.getErrors().size());
		assertEquals(1, proc.getWarnings().size());
	}

	/**
	 * Test the wc function
	 */

	public static void testWc() {
		Processor p = new Processor();
		Macro macro = new Macro(p);
		String a = macro.process("${lsr;" + new File("src" + File.separator + "test").getAbsolutePath() + ";*.java}");
		assertTrue(a.contains("MacroTest.java"));
		assertTrue(a.contains("ManifestTest.java"));
		assertFalse(a.contains("bnd.info"));
		assertFalse(a.contains("com.acme"));
		assertFalse(a.contains("src" + File.separator + "test" + File.separator + "MacroTest.java"));
		assertFalse(a.contains("src" + File.separator + "test" + File.separator + "ManifestTest.java"));

		String b = macro.process("${lsa;" + new File("src" + File.separator + "test").getAbsolutePath() + ";*.java}");
		assertTrue(b.contains("src" + File.separator + "test" + File.separator + "MacroTest.java"));
		assertTrue(b.contains("src" + File.separator + "test" + File.separator + "ManifestTest.java"));
	}

	/**
	 * Check the uniq command
	 */

	public static void testUniq() {
		Builder builder = new Builder();
		Properties p = new Properties();
		p.setProperty("a", "${uniq;1}");
		p.setProperty("b", "${uniq;1,2}");
		p.setProperty("c", "${uniq;1;2}");
		p.setProperty("d", "${uniq;1; 1,  2 , 3}");
		p.setProperty("e", "${uniq;1; 1 , 2 ;      3;3,4,5,6}");
		builder.setProperties(p);
		assertEquals("1,2,3", builder.getProperty("d"));
		assertEquals("1,2", builder.getProperty("b"));
		assertEquals("1", builder.getProperty("a"));
		assertEquals("1,2", builder.getProperty("c"));
		assertEquals("1,2,3", builder.getProperty("d"));
		assertEquals("1,2,3,4,5,6", builder.getProperty("e"));

	}

	/**
	 * Test arguments with difficult characters like ;
	 */

	public static void testEscapedArgs() {
		Builder builder = new Builder();
		Properties p = new Properties();
		p.setProperty("x", "${replace;1,2,3;.+;$0\\;version=1}");
		builder.setProperties(p);
		assertEquals("1;version=1, 2;version=1, 3;version=1", builder.getProperty("x"));

	}

	/**
	 * Check if variables that contain variables, ad nauseum, really wrk
	 */
	public static void testNested() {
		Builder builder = new Builder();
		Properties p = new Properties();
		p.setProperty("a", ".");
		p.setProperty("b", "${a}");
		p.setProperty("c", "${b}");

		p.setProperty("d", "${tstamp;${format};UTC;${aug152008}}");
		p.setProperty("format", "yyyy");
		p.setProperty("aug152008", "1218810097322");

		p.setProperty("f", "${d}");
		p.setProperty("aug152008", "1218810097322");

		builder.setProperties(p);
		assertEquals(".", builder.getProperty("c"));
		assertEquals("2008", builder.getProperty("d"));
		assertEquals(builder.getProperty("f"), builder.getProperty("d"));
	}

	public static void testLoop() {
		Builder builder = new Builder();
		Properties p = new Properties();
		p.setProperty("a", "${b}");
		p.setProperty("b", "${a}");

		p.setProperty("d", "${e}");
		p.setProperty("e", "${f}");
		p.setProperty("f", "${g}");
		p.setProperty("g", "${h}");
		p.setProperty("h", "${d}");

		builder.setProperties(p);
		assertEquals("${infinite:[a,b,${b}]}", builder.getProperty("a"));
		assertEquals("${infinite:[d,h,g,f,e,${e}]}", builder.getProperty("d"));
	}

	public static void testTstamp() {
		String aug152008 = "1218810097322";
		Processor p = new Processor();
		Macro m = new Macro(p);
		assertEquals("200808151421", m.process("${tstamp;yyyyMMddHHmm;UTC;" + aug152008 + "}"));
		assertEquals("200808151521", m.process("${tstamp;yyyyMMddHHmm;GMT+01;" + aug152008 + "}"));
		assertEquals("2008", m.process("${tstamp;yyyy;UTC;" + aug152008 + "}"));
		
		// Why Tokyo? Japan doesn't use daylight savings, so the test shouldn't break when clocks change.
		assertEquals("200808152321", m.process("${tstamp;yyyyMMddHHmm;Asia/Tokyo;" + aug152008 + "}"));
	}

	public static void testIsfile() {
		Processor p = new Processor();
		Macro m = new Macro(p);
		assertEquals("true", m.process("${isfile;.project}"));
		assertEquals("false", m.process("${isfile;thisfiledoesnotexist}"));
	}

	public static void testParentFile() {
		Processor p = new Processor();
		Macro m = new Macro(p);
		assertTrue(m.process("${dir;.project}").endsWith("biz.aQute.bndlib.tests"));
	}

	public static void testBasename() {
		Processor p = new Processor();
		Macro m = new Macro(p);
		assertEquals("biz.aQute.bndlib.tests", m.process("${basename;${dir;.project}}"));
	}

	public static void testMavenVersionMacro() throws Exception {
		Builder builder = new Builder();
		Properties p = new Properties();
		p.setProperty("Export-Package", "org.objectweb.*;version=1.5-SNAPSHOT");
		builder.setProperties(p);
		builder.setClasspath(new File[] {
			new File("jar/asm.jar")
		});
		Jar jar = builder.build();
		Manifest manifest = jar.getManifest();
		String export = manifest.getMainAttributes().getValue("Export-Package");
		assertNotNull(export);
		assertTrue("Test snapshot version", export.contains("1.5.0.SNAPSHOT"));
	}

	/**
	 * Check if we can check for the defintion of a variable
	 */

	public static void testDef() {
		Processor p = new Processor();
		p.setProperty("set.1", "1");
		p.setProperty("set.2", "2");
		Macro m = new Macro(p);
		assertEquals("NO", m.process("${if;${def;set.3};YES;NO}"));
		assertEquals("YES", m.process("${if;${def;set.1};YES;NO}"));
		assertEquals("YES", m.process("${if;${def;set.2};YES;NO}"));
	}

	/**
	 * NEW
	 */
	public static void testReplace() {
		Processor p = new Processor();
		p.setProperty("specs", "a,b, c,    d");
		Macro m = new Macro(p);
		assertEquals("xay, xby, xcy, xdy", m.process("${replace;${specs};([^\\s]+);x$1y}"));
	}

	public static void testToClassName() {
		Processor p = new Processor();
		Macro m = new Macro(p);
		assertEquals("com.acme.test.Test", m.process("${toclassname;com/acme/test/Test.class}"));
		assertEquals("Test", m.process("$<toclassname;Test.class>"));
		assertEquals("Test,com.acme.test.Test", m.process("${toclassname;Test.class, com/acme/test/Test.class}"));
		assertEquals("", m.process("$(toclassname;Test)"));
		assertEquals("com/acme/test/Test.class", m.process("$[toclasspath;com.acme.test.Test]"));
		assertEquals("Test.class", m.process("${toclasspath;Test}"));
		assertEquals("Test.class,com/acme/test/Test.class", m.process("${toclasspath;Test,com.acme.test.Test}"));
	}

	public static void testFindPath() throws IOException {
		Analyzer analyzer = new Analyzer();
		analyzer.setJar(new File("jar/asm.jar"));
		Macro m = new Macro(analyzer);

		assertTrue(m.process("${findname;(.*)\\.class;$1.xyz}").indexOf("FieldVisitor.xyz,") >= 0);
		assertTrue(m.process("${findname;(.*)\\.class;$1.xyz}").indexOf("MethodVisitor.xyz,") >= 0);
		assertTrue(m.process("${findpath;(.*)\\.class}").indexOf("org/objectweb/asm/AnnotationVisitor.class,") >= 0);
		assertTrue(m.process("${findpath;(.*)\\.class}").indexOf(
				"org/objectweb/asm/ByteVector.class, org/objectweb/asm/ClassAdapter.class,") >= 0);
		assertEquals("META-INF/MANIFEST.MF", m.process("${findpath;META-INF/MANIFEST.MF}"));
		assertEquals("Label.class", m.process("${findname;Label\\..*}"));
		assertEquals("Adapter, Visitor, Writer", m.process("${findname;Method(.*)\\.class;$1}"));
	}

	public static void testWarning() {
		Processor p = new Processor();
		p.setProperty("three", "333");
		p.setProperty("empty", "");
		p.setProperty("real", "true");
		Macro m = new Macro(p);

		m.process("    ${warning;xw;1;2;3 ${three}}");
		m.process("    ${error;xe;1;2;3 ${three}}");
		m.process("    ${if;1;$<a>}");
		assertEquals("xw", p.getWarnings().get(0));
		assertEquals("1", p.getWarnings().get(1));
		assertEquals("2", p.getWarnings().get(2));
		assertEquals("3 333", p.getWarnings().get(3));

		assertEquals("xe", p.getErrors().get(0));
		assertEquals("1", p.getErrors().get(1));
		assertEquals("2", p.getErrors().get(2));
		assertEquals("3 333", p.getErrors().get(3));
	}

	public static void testNestedReplace() {
		Processor p = new Processor();
		Macro m = new Macro(p);
		String value = m.process("xx$(replace;1.2.3-SNAPSHOT;(\\d(\\.\\d)+).*;$1)xx");
		System.err.println(p.getWarnings());
		assertEquals("xx1.2.3xx", value);

		assertEquals("xx1.222.3xx", m.process("xx$(replace;1.222.3-SNAPSHOT;(\\d+(\\.\\d+)+).*;$1)xx"));

		p.setProperty("a", "aaaa");
		assertEquals("[cac]", m.process("$[replace;acaca;a(.*)a;[$1]]"));
		assertEquals("xxx", m.process("$(replace;yxxxy;[^x]*(x+)[^x]*;$1)"));
		assertEquals("xxx", m.process("$(replace;yxxxy;([^x]*(x+)[^x]*);$2)"));

	}

	public static void testParentheses() {
		Processor p = new Processor();
		Macro m = new Macro(p);
		String value = m.process("$(replace;();(\\(\\));$1)");
		assertEquals("()", value);
	}

	public static void testSimple() {
		Processor p = new Processor();
		p.setProperty("a", "aaaa");
		Macro m = new Macro(p);
		assertEquals("aaaa", m.process("${a}"));
		assertEquals("aaaa", m.process("$<a>"));
		assertEquals("aaaa", m.process("$(a)"));
		assertEquals("aaaa", m.process("$[a]"));

		assertEquals("xaaaax", m.process("x${a}x"));
		assertEquals("xaaaaxaaaax", m.process("x${a}x${a}x"));
	}

	public static void testFilter() {
		Processor p = new Processor();
		p.setProperty("a", "aaaa");
		Macro m = new Macro(p);
		assertEquals("aa,cc,ee", m.process("${filter;aa,bb,cc,dd,ee,ff;[ace]+}"));
		assertEquals("aaaa,cc,ee", m.process("${filter;${a},bb,cc,dd,ee,ff;[ace]+}"));
		assertEquals("bb,dd,ff", m.process("${filter;${a},bb,cc,dd,ee,ff;[^ace]+}"));
	}

	public static void testFilterOut() {
		Processor p = new Processor();
		p.setProperty("a", "aaaa");
		Macro m = new Macro(p);
		assertEquals("bb,dd,ff", m.process("${filterout;aa,bb,cc,dd,ee,ff;[ace]+}"));
		assertEquals("bb,dd,ff", m.process("${filterout;${a},bb,cc,dd,ee,ff;[ace]+}"));
		assertEquals("aaaa,cc,ee", m.process("${filterout;${a},bb,cc,dd,ee,ff;[^ace]+}"));
	}

	public static void testSort() {
		Processor p = new Processor();
		p.setProperty("a", "aaaa");
		Macro m = new Macro(p);
		assertEquals("aa,bb,cc,dd,ee,ff", m.process("${sort;aa,bb,cc,dd,ee,ff}"));
		assertEquals("aa,bb,cc,dd,ee,ff", m.process("${sort;ff,ee,cc,bb,dd,aa}"));
		assertEquals("aaaa,bb,cc,dd,ee,ff", m.process("${sort;ff,ee,cc,bb,dd,$<a>}"));
	}

	public static void testJoin() {
		Processor p = new Processor();
		p.setProperty("a", "aaaa");
		Macro m = new Macro(p);
		assertEquals("aa,bb,cc,dd,ee,ff", m.process("${join;aa,bb,cc,dd,ee,ff}"));
		assertEquals("aa,bb,cc,dd,ee,ff", m.process("${join;aa,bb,cc;dd,ee,ff}"));
		assertEquals("aa,bb,cc,dd,ee,ff", m.process("${join;aa;bb;cc;dd;ee,ff}"));
	}

	public static void testIf() {
		Processor p = new Processor();
		p.setProperty("a", "aaaa");
		Macro m = new Macro(p);
		assertEquals("aaaa", m.process("${if;1;$<a>}"));
		assertEquals("", m.process("${if;;$<a>}"));
		assertEquals("yes", m.process("${if;;$<a>;yes}"));
		assertEquals("yes", m.process("${if;false;$<a>;yes}"));
	}

	public static void testLiteral() {
		Processor p = new Processor();
		p.setProperty("a", "aaaa");
		Macro m = new Macro(p);
		assertEquals("${aaaa}", m.process("${literal;$<a>}"));
	}

	public static void testFilterout() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.addClasspath(new File("jar/ds.jar"));
		b.setProperty("Export-Package", "org.eclipse.*, org.osgi.*");
		b.setProperty("fwusers", "${classes;importing;org.osgi.framework}");
		b.setProperty("foo", "${filterout;${fwusers};org\\.osgi\\..*}");
		b.build();
		String fwusers = b.getProperty("fwusers");
		String foo = b.getProperty("foo");
		assertTrue(fwusers.length() > foo.length());
		assertTrue(fwusers.indexOf("org.osgi.framework.ServicePermission") >= 0);
		assertTrue(fwusers.indexOf("org.eclipse.equinox.ds.instance.BuildDispose") >= 0);
		assertFalse(foo.indexOf("org.osgi.framework.ServicePermission") >= 0);
		assertTrue(foo.indexOf("org.eclipse.equinox.ds.instance.BuildDispose") >= 0);
		System.err.println(b.getProperty("fwusers"));
		System.err.println(b.getProperty("foo"));

	}
}
