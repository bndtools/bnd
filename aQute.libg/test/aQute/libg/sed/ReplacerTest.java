package aQute.libg.sed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;

import aQute.libg.reporter.ReporterAdapter;

public class ReplacerTest {

	static class Processor extends ReporterAdapter implements Domain {
		final Map<String, String>	map	= new HashMap<>();
		final Domain				parent;
		final ReplacerAdapter		replacer;

		public Processor(Processor parent) {
			this.parent = parent;
			this.replacer = new ReplacerAdapter(this);
			this.replacer.setReporter(this);
		}

		public Processor() {
			this(null);
		}

		@Override
		public Map<String, String> getMap() {
			return map;
		}

		@Override
		public Domain getParent() {
			return parent;
		}

		String getProcessed(String key) {
			return replacer.getProcessed(key);
		}

		public ReplacerAdapter getReplacer() {
			return replacer;
		}

		public String process(String string) {
			return getReplacer().process(string);
		}

		@Override
		public String toString() {
			return map.toString();
		}
	}

	/**
	 * Test non-string returns
	 */

	@Test
	public void testNonStrings() {
		Processor top = new Processor();
		top.getMap()
			.put("p", "${processors}");
		Integer n = Integer.parseInt(top.getProcessed("p"));
		assertTrue(n >= 1);
	}

	/**
	 * Test replacement of ./ with cwd
	 */

	@Test
	public void testCurrentWorkingDirectory() {
		Processor top = new Processor();
		top.getMap()
			.put("cwd.1", "./"); // empty
		top.getMap()
			.put("cwd.2", " ./"); // empty
		top.getMap()
			.put("cwd.3", "./ "); // empty
		top.getMap()
			.put("cwd.4", " ./ "); // empty
		top.getMap()
			.put("cwd.5", "|./|"); // empty
		top.getMap()
			.put("cwd.6", "/.//"); // empty
		top.getMap()
			.put("cwd.7", "."); // empty
		top.getMap()
			.put("cwd.8", " . "); // empty
		top.getMap()
			.put("cwd.9", " . /"); // empty
		top.getMap()
			.put("cwd.10", " ."); // empty
		top.getMap()
			.put("cwd.11", "| ./|"); // empty
		top.getMap()
			.put("cwd.12", "|\t./|"); // empty
		top.getMap()
			.put("cwd.13", "|\r./|"); // empty
		top.getMap()
			.put("cwd.14", "|\n./|"); // empty

		String cwd = new File("").getAbsolutePath() + "/";

		assertEquals(" . ", top.getProcessed("cwd.8"));
		assertEquals(cwd, top.getProcessed("cwd.1"));
		assertEquals(" " + cwd, top.getProcessed("cwd.2"));
		assertEquals(cwd + " ", top.getProcessed("cwd.3"));
		assertEquals(" " + cwd + " ", top.getProcessed("cwd.4"));
		assertEquals("|./|", top.getProcessed("cwd.5"));
		assertEquals("/.//", top.getProcessed("cwd.6"));
		assertEquals(".", top.getProcessed("cwd.7"));
		assertEquals(" . /", top.getProcessed("cwd.9"));
		assertEquals(" .", top.getProcessed("cwd.10"));
		assertEquals("| " + cwd + "|", top.getProcessed("cwd.11"));
		assertEquals("|\t" + cwd + "|", top.getProcessed("cwd.12"));
		assertEquals("|\r" + cwd + "|", top.getProcessed("cwd.13"));
		assertEquals("|\n" + cwd + "|", top.getProcessed("cwd.14"));

		top.check();
	}

	/**
	 * Test if $if accepts isdir
	 */

	@Test
	public void testifDir() {
		Processor top = new Processor();
		top.getMap()
			.put("presentd", "${if;${isdir;src};YES;NO}");
		top.getMap()
			.put("presentd", "${if;${isdir;test};YES;NO}");
		top.getMap()
			.put("absentd", "${if;${isdir;xxx};YES;NO}");
		top.getMap()
			.put("wrongd", "${if;${isdir;bnd.bnd};YES;NO}");
		assertEquals("YES", top.getProcessed("presentd"));
		assertEquals("NO", top.getProcessed("wrongd"));
		assertEquals("NO", top.getProcessed("absentd"));
		top.getMap()
			.put("presentf", "${if;${isfile;bnd.bnd};YES;NO}");
		top.getMap()
			.put("absentf", "${if;${isfile;xxx};YES;NO}");
		top.getMap()
			.put("wrongf", "${if;${isfile;jar};YES;NO}");
		assertEquals("YES", top.getProcessed("presentf"));
		assertEquals("NO", top.getProcessed("absentf"));
		assertEquals("NO", top.getProcessed("wrongf"));
		top.check();
	}

	/**
	 * Test the combine macro that groups properties
	 */

	@Test
	public void testWildcardKeys() {
		Processor top = new Processor();
		top.getMap()
			.put("a.3", "a.3");
		top.getMap()
			.put("a.1", "a.1");
		top.getMap()
			.put("a.2", "a.2");
		top.getMap()
			.put("a.4", "a.4");
		top.getMap()
			.put("aa", "${a.*}");
		assertEquals("a.1,a.2,a.3,a.4", top.getProcessed("a.*"));
		assertEquals("a.1,a.2,a.3,a.4", top.getProcessed("aa"));
		top.check();
	}

	@Test
	public void testEnv() {
		Processor top = new Processor();
		String s = top.getReplacer()
			.process("${env;USER}");
		assertNotNull(s);
		top.check();
	}

	/**
	 * Test the random macro
	 */
	@Test
	public void testRandom() {
		Processor top = new Processor();
		top.getMap()
			.put("a", "${random}");
		top.getMap()
			.put("a12", "${random;12}");
		String a = top.getProcessed("a");
		System.err.println(a);
		assertEquals(8, a.length());
		String a12 = top.getProcessed("a12");
		System.err.println(a12);
		assertEquals(12, a12.length());
		assertNotSame(a, a12);
		top.check();
	}

	/**
	 * Testing an example with nesting that was supposd not to work
	 */

	@Test
	public void testSuper() {
		Processor top = new Processor();
		Processor middle = new Processor(top);
		Processor bottom = new Processor(middle);

		top.getMap()
			.put("a", "top.a");
		top.getMap()
			.put("b", "top.b");
		top.getMap()
			.put("c", "top.c");
		top.getMap()
			.put("Bundle-Version", "0.0.0");
		middle.getMap()
			.put("a", "middle.a");
		middle.getMap()
			.put("b", "${^a}");
		middle.getMap()
			.put("c", "-${^c}-");
		middle.getMap()
			.put("Bundle-Version", "${^Bundle-Version}");
		assertEquals("middle.a", bottom.getProcessed("a"));
		assertEquals("top.a", bottom.getProcessed("b"));
		assertEquals("-top.c-", bottom.getProcessed("c"));
		assertEquals("0.0.0", bottom.getProcessed("Bundle-Version"));
		top.check();
		middle.check();
		bottom.check();
	}

	/**
	 * Testing an example with nesting that was supposd not to work
	 */

	@Test
	public void testNesting2() {
		Processor p = new Processor();
		p.getMap()
			.put("groupId", "com.trivadis.tomas");
		p.getMap()
			.put("artifactId", "common");
		p.getMap()
			.put("bsn", "${if;${symbolicName};${symbolicName};${groupId}.${artifactId}}");
		p.getMap()
			.put("Bundle-SymbolicName", "${bsn}");
		p.getMap()
			.put("symbolicName", "");

		// Not set, so get the maven name
		assertEquals("com.trivadis.tomas.common", p.getProcessed("Bundle-SymbolicName"));

		// Set it
		p.getMap()
			.put("symbolicName", "testing");
		assertEquals("testing", p.getProcessed("Bundle-SymbolicName"));

		// And remove it
		p.getMap()
			.put("symbolicName", "");
		assertEquals("com.trivadis.tomas.common", p.getProcessed("Bundle-SymbolicName"));
		p.check();
	}

	/**
	 * Verify system command
	 */

	@Test
	@DisabledOnOs(WINDOWS)
	public void testSystem() throws Exception {
		Processor p = new Processor();
		assertEquals("Hello World", p.process("${system;echo Hello World}"));
		assertTrue(p.process("${system;wc;Hello World}")
			.matches("\\s*[0-9]+\\s+[0-9]+\\s+[0-9]+\\s*"));
		p.check();
	}

	@Test
	public void testSystemFail() throws Exception {
		Processor p = new Processor();
		String cmd = "${system;mostidioticcommandthatwillsurelyfail}";
		assertTrue(p.process(cmd)
			.startsWith("${system;"));
		p.check();
	}

	/**
	 * Verify system-allow-fail command
	 */

	@Test
	public void testSystemAllowFail() throws Exception {
		Processor p = new Processor();
		assertEquals("", p.process("${system-allow-fail;mostidioticcommandthatwillsurelyfail}"));
		p.check();
	}

	/**
	 * Check that variables override macros.
	 */
	@Test
	public void testPriority() {
		Processor p = new Processor();
		p.getMap()
			.put("now", "not set");
		assertEquals("not set", p.process("${now}"));
		p.check();
	}

	@Test
	public void testNames() {
		Processor p = new Processor();
		p.getMap()
			.put("a", "a");
		p.getMap()
			.put("aa", "aa");

		assertEquals("aa", p.process("${${a}${a}}"));
		p.check();
	}

	/**
	 * Test the wc function
	 */

	@Test
	public void testWc() {
		String pckg = ReplacerTest.class.getPackage()
			.getName()
			.replace('.', '/');

		Processor p = new Processor();
		String a = p.process("${lsr;test/" + pckg + ";*.java}");
		assertTrue(a.contains("ReplacerTest.java"));
		assertFalse(a.contains("test/" + pckg + "/ReplacerTest.java"));

		String b = p.process("${lsa;test/" + pckg + ";*.java}");
		assertTrue(b.contains("test/" + pckg + "/ReplacerTest.java"));
		p.check();
	}

	/**
	 * Check the uniq command
	 */

	@Test
	public void testUniq() {
		Processor p = new Processor();
		p.getMap()
			.put("a", "${uniq;1}");
		p.getMap()
			.put("b", "${uniq;1,2}");
		p.getMap()
			.put("c", "${uniq;1;2}");
		p.getMap()
			.put("d", "${uniq;1; 1,  2 , 3}");
		p.getMap()
			.put("e", "${uniq;1; 1 , 2 ;      3;3,4,5,6}");
		assertEquals("1,2,3", p.getProcessed("d"));
		assertEquals("1,2", p.getProcessed("b"));
		assertEquals("1", p.getProcessed("a"));
		assertEquals("1,2", p.getProcessed("c"));
		assertEquals("1,2,3", p.getProcessed("d"));
		assertEquals("1,2,3,4,5,6", p.getProcessed("e"));
		p.check();
	}

	/**
	 * Test arguments with difficult characters like ;
	 */

	@Test
	public void testEscapedArgs() {
		Processor p = new Processor();
		p.getMap()
			.put("x", "${replace;1,2,3;.+;$0\\;version=1}");
		assertEquals("1;version=1,2;version=1,3;version=1", p.getProcessed("x"));
		p.check();
	}

	/**
	 * Check if variables that contain variables, ad nauseum, really wrk
	 */
	@Test
	public void testNested() {
		Processor p = new Processor();
		p.getMap()
			.put("a", ".");
		p.getMap()
			.put("b", "${a}");
		p.getMap()
			.put("c", "${b}");

		p.getMap()
			.put("d", "${tstamp;${format};UTC;${aug152008}}");
		p.getMap()
			.put("format", "yyyy");
		p.getMap()
			.put("aug152008", "1218810097322");

		p.getMap()
			.put("f", "${d}");
		p.getMap()
			.put("aug152008", "1218810097322");

		assertEquals(".", p.getProcessed("c"));
		assertEquals("2008", p.getProcessed("d"));
		assertEquals(p.getProcessed("f"), p.getProcessed("d"));
		p.check();
	}

	@Test
	public void testLoop() {
		Processor p = new Processor();
		p.getMap()
			.put("a", "${b}");
		p.getMap()
			.put("b", "${a}");

		p.getMap()
			.put("d", "${e}");
		p.getMap()
			.put("e", "${f}");
		p.getMap()
			.put("f", "${g}");
		p.getMap()
			.put("g", "${h}");
		p.getMap()
			.put("h", "${d}");

		assertEquals("${infinite:[a,b]}", p.getProcessed("a"));
		assertEquals("${infinite:[d,e,f,g,h]}", p.getProcessed("d"));
		p.check();
	}

	@Test
	public void testTstamp() {
		String aug152008 = "1218810097322";
		Processor p = new Processor();
		assertEquals("200808151421", p.process("${tstamp;yyyyMMddHHmm;UTC;" + aug152008 + "}"));
		assertEquals("200808151521", p.process("${tstamp;yyyyMMddHHmm;GMT+01;" + aug152008 + "}"));
		assertEquals("2008", p.process("${tstamp;yyyy;UTC;" + aug152008 + "}"));

		// Why Tokyo? Japan doesn't use daylight savings, so the test shouldn't
		// break when clocks change.
		assertEquals("200808152321", p.process("${tstamp;yyyyMMddHHmm;Asia/Tokyo;" + aug152008 + "}"));
		p.check();
	}

	@Test
	public void testIsfile() {
		Processor p = new Processor();
		assertEquals("true", p.process("${isfile;.project}"));
		assertEquals("false", p.process("${isfile;thisfiledoesnotexist}"));
		p.check();
	}

	@Test
	public void testParentFile() {
		Processor p = new Processor();
		assertTrue(p.process("${dir;.project}")
			.endsWith("aQute.libg"));
		p.check();
	}

	@Test
	public void testBasename() {
		Processor p = new Processor();
		assertEquals("aQute.libg", p.process("${basename;${dir;.project}}"));
		p.check();
	}

	/**
	 * Check if we can check for the defintion of a variable
	 */

	@Test
	public void testDef() {
		Processor p = new Processor();
		p.getMap()
			.put("set.1", "1");
		p.getMap()
			.put("set.2", "2");
		assertEquals("NO", p.process("${if;${def;set.3};YES;NO}"));
		assertEquals("YES", p.process("${if;${def;set.1};YES;NO}"));
		assertEquals("YES", p.process("${if;${def;set.2};YES;NO}"));
		p.check();
	}

	/**
	 * NEW
	 */
	@Test
	public void testReplace() {
		Processor p = new Processor();
		p.getMap()
			.put("specs", "a,b, c,    d");
		assertEquals("xay,xby,xcy,xdy", p.process("${replace;${specs};([^\\s]+);x$1y}"));
		p.check();
	}

	@Test
	public void testToClassName() {
		Processor p = new Processor();
		assertEquals("com.acme.test.Test", p.process("${toclassname;com/acme/test/Test.class}"));
		assertEquals("Test", p.process("$<toclassname;Test.class>"));
		assertEquals("Test,com.acme.test.Test", p.process("${toclassname;Test.class, com/acme/test/Test.class}"));
		assertEquals("", p.process("$(toclassname;Test)"));
		assertEquals("com/acme/test/Test.class", p.process("$[toclasspath;com.acme.test.Test]"));
		assertEquals("Test.class", p.process("${toclasspath;Test}"));
		assertEquals("Test.class,com/acme/test/Test.class", p.process("${toclasspath;Test,com.acme.test.Test}"));
		p.check();
	}

	@Test
	public void testWarning() {
		Processor p = new Processor();
		p.getMap()
			.put("three", "333");
		p.getMap()
			.put("empty", "");
		p.getMap()
			.put("real", "true");

		p.process("    ${warning;xw;1;2;3 ${three}}");
		p.process("    ${error;xe;1;2;3 ${three}}");
		p.process("    ${if;1;$<a>}");
		assertEquals("xw", p.getWarnings()
			.get(0));
		assertEquals("1", p.getWarnings()
			.get(1));
		assertEquals("2", p.getWarnings()
			.get(2));
		assertEquals("3 333", p.getWarnings()
			.get(3));

		assertEquals("xe", p.getErrors()
			.get(0));
		assertEquals("1", p.getErrors()
			.get(1));
		assertEquals("2", p.getErrors()
			.get(2));
		assertEquals("3 333", p.getErrors()
			.get(3));
	}

	@Test
	public void testNestedReplace() {
		Processor p = new Processor();
		String value = p.process("xx$(replace;1.2.3-SNAPSHOT;(\\d(\\.\\d)+).*;$1)xx");
		System.err.println(p.getWarnings());
		assertEquals("xx1.2.3xx", value);

		assertEquals("xx1.222.3xx", p.process("xx$(replace;1.222.3-SNAPSHOT;(\\d+(\\.\\d+)+).*;$1)xx"));

		p.getMap()
			.put("a", "aaaa");
		assertEquals("[cac]", p.process("$[replace;acaca;a(.*)a;[$1]]"));
		assertEquals("xxx", p.process("$(replace;yxxxy;[^x]*(x+)[^x]*;$1)"));
		assertEquals("xxx", p.process("$(replace;yxxxy;([^x]*(x+)[^x]*);$2)"));
		p.check();
	}

	@Test
	public void testParentheses() {
		Processor p = new Processor();
		String value = p.process("$(replace;();(\\(\\));$1)");
		assertEquals("()", value);
		p.check();
	}

	@Test
	public void testSimple() {
		Processor p = new Processor();
		p.getMap()
			.put("a", "aaaa");
		assertEquals("aaaa", p.process("${a}"));
		assertEquals("aaaa", p.process("$<a>"));
		assertEquals("aaaa", p.process("$(a)"));
		assertEquals("aaaa", p.process("$[a]"));

		assertEquals("xaaaax", p.process("x${a}x"));
		assertEquals("xaaaaxaaaax", p.process("x${a}x${a}x"));
		p.check();
	}

	@Test
	public void testFilter() {
		Processor p = new Processor();
		p.getMap()
			.put("a", "aaaa");
		assertEquals("aa,cc,ee", p.process("${filter;aa,bb,cc,dd,ee,ff;[ace]+}"));
		assertEquals("aaaa,cc,ee", p.process("${filter;${a},bb,cc,dd,ee,ff;[ace]+}"));
		assertEquals("bb,dd,ff", p.process("${filter;${a},bb,cc,dd,ee,ff;[^ace]+}"));
		p.check();
	}

	@Test
	public void testFilterOut() {
		Processor p = new Processor();
		p.getMap()
			.put("a", "aaaa");
		assertEquals("bb,dd,ff", p.process("${filterout;aa,bb,cc,dd,ee,ff;[ace]+}"));
		assertEquals("bb,dd,ff", p.process("${filterout;${a},bb,cc,dd,ee,ff;[ace]+}"));
		assertEquals("aaaa,cc,ee", p.process("${filterout;${a},bb,cc,dd,ee,ff;[^ace]+}"));
		p.check();
	}

	@Test
	public void testSort() {
		Processor p = new Processor();
		p.getMap()
			.put("a", "aaaa");
		assertEquals("aa,bb,cc,dd,ee,ff", p.process("${sort;aa,bb,cc,dd,ee,ff}"));
		assertEquals("aa,bb,cc,dd,ee,ff", p.process("${sort;ff,ee,cc,bb,dd,aa}"));
		assertEquals("aaaa,bb,cc,dd,ee,ff", p.process("${sort;ff,ee,cc,bb,dd,$<a>}"));
		p.check();
	}

	@Test
	public void testJoin() {
		Processor p = new Processor();
		p.getMap()
			.put("a", "aaaa");
		assertEquals("aa,bb,cc,dd,ee,ff", p.process("${join;aa,bb,cc,dd,ee,ff}"));
		assertEquals("aa,bb,cc,dd,ee,ff", p.process("${join;aa,bb,cc;dd,ee,ff}"));
		assertEquals("aa,bb,cc,dd,ee,ff", p.process("${join;aa;bb;cc;dd;ee,ff}"));
		p.check();
	}

	@Test
	public void testIf() {
		Processor p = new Processor();
		p.getMap()
			.put("a", "aaaa");
		assertEquals("aaaa", p.process("${if;1;$<a>}"));
		assertEquals("", p.process("${if;;$<a>}"));
		assertEquals("yes", p.process("${if;;$<a>;yes}"));
		assertEquals("yes", p.process("${if;false;$<a>;yes}"));
		p.check();
	}

	@Test
	public void testLiteral() {
		Processor p = new Processor();
		p.getMap()
			.put("a", "aaaa");
		assertEquals("${aaaa}", p.process("${literal;$<a>}"));
		p.check();
	}

}
