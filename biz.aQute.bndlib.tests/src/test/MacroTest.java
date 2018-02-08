package test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class MacroTest extends TestCase {

	public void testFilterExpression() throws Exception {
		Processor p = new Processor();
		p.setProperty("a", "A");
		p.setProperty("b", "1");

		assertEquals("true", p.getReplacer().process("${if;(a=A)}"));
		assertEquals("true", p.getReplacer().process("${if;(a>=A)}"));
		assertEquals("true", p.getReplacer().process("${if;(a<=A)}"));
		assertEquals("", p.getReplacer().process("${if;(a<A)}"));
		assertEquals("", p.getReplacer().process("${if;(a>A)}"));
		assertEquals("", p.getReplacer().process("${if;(a!=A)}"));

		assertEquals("true", p.getReplacer().process("${if;(a=${a})}"));

		assertEquals("true", p.getReplacer().process("${if;(a>=A)}"));
		assertEquals("true", p.getReplacer().process("${if;(a<=A)}"));
		assertEquals("", p.getReplacer().process("${if;(a<=${b})}"));

	}

	public void testFilterSubExpression() throws Exception {
		Processor p = new Processor();
		p.setProperty("a", "A");
		p.setProperty("b", "1");

		assertEquals("true", p.getReplacer().process("${if;(&(a=A)(b=1))}"));
		assertEquals("true", p.getReplacer().process("${if;(&(a=A)(b=1)(|(a!=A)(a=A)))}"));
	}

	public void testFilterWithArrays() throws Exception {
		Processor p = new Processor();
		p.setProperty("a", "A,B,C,D");
		p.setProperty("b", "1");

		assertEquals("", p.getReplacer().process("${if;(a=A)}"));
		assertEquals("true", p.getReplacer().process("${if;(a[]=A)}"));
		assertEquals("true", p.getReplacer().process("${if;(a[]=B)}"));
		assertEquals("true", p.getReplacer().process("${if;(a[]=D)}"));
		assertEquals("", p.getReplacer().process("${if;(a[]=E)}"));
		assertEquals("", p.getReplacer().process("${if;(a[]!=E)}"));
	}

	public void testFilterWithInheritance() throws Exception {
		Processor p = new Processor();
		Processor p1 = new Processor(p);
		Processor p2 = new Processor(p1);
		p.setProperty("a", "A,B,C,D");
		p.setProperty("b", "1");

		assertEquals("", p.getReplacer().process("${if;(a=A)}"));
		assertEquals("true", p.getReplacer().process("${if;(a[]=A)}"));
		assertEquals("true", p.getReplacer().process("${if;(a[]=B)}"));
		assertEquals("true", p.getReplacer().process("${if;(a[]=D)}"));
		assertEquals("", p.getReplacer().process("${if;(a[]=E)}"));
		assertEquals("", p.getReplacer().process("${if;(a[]!=E)}"));
	}

	public void testFilterExpressionWithReplacement() throws Exception {
		Processor p = new Processor();
		p.setProperty("a", "A");
		p.setProperty("b", "1");

		assertEquals("YES", p.getReplacer().process("${if;(a=A);YES}"));
		assertEquals("", p.getReplacer().process("${if;(a!=A);YES}"));
		assertEquals("YES", p.getReplacer().process("${if;(a=A);YES;NO}"));
		assertEquals("NO", p.getReplacer().process("${if;(a!=A);YES;NO}"));
	}

	public void testUnknownMacroDelimeters() throws IOException {
		Processor p = new Processor();
		assertEquals("${unknown}", p.getReplacer().process("${unknown}"));
		assertEquals("$<unknown>", p.getReplacer().process("$<unknown>"));
		assertEquals("$(unknown)", p.getReplacer().process("$(unknown)"));
		assertEquals("$[unknown]", p.getReplacer().process("$[unknown]"));
		assertEquals("$«unknown»", p.getReplacer().process("$«unknown»"));
		assertEquals("$‹unknown›", p.getReplacer().process("$‹unknown›"));
		assertTrue(p.check("No translation found for macro: unknown"));
	}

	public void testVersionMaskWithTarget() throws IOException {
		Processor p = new Processor();
		assertEquals("${version;===;$<@>}", p.getReplacer().process("${version;===;$<@>}"));
		assertTrue(p.check());
	}

	public void testVersionMaskWithoutTarget() throws IOException {
		Processor p = new Processor();
		assertEquals("${version;===}", p.getReplacer().process("${version;===}"));
		assertTrue(p.check());
	}

	public void testVersionMask() throws IOException {
		Processor p = new Processor();
		assertEquals("1.2.3", p.getReplacer().process("${version;===;1.2.3}"));
		assertTrue(p.check());
	}

	public void testVersionMaskWithSetExplicitTarget() throws IOException {
		Processor p = new Processor();
		p.setProperty("@", "1.2.3");
		assertEquals("1.2.3", p.getReplacer().process("${version;===;${@}}"));
		assertTrue(p.check());
	}

	public void testVersionMaskWithSetTarget() throws IOException {
		Processor p = new Processor();
		p.setProperty("@", "1.2.3");
		assertEquals("1.2.3", p.getReplacer().process("${version;===}"));
		assertTrue(p.check());
	}

	public void testRangeWithSetTarget() throws IOException {
		Processor p = new Processor();
		p.setProperty("@", "1.2.3");
		assertEquals("[1.2.3,2.2.3)", p.getReplacer().process("${range;[===,+===)}"));
		assertTrue(p.check());
	}

	public void testRangeWithSetExplicitTarget() throws IOException {
		Processor p = new Processor();
		p.setProperty("@", "1.2.3");
		assertEquals("[1.2.3,2.2.3)", p.getReplacer().process("${range;[===,+===);${@}}"));
		assertTrue(p.check());
	}

	public void testRangeWithTarget() throws IOException {
		Processor p = new Processor();
		assertEquals("${range;[===,+===)}", p.getReplacer().process("${range;[===,+===)}"));
		assertTrue(p.check());
	}

	public void testRangeWithExplicitTarget() throws IOException {
		Processor p = new Processor();
		assertEquals("${range;[===,+===);${@}}", p.getReplacer().process("${range;[===,+===);${@}}"));
		assertTrue(p.check());
	}

	public void testGlobToRegExp() {
		Processor p = new Processor();
		Macro m = p.getReplacer();
		assertEquals(".*x", m.process("${glob;*x}"));
		assertEquals("(?!.*x)", m.process("${glob;!*x}"));
	}

	/**
	 * A macro to get an attribute from a package
	 * 
	 * @throws Exception
	 */

	public static void testPackageAttribute() throws Exception {
		Builder builder = new Builder();
		builder.addClasspath(IO.getFile("jar/osgi.jar"));
		builder.setExportPackage("org.osgi.service.event;foo=3");
		builder.setProperty("Header-Version", "${packageattribute;org.osgi.service.event}");
		builder.setProperty("Header-Foo", "${packageattribute;org.osgi.service.event;from:}");
		builder.build();
		assertTrue(builder.check());

		Manifest m = builder.getJar().getManifest();
		String value = m.getMainAttributes().getValue("Header-Version");
		assertEquals("1.0.1", value);
		value = m.getMainAttributes().getValue("Header-Foo");
		assertNotNull(value);
	}
	/*
	 * #722 ${cat;<file>} removes \ before a $
	 */

	public void testCat() {
		Processor b = new Processor();
		b.setProperty("tst", "${cat;testresources/macro/cat-test.txt}");
		String tst = b.getProperty("tst");
		assertEquals("This is a \\$ backslashed dollar\n", tst);
	}

	/*
	 * #761 Tstamp consistent
	 */

	public void testTstampConsistent() throws Exception {
		Processor top = new Processor();
		Processor base = new Processor(top);

		base.setProperty("time", "${tstamp;S}");
		String start = base.getProperty("time");
		Thread.sleep(10);
		String end = base.getProperty("time");
		assertFalse(start.equals(end));

		top.setProperty("_@tstamp", end);

		start = base.getProperty("time");
		assertTrue(start.equals(end));
		Thread.sleep(10);
		end = base.getProperty("time");
		assertTrue(start.equals(end));
	}

	/**
	 * Combine
	 */

	public void testFuntionMacrosAndReplace() throws Exception {
		Processor processor = new Processor();
		processor.setProperty("libs", "/lib/a.jar, /lib/b.jar");
		processor.setProperty("foo", "--${1}--");
		processor.setProperty("xlibs", "${replace;${libs};/lib/(.*).jar;$0=${foo;$1}}");

		assertEquals("/lib/a.jar=--a--, /lib/b.jar=--b--", processor.getProperty("xlibs"));
	}

	/**
	 * File name tests
	 * 
	 * @throws Exception
	 */

	public void testFileNameMacros() throws Exception {
		Processor processor = new Processor();
		File a = IO.getFile("testresources/testfilenamemacros.properties");
		processor.setProperties(a);

		File b = IO.getFile(processor._thisfile(new String[0]));
		assertEquals(a, b);

		assertEquals("properties", processor.getReplacer()._extension(new String[] {
				"", "testresources/testfilenamemacros.properties"
		}));

		assertEquals("testfilenamemacros.properties",
				processor.getReplacer().process("${basename;testfilenamemacros.properties}"));
		assertEquals("testfilenamemacros", processor.getReplacer().process("${stem;testfilenamemacros.properties}"));
	}

	/**
	 * List functions
	 */
	public void testMacroLists() throws Exception {
		Processor processor = new Processor();

		assertEquals("true", processor.getReplacer().process("${apply;isnumber;1,2,3,4}"));
		assertEquals("10", processor.getReplacer().process("${apply;sum;1,2,3,4}"));
		assertEquals("false", processor.getReplacer().process("${apply;isnumber;1,2,3,a,4}"));

		processor.setProperty("double", "${1}${1}");
		processor.setProperty("mulbyindex", "${js;${1}*${2}}");
		assertEquals("A,B,C,D,E,F", processor.getReplacer().process("${map;toupper;a, b, c, d, e, f}"));
		assertEquals("aa,bb,cc,dd,ee,ff", processor.getReplacer().process("${map;double;a, b, c, d, e, f}"));
		assertEquals("0,2,6,12,20,30,42,56,72,90",
				processor.getReplacer().process("${foreach;mulbyindex;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));

		assertEquals("6", processor.getReplacer().process("${size;a, b, c, d, e, f}"));
		assertEquals("0", processor.getReplacer().process("${size;}"));

		assertEquals("d", processor.getReplacer().process("${get;3;a, b, c, d, e, f}"));
		assertEquals("d", processor.getReplacer().process("${get;-3;a, b, c, d, e, f}"));
		assertEquals("f", processor.getReplacer().process("${get;-1;a, b, c, d, e, f}"));

		assertEquals("b,c", processor.getReplacer().process("${sublist;1;3;a, b, c, d, e, f}"));
		assertEquals("e,f", processor.getReplacer().process("${sublist;-1;-3;a, b, c, d, e, f}"));

		assertEquals("a", processor.getReplacer().process("${first;a, b, c, d, e, f}"));
		assertEquals("", processor.getReplacer().process("${first;}"));
		assertEquals("f", processor.getReplacer().process("${last;a, b, c, d, e, f}"));
		assertEquals("", processor.getReplacer().process("${last;}"));

		assertEquals("5", processor.getReplacer().process("${indexof;6;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));
		assertEquals("-1", processor.getReplacer().process("${indexof;60;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));

		assertEquals("9", processor.getReplacer().process("${lastindexof;7;1, 2, 3, 4, 5, 6, 7, 7, 7, 10}"));

		assertEquals("10,9,8,7,6,5,4,3,2,1",
				processor.getReplacer().process("${reverse;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));

		assertEquals("55", processor.getReplacer().process("${sum;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));
		assertEquals("55", processor.getReplacer().process("${sum;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));

		assertEquals("5.5", processor.getReplacer().process("${average;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));

		assertEquals("-16", processor.getReplacer().process("${nmin;2, 0, -13, 40, 55, -16, 700, -8, 9, 10}"));
		assertEquals("-16", processor.getReplacer().process("${nmin;2; 0; -13; 40 ; 55 ; -16; 700; -8; 9; 10}"));

		assertEquals("700", processor.getReplacer().process("${nmax;2; 0, -13, 40, 55, -16, 700, -8, 9, 10}"));
		assertEquals("700", processor.getReplacer().process("${nmax;2; 0; -13; 40; 55; -16; 700, -8, 9, 10}"));

		assertEquals("-13", processor.getReplacer().process("${min;2; 0; -13; 40; 55; -16; 700, -8, 9, 10}"));
		assertEquals("9", processor.getReplacer().process("${max;2; 0, -13, 40, 55, -16, 700, -8, 9, 10}"));
	}

	/**
	 * String functions
	 */

	public void testMacroStrings() throws Exception {
		Processor processor = new Processor();
		processor.setProperty("empty", "");

		assertEquals("6", processor.getReplacer().process("${length;abcdef}"));

		assertEquals("true", processor.getReplacer().process("${is;1.3;1.3;1.3}"));
		assertEquals("false", processor.getReplacer().process("${is;abc;1.3}"));

		assertEquals("true", processor.getReplacer().process("${isnumber;1.3}"));
		assertEquals("false", processor.getReplacer().process("${isnumber;abc}"));

		assertEquals("true", processor.getReplacer().process("${isempty;${empty}}"));
		assertEquals("true", processor.getReplacer().process("${isempty;${empty};${empty};${empty};${empty};}"));
		assertEquals("false", processor.getReplacer().process("${isempty;abc}"));

		assertEquals("\n000010", processor.getReplacer().process("${format;\n%06d;10}"));
		assertEquals("000010", processor.getReplacer().process("${format;%1$06d;10}"));
		assertEquals("2e C8 300 620", processor.getReplacer().process("${format;%x %X %d %o;46;200;300;400;500}"));
		assertEquals("+00010", processor.getReplacer().process("${format;%+06d;10}"));
		assertEquals(String.format("%,6d", 100000), processor.getReplacer().process("${format;%,6d;100000}"));

		assertEquals("xyz", processor.getReplacer().process("${trim; \txyz\t  }"));

		assertEquals("bcdef", processor.getReplacer()
			.process("${subst;abacdaef;a}"));
		assertEquals("DEFbDEFcdDEFef", processor.getReplacer().process("${subst;abacdaef;a;DEF}"));
		assertEquals("DEFbacdaef", processor.getReplacer().process("${subst;abacdaef;a;DEF;1}"));
		assertEquals("DEFbDEFcdaef", processor.getReplacer().process("${subst;abacdaef;a;DEF;2}"));
		assertEquals("DEFbDEFcdDEFef", processor.getReplacer().process("${subst;abacdaef;a;DEF;3}"));
		assertEquals("DEFbDEFcdDEFef", processor.getReplacer().process("${subst;abacdaef;a;DEF;300}"));

		assertEquals("true", processor.getReplacer().process("${matches;aaaabcdef;[a]+bcdef}"));
		assertEquals("false", processor.getReplacer().process("${matches;bcdef;[a]+bcdef}"));

		assertEquals("-1", processor.getReplacer().process("${ncompare;2;200}"));
		assertEquals("1", processor.getReplacer().process("${ncompare;200;1}"));
		assertEquals("0", processor.getReplacer().process("${ncompare;200;200}"));

		assertEquals("-1", processor.getReplacer().process("${compare;abc;def}"));
		assertEquals("1", processor.getReplacer().process("${compare;def;abc}"));
		assertEquals("0", processor.getReplacer().process("${compare;abc;abc}"));

		assertEquals("ABCDEF", processor.getReplacer().process("${toupper;abcdef}"));
		assertEquals("abcdef", processor.getReplacer().process("${tolower;ABCDEF}"));

		assertEquals("ab,efab,ef", processor.getReplacer().process("${split;cd;abcdefabcdef}"));
		assertEquals("ab,d,fab,d,f", processor.getReplacer().process("${split;[ce];abcdefabcdef}"));

		assertEquals("3", processor.getReplacer().process("${find;abcdef;def}"));
		assertEquals("-1", processor.getReplacer().process("${find;abc;defxyz}"));
		assertEquals("9", processor.getReplacer().process("${findlast;def;abcdefabcdef}"));

		assertEquals("abcdef", processor.getReplacer().process("${startswith;abcdef;abc}"));
		assertEquals("", processor.getReplacer().process("${startswith;abcdef;xyz}"));

		assertEquals("abcdef", processor.getReplacer().process("${endswith;abcdef;def}"));
		assertEquals("", processor.getReplacer().process("${endswith;abcdef;xyz}"));

		assertEquals("abcdef", processor.getReplacer().process("${endswith;abcdef;def}"));
		assertEquals("", processor.getReplacer().process("${endswith;abcdef;xyz}"));

		assertEquals("def", processor.getReplacer().process("${extension;abcdef.def}"));
		assertEquals("", processor.getReplacer().process("${extension;abcdefxyz}"));

		assertEquals("abc", processor.getReplacer().process("${substring;abcdef;0;3}"));
		assertEquals("abc", processor.getReplacer().process("${substring;abcdef;;3}"));
		assertEquals("def", processor.getReplacer().process("${substring;abcdef;-3}"));
		assertEquals("de", processor.getReplacer().process("${substring;abcdef;-3;-1}"));
		assertEquals("def", processor.getReplacer().process("${substring;abcdef;3}"));

		assertEquals("6", processor.getReplacer().process("${length;abcdef}"));
		assertEquals("0", processor.getReplacer().process("${length;}"));

	}

	/**
	 * Test rand
	 */

	public void testRan() {
		Processor processor = new Processor();
		for (int i = 0; i < 1000; i++) {
			int value = Integer.parseInt(processor.getReplacer().process("${rand;-10;10}"));
			assertTrue(value >= -10 && value <= 10);
		}
	}

	/**
	 * Test Javascript stuff
	 */

	public void testJSSimple() {
		Processor processor = new Processor();
		processor.setProperty("alpha", "25");
		assertEquals("3", processor.getReplacer().process("${js;1+2;}"));
		assertEquals("25", processor.getReplacer().process("${js;domain.get('alpha');}"));
		assertEquals("5", processor.getReplacer().process("${js;domain.get('alpha')/5;}"));

	}

	/**
	 * Check if we can initialize
	 */
	public void testJSINit() {
		Processor processor = new Processor();
		processor.setProperty("javascript", "function top() { return 13; }");
		assertEquals("16", processor.getReplacer().process("${js;1+2+top()}"));
	}

	/**
	 * See if the initcode is concatenated correctly
	 */
	public void testJSINit2() {
		Processor processor = new Processor();
		processor.setProperty("javascript", "function top() { return 1; }");
		processor.setProperty("javascript.1", "function top() { return 2; }");
		processor.setProperty("javascript.2", "function top() { return 3; }");
		assertEquals("3", processor.getReplacer().process("${js;top()}"));
	}

	/**
	 * Test control characters
	 */
	public void testControlCharacters() throws Exception {
		Processor p = new Processor();
		p.setProperty("a", "a, b, c");
		String s = p.getReplacer().process("${unescape;${replace;${a};(.+);$0;\\n}}\n");
		assertEquals("a\nb\nc\n", s);
	}

	/**
	 * Test the custom macros
	 */

	public void testCustomMacros() {
		Processor x = new Processor();
		x.setProperty("foo", "Hello ${1}");
		assertEquals("Hello Peter", x.getReplacer().process("${foo;Peter}"));

		assertTemplate("this is 1 abc, and this is def", "this is 1 ${1}, and this is ${2}", "abc;def");
		assertTemplate("abc,def", "${#}", "abc;def");
		assertTemplate("osgi.ee;filter:='(&(osgi.ee=JavaSE)(version=1.6))'",
				"osgi.ee;filter:='(&(osgi.ee=JavaSE)(version=1.${1}))'", "6");
	}

	void assertTemplate(String result, String template, String params) {
		Processor top = new Processor();
		top.setProperty("template", template);
		top.setProperty("macro", "${template;" + params + "}");
		String expanded = top.getProperty("macro");
		assertEquals(result, expanded);
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
		String s = proc.getReplacer().process("${env;PATH}");
		assertNotNull(s);
		assertTrue(s.length() > 0);
	}

	public static void testEnvAlt() {
		Processor proc = new Processor();
		String s = proc.getReplacer().process("${env;FOOBAR;hello}");
		assertEquals("hello", s);
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
		if (!"/".equals(File.separator))
			return;

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

		//
		// Add the S modifier. If qualifier is SNAPSHOT, it will return a
		// maven version
		//

		assertEquals("1.2.3-SNAPSHOT", macro.process("${version;===S;1.2.3.SNAPSHOT}"));
		assertEquals("1.2.3-SNAPSHOT", macro.process("${version;===s;1.2.3.SNAPSHOT}"));
		assertEquals("1.2.3.SNAPSHOT", macro.process("${version;====;1.2.3.SNAPSHOT}"));
		assertEquals("1.2.3-SNAPSHOT", macro.process("${version;===S;1.2.3.BUILD-SNAPSHOT}"));
		assertEquals("1.2.3-SNAPSHOT", macro.process("${version;===s;1.2.3.BUILD-SNAPSHOT}"));
		assertEquals("1.2.3.BUILD-SNAPSHOT", macro.process("${version;====;1.2.3.BUILD-SNAPSHOT}"));
		assertEquals("1.2.3.X", macro.process("${version;===S;1.2.3.X}"));
		assertEquals("1.2.3", macro.process("${version;===s;1.2.3.X}"));
		assertEquals("1.2.3.X", macro.process("${version;====;1.2.3.X}"));
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
		String a = macro.process("${lsr;src/test;*.java}");
		assertTrue(a.contains("MacroTest.java"));
		assertTrue(a.contains("ManifestTest.java"));
		assertFalse(a.contains("bnd.info"));
		assertFalse(a.contains("com.acme"));
		assertFalse(a.contains("src/test/MacroTest.java"));
		assertFalse(a.contains("src/test/ManifestTest.java"));

		String b = macro.process("${lsa;src/test;*.java}");
		assertTrue(b.contains("src/test/MacroTest.java"));
		assertTrue(b.contains("src/test/ManifestTest.java"));
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

		// Why Tokyo? Japan doesn't use daylight savings, so the test shouldn't
		// break when clocks change.
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
				IO.getFile("jar/asm.jar")
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
		p.setProperty("specs", "a0,b0, c0,    d0");
		Macro m = new Macro(p);
		assertEquals("xa0y, xb0y, xc0y, xd0y", m.process("${replace;${specs};([^\\s]+);x$1y}"));
		assertEquals("a, b, c, d", m.process("${replace;${specs};0}"));
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
		try (Analyzer analyzer = new Analyzer()) {
			analyzer.setJar(IO.getFile("jar/asm.jar"));
			Macro m = new Macro(analyzer);

			assertTrue(m.process("${findname;(.*)\\.class;$1.xyz}").contains("FieldVisitor.xyz,"));
			assertTrue(m.process("${findname;(.*)\\.class;$1.xyz}").contains("MethodVisitor.xyz,"));
			assertTrue(
					m.process("${findpath;(.*)\\.class}").contains("org/objectweb/asm/AnnotationVisitor.class,"));
			assertTrue(m.process("${findpath;(.*)\\.class}").contains("org/objectweb/asm/ByteVector.class, org/objectweb/asm/ClassAdapter.class,"));
			assertEquals("META-INF/MANIFEST.MF", m.process("${findpath;META-INF/MANIFEST.MF}"));
			assertEquals("Label.class", m.process("${findname;Label\\..*}"));
			assertEquals("Adapter, Visitor, Writer", m.process("${findname;Method(.*)\\.class;$1}"));
		}
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

		assertTrue("xw", p.getWarnings().get(0).endsWith("xw"));
		assertTrue("1", p.getWarnings().get(1).endsWith("1"));
		assertTrue("2", p.getWarnings().get(2).endsWith("2"));
		assertTrue("3 333", p.getWarnings().get(3).endsWith("3 333"));

		assertTrue("xw", p.getErrors().get(0).endsWith("xe"));
		assertTrue("1", p.getErrors().get(1).endsWith("1"));
		assertTrue("2", p.getErrors().get(2).endsWith("2"));
		assertTrue("3 333", p.getErrors().get(3).endsWith("3 333"));
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

		assertEquals("aaXbbXccXddXeeXff", m.process("${sjoin;X;aa,bb,cc,dd,ee,ff}"));
		assertEquals("aa\nbb\ncc\ndd\nee\nff", m.process("${sjoin;\n;aa,bb,cc;dd,ee,ff}"));
		assertEquals("aa\nbb\ncc\ndd\nee\nff", m.process("${unescape;${sjoin;\\n;aa,bb,cc;dd,ee,ff}}"));
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
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.addClasspath(IO.getFile("jar/ds.jar"));
		b.setProperty("Export-Package", "org.eclipse.*, org.osgi.*");
		b.setProperty("fwusers", "${classes;importing;org.osgi.framework}");
		b.setProperty("foo", "${filterout;${fwusers};org\\.osgi\\..*}");
		b.build();
		String fwusers = b.getProperty("fwusers");
		String foo = b.getProperty("foo");
		assertTrue(fwusers.length() > foo.length());
		assertTrue(fwusers.contains("org.osgi.framework.ServicePermission"));
		assertTrue(fwusers.contains("org.eclipse.equinox.ds.instance.BuildDispose"));
		assertFalse(foo.contains("org.osgi.framework.ServicePermission"));
		assertTrue(foo.contains("org.eclipse.equinox.ds.instance.BuildDispose"));
		System.err.println(b.getProperty("fwusers"));
		System.err.println(b.getProperty("foo"));

	}

	public static void testPackagesMacro() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new Jar[] {
				new Jar(IO.getFile("bin"))
		});
		b.setProperty("Private-Package",
				"test.packageinfo.annotated,test.packageinfo.notannotated,test.packageinfo.nopackageinfo,test.activator");
		b.setProperty("All-Packages", "${packages}");
		b.setProperty("Annotated", "${packages;annotated;test.packageinfo.annotated.BlahAnnotation}");
		b.setProperty("Named", "${packages;named;*.notannotated}");
		b.setProperty("Negated", "${packages;named;!*.no*}");
		b.setProperty("Versioned", "${packages;versioned}");
		b.build();

		assertEquals(0, b.getErrors().size());

		assertEquals(
				"test.packageinfo.annotated,test.packageinfo.notannotated,test.packageinfo.nopackageinfo,test.activator",
				b.getProperty("All-Packages"));
		assertEquals("test.packageinfo.annotated", b.getProperty("Annotated"));
		assertEquals("test.packageinfo.notannotated", b.getProperty("Named"));
		assertEquals("test.packageinfo.annotated,test.activator", b.getProperty("Negated"));
		assertEquals("test.packageinfo.annotated,test.packageinfo.notannotated", b.getProperty("Versioned"));
	}

	public void testBase64() {
		Processor b = new Processor();
		b.setProperty("b64", "${base64;testresources/macro/base64-test.gif}");
		String b64 = "R0lGODlhBwAIAKIAANhCT91bZuN3gOeIkOiQl+ygp////////yH5BAEAAAcALAAAAAAHAAgAAAMXCLoqFUWoYogpKlgS8u4AZWGAAw0MkwAAOw==";
		assertEquals(b64, b.getProperty("b64"));
	}

	public void testDigest() {
		Processor b = new Processor();
		b.setProperty("a", "${digest;SHA-256;testresources/macro/digest-test.jar}");
		b.setProperty("b", "${digest;MD5;testresources/macro/digest-test.jar}");

		assertEquals("3B21F1450430C0AFF57E12A338EF6AA1A2E0EE318B8883DD196048450C2FC1FC", b.getProperty("a"));
		assertEquals("F31BAC7F1F70E5D8705B98CC0FBCFF5E", b.getProperty("b"));
	}

	public void testProcessNullValue() throws Exception {
		try (Processor b = new Processor()) {
			Macro m = b.getReplacer();
			String tst = m.process(null);
			assertEquals("", tst);
			assertTrue(b.check());
		}
	}

	public void testNonStringValue() throws Exception {
		try (Processor b = new Processor()) {
			// getProperty will return null for non-String value
			b.getProperties().put("tst", new StringBuilder("foo"));
			b.getProperties().put("num", 2);
			String tst = b.getProperty("tst");
			assertNull(tst);
			String num = b.getProperty("num");
			assertNull(num);
			assertTrue(b.check("Key 'tst' has a non-String value", "Key 'num' has a non-String value"));
		}
	}

	public void testNonStringFlattenedValue() throws Exception {
		try (Processor b = new Processor()) {
			// getProperty will return null for non-String value
			b.getProperties().put("tst", new StringBuilder("foo"));
			b.getProperties().put("num", 2);
			Properties f = b.getFlattenedProperties();
			String tst = f.getProperty("tst");
			assertNull(tst);
			String num = f.getProperty("num");
			assertNull(num);
			assertTrue(b.check("Key 'tst' has a non-String value", "Key 'num' has a non-String value"));
		}
	}
}
