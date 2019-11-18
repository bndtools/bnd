package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

@SuppressWarnings("resource")
public class MacroTest {

	@Test
	public void testForEmptyMacroKey() throws IOException {
		try (Processor p = new Processor()) {
			p.setProperty("a", "${;foo}");
			assertThat(p.getProperty("a")).isEqualTo("${;foo}");
			assertTrue(p.check("No translation found for macro: ;foo", "Found empty macro key ';foo'"));
		}
	}

	@Test
	public void testWithException() throws IOException {
		try (Processor p = new Processor()) {
			p.getReplacer().inTest = true;
			p.setProperty("a", "${_testdebug;exception;java.util.IOException;foo}");
			assertThat(p.getProperty("a")).isEqualTo("${_testdebug;exception;java.util.IOException;foo}");
			assertTrue(
				p.check("No translation found for macro", "java.util.IOException, for cmd: _testdebug, arguments"));
		}
	}

	@Test
	public void testWildcardWithException() throws IOException {
		try (Processor p = new Processor()) {
			p.getReplacer().inTest = true;
			p.setProperty("a", "${_testdebug;exception}");
			assertThat(p.getProperty("a*")).isEqualTo("${_testdebug;exception}");
			assertTrue(p.check("null, for cmd: _testdebug, arguments;",
				"No translation found for macro: _testdebug;exception"));
		}
	}

	@Test
	public void testSystemProperty() throws IOException {
		System.setProperty("testSystemProperty", "true");
		try (Processor p = new Processor()) {
			assertThat(p.getProperty("testSystemProperty")).isEqualTo("true");
		}
	}

	@Test
	public void testEnvironmentVariable() throws IOException {
		String javaHome = System.getenv("JAVA_HOME");
		if (javaHome != null) {
			try (Processor p = new Processor()) {
				assertThat(p.getReplacer()
					.process("${env;JAVA_HOME}")).isEqualTo(javaHome);
			}

		}
	}

	@Test
	public void testProfile() throws IOException {
		try (Processor p = new Processor()) {
			p.setProperty("[debug]a", "DEBUG");
			p.setProperty("[exec]a", "EXEC");
			p.setProperty("[debug]f", "DEBUG(${1})");
			p.setProperty("[exec]f", "EXEC(${1})");
			p.setProperty("Header", "${a}");
			p.setProperty(Constants.PROFILE, "exec");

			assertEquals("EXEC", p.getProperty("a"));
			assertEquals("EXEC", p.getProperty("Header"));
			assertEquals("EXEC(FOO)", p.getReplacer()
				.process("${f;FOO}"));

			p.setProperty(Constants.PROFILE, "debug");
			assertEquals("DEBUG", p.getProperty("a"));
			assertEquals("DEBUG", p.getProperty("Header"));
			assertEquals("EXEC(FOO)", p.getReplacer()
				.process("${[exec]f;FOO}"));

		}
	}

	@Test
	public void testwildcards() throws IOException {
		try (Processor proc = new Processor()) {
			proc.setProperty("foo", "FOO");
			proc.setProperty("a", "A-${foo}");
			proc.setProperty("a.b", "A.B-${foo}");
			proc.setProperty("a.c", "A.C-${a}");

			String property = proc.getProperty("a*");
			assertThat(Strings.split(property)).contains("A-FOO", "A.B-FOO", "A.C-A-FOO");
		}
	}

	@Test
	public void testCycle() throws IOException {
		try (Processor proc = new Processor()) {
			proc.setProperty("top", "${middle}");
			proc.setProperty("middle", "${bottom}");
			proc.setProperty("bottom", "${top}");

			String property = proc.getProperty("top");
			assertThat(property).isEqualTo("${infinite:[top,bottom,middle,${middle}]}");
			assertTrue(proc.check());
		}
	}

	@Test
	public void testCycleWithFunctions() throws IOException {
		try (Processor proc = new Processor()) {
			proc.setProperty("top", "${middle;A}");
			proc.setProperty("middle", "${bottom;${1}}");
			proc.setProperty("bottom", "${top}");

			String property = proc.getProperty("top");
			assertThat(property).isEqualTo("${infinite:[top,bottom;A,middle;A,${middle;A}]}");
			assertTrue(proc.check());
		}
	}

	@Test
	public void testCommands() throws IOException {
		try (Processor proc = new Processor()) {
			Map<String, String> command = proc.getReplacer()
				.getCommands();
			assertThat(command).isNotNull()
				.containsKeys("template", "decorated", "list", "removeall", "cat");
		}
	}

	@Test
	public void testCommandsWithError() throws IOException {
		try (Processor proc = new Processor()) {
			Map<String, String> command = proc.getReplacer()
				.getCommands();
			assertThat(command).isNotNull()
				.containsKeys("template", "decorated", "list", "removeall", "cat");
		}
	}

	@Test
	public void testTemplates() throws IOException {
		try (Processor proc = new Processor()) {
			proc.setProperty("foo", "a;v=1, b;v=2;x=3");
			String process = proc.getReplacer()
				.process("${template;foo;${@}=${@v}\\;xxx}");
			assertThat(process).isEqualTo("a=1;xxx,b=2;xxx");
		}
	}

	@Test
	public void testTemplatesWithConcat() throws IOException {
		try (Processor proc = new Processor()) {
			proc.setProperty("foo", "a;v=1, b;v=2;x=3");
			String process = proc.getReplacer()
				.process("${template;foo;${@}=${@v};xxx}");
			assertThat(process).isEqualTo("a=1;xxx,b=2;xxx");
		}
	}

	@Test
	public void testTemplatesWithDuplicates() throws IOException {
		try (Processor proc = new Processor()) {
			proc.setProperty("foo", "a;v=1, a;v=2;x=3");
			String process = proc.getReplacer()
				.process("${template;foo;${@}=${@v}\\;xxx}");
			assertThat(process).isEqualTo("a=1;xxx,a=2;xxx");
		}
	}

	@Test
	public void testTemplatesWithMerge() throws IOException {
		try (Processor proc = new Processor()) {
			proc.setProperty("foo", "a;v=1, b;v=2;x=3");
			proc.setProperty("foo.1", "c;v=3");
			String process = proc.getReplacer()
				.process("${template;foo;${@}=${@v}\\;xxx}");
			assertThat(process).isEqualTo("a=1;xxx,b=2;xxx,c=3;xxx");
		}
	}

	@Test
	public void testTemplatesWithDecorate() throws IOException {
		try (Processor proc = new Processor()) {
			proc.setProperty("foo", "a;v=1, b;v=2;x=3");
			proc.setProperty("foo.1", "c;v=3");
			proc.setProperty("foo+", "*;v=0");
			String process = proc.getReplacer()
				.process("${template;foo;${@}=${@v}\\;xxx}");
			assertThat(process).isEqualTo("a=0;xxx,b=0;xxx,c=0;xxx");
		}
	}

	@Test
	public void testDecorated() throws IOException {
		try (Processor proc = new Processor()) {
			proc.setProperty("foo", "a;v=1, b;v=2;x='3,4,5'");
			proc.setProperty("foo.1", "c;v=3");
			proc.setProperty("foo+", "*;v=0");
			String process = proc.getReplacer()
				.process("${decorated;foo}");
			assertThat(process).isEqualTo("a;v=0,b;v=0;x=\"3,4,5\",c;v=0");
		}
	}

	@Test
	public void testDecoratedWithLiterals() throws IOException {
		try (Processor proc = new Processor()) {
			proc.setProperty("foo", "a;v=1, b;v=2;x='3,4,5'");
			proc.setProperty("foo.1", "c;v=3");
			proc.setProperty("foo+", "z;v=1,*;v=0");
			String process = proc.getReplacer()
				.process("${decorated;foo;true}");
			assertThat(process).isEqualTo("a;v=0,b;v=0;x=\"3,4,5\",c;v=0,z;v=1");
		}
	}

	@Test
	public void testRemoveall() throws Exception {
		try (Builder b = new Builder()) {
			Properties p = new Properties();
			p.setProperty("a", "${removeall;A,B,C,D,E,F;B,D,F,G}");
			p.setProperty("empty", "");
			p.setProperty("b", "${removeall;${empty};${empty}}");
			p.setProperty("c", "${removeall;A,B,C,D,E,F;${empty}}");
			p.setProperty("d", "${removeall}");
			p.setProperty("e", "${removeall;A,B,C,D,E,F}");
			p.setProperty("f", "${removeall;not-empty,${empty};not-empty,${empty}}");
			b.setProperties(p);
			assertEquals("A,C,E", b.getProperty("a"));
			assertEquals("", b.getProperty("b"));
			assertEquals("A,B,C,D,E,F", b.getProperty("c"));
			assertEquals("", b.getProperty("d"));
			assertEquals("A,B,C,D,E,F", b.getProperty("e"));
			assertEquals("", b.getProperty("f"));
		}
	}

	@Test
	public void testRetainall() throws Exception {
		try (Builder b = new Builder()) {
			Properties p = new Properties();
			p.setProperty("a", "${retainall;A,B,C,D,E,F;B,D,F,G}");
			p.setProperty("empty", "");
			p.setProperty("b", "${retainall;${empty};${empty}}");
			p.setProperty("c", "${retainall;A,B,C,D,E,F;${empty}}");
			p.setProperty("d", "${retainall}");
			p.setProperty("e", "${retainall;A,B,C,D,E,F}");
			p.setProperty("f", "${retainall;not-empty1,${empty};not-empty2,${empty}}");
			b.setProperties(p);
			assertEquals("B,D,F", b.getProperty("a"));
			assertEquals("", b.getProperty("b"));
			assertEquals("", b.getProperty("c"));
			assertEquals("", b.getProperty("d"));
			assertEquals("", b.getProperty("e"));
			assertEquals("", b.getProperty("f"));
		}
	}

	@Test
	public void testFilterExpression() throws Exception {
		Processor p = new Processor();
		p.setProperty("a", "A");
		p.setProperty("b", "1");

		assertEquals("true", p.getReplacer()
			.process("${if;(a=A)}"));
		assertEquals("true", p.getReplacer()
			.process("${if;(a>=A)}"));
		assertEquals("true", p.getReplacer()
			.process("${if;(a<=A)}"));
		assertEquals("", p.getReplacer()
			.process("${if;(a<A)}"));
		assertEquals("", p.getReplacer()
			.process("${if;(a>A)}"));
		assertEquals("", p.getReplacer()
			.process("${if;(a!=A)}"));

		assertEquals("true", p.getReplacer()
			.process("${if;(a=${a})}"));

		assertEquals("true", p.getReplacer()
			.process("${if;(a>=A)}"));
		assertEquals("true", p.getReplacer()
			.process("${if;(a<=A)}"));
		assertEquals("", p.getReplacer()
			.process("${if;(a<=${b})}"));

	}

	@Test
	public void testFilterSubExpression() throws Exception {
		Processor p = new Processor();
		p.setProperty("a", "A");
		p.setProperty("b", "1");

		assertEquals("true", p.getReplacer()
			.process("${if;(&(a=A)(b=1))}"));
		assertEquals("true", p.getReplacer()
			.process("${if;(&(a=A)(b=1)(|(a!=A)(a=A)))}"));
	}

	@Test
	public void testFilterWithArrays() throws Exception {
		Processor p = new Processor();
		p.setProperty("a", "A,B,C,D");
		p.setProperty("b", "1");

		assertEquals("", p.getReplacer()
			.process("${if;(a=A)}"));
		assertEquals("true", p.getReplacer()
			.process("${if;(a[]=A)}"));
		assertEquals("true", p.getReplacer()
			.process("${if;(a[]=B)}"));
		assertEquals("true", p.getReplacer()
			.process("${if;(a[]=D)}"));
		assertEquals("", p.getReplacer()
			.process("${if;(a[]=E)}"));
		assertEquals("true", p.getReplacer()
			.process("${if;(a[]!=E)}"));
	}

	@Test
	public void testFilterWithInheritance() throws Exception {
		Processor p = new Processor();
		Processor p1 = new Processor(p);
		Processor p2 = new Processor(p1);
		p.setProperty("a", "A,B,C,D");
		p.setProperty("b", "1");

		assertEquals("", p.getReplacer()
			.process("${if;(a=A)}"));
		assertEquals("true", p.getReplacer()
			.process("${if;(a[]=A)}"));
		assertEquals("true", p.getReplacer()
			.process("${if;(a[]=B)}"));
		assertEquals("true", p.getReplacer()
			.process("${if;(a[]=D)}"));
		assertEquals("", p.getReplacer()
			.process("${if;(a[]=E)}"));
		assertEquals("true", p.getReplacer()
			.process("${if;(a[]!=E)}"));
	}

	@Test
	public void testFilterExpressionWithReplacement() throws Exception {
		Processor p = new Processor();
		p.setProperty("a", "A");
		p.setProperty("b", "1");

		assertEquals("YES", p.getReplacer()
			.process("${if;(a=A);YES}"));
		assertEquals("", p.getReplacer()
			.process("${if;(a!=A);YES}"));
		assertEquals("YES", p.getReplacer()
			.process("${if;(a=A);YES;NO}"));
		assertEquals("NO", p.getReplacer()
			.process("${if;(a!=A);YES;NO}"));
	}

	@Test
	public void testUnknownMacroDelimeters() throws IOException {
		Processor p = new Processor();
		assertEquals("${unknown}", p.getReplacer()
			.process("${unknown}"));
		assertEquals("$<unknown>", p.getReplacer()
			.process("$<unknown>"));
		assertEquals("$(unknown)", p.getReplacer()
			.process("$(unknown)"));
		assertEquals("$[unknown]", p.getReplacer()
			.process("$[unknown]"));
		assertEquals("$«unknown»", p.getReplacer()
			.process("$«unknown»"));
		assertEquals("$‹unknown›", p.getReplacer()
			.process("$‹unknown›"));
		assertTrue(p.check("No translation found for macro: unknown"));
	}

	@Test
	public void testVersionMaskWithTarget() throws IOException {
		Processor p = new Processor();
		assertEquals("${version;===;$<@>}", p.getReplacer()
			.process("${version;===;$<@>}"));
		assertTrue(p.check());
	}

	@Test
	public void testVersionMaskWithoutTarget() throws IOException {
		Processor p = new Processor();
		assertEquals("${version;===}", p.getReplacer()
			.process("${version;===}"));
		assertTrue(p.check());
	}

	@Test
	public void testVersionMask() throws IOException {
		Processor p = new Processor();
		assertEquals("1.2.3", p.getReplacer()
			.process("${version;===;1.2.3}"));
		assertTrue(p.check());
	}

	@Test
	public void testVersionMaskNextMajorVersion() throws IOException {
		Processor p = new Processor();
		assertEquals("2.0.0", p.getReplacer()
			.process("${version;+00;1.2.3}"));
		assertTrue(p.check());
	}

	@Test
	public void testVersionMaskWithSetExplicitTarget() throws IOException {
		Processor p = new Processor();
		p.setProperty("@", "1.2.3");
		assertEquals("1.2.3", p.getReplacer()
			.process("${version;===;${@}}"));
		assertTrue(p.check());
	}

	@Test
	public void testVersionMaskWithSetTarget() throws IOException {
		Processor p = new Processor();
		p.setProperty("@", "1.2.3");
		assertEquals("1.2.3", p.getReplacer()
			.process("${version;===}"));
		assertTrue(p.check());
	}

	@Test
	public void testRangeWithSetTarget() throws IOException {
		Processor p = new Processor();
		p.setProperty("@", "1.2.3");
		assertEquals("[1.2.3,2.2.3)", p.getReplacer()
			.process("${range;[===,+===)}"));
		assertTrue(p.check());
	}

	@Test
	public void testRangeWithSetExplicitTarget() throws IOException {
		Processor p = new Processor();
		p.setProperty("@", "1.2.3");
		assertEquals("[1.2.3,2.2.3)", p.getReplacer()
			.process("${range;[===,+===);${@}}"));
		assertTrue(p.check());
	}

	@Test
	public void testRangeWithTarget() throws IOException {
		Processor p = new Processor();
		assertEquals("${range;[===,+===)}", p.getReplacer()
			.process("${range;[===,+===)}"));
		assertTrue(p.check());
	}

	@Test
	public void testRangeWithExplicitTarget() throws IOException {
		Processor p = new Processor();
		assertEquals("${range;[===,+===);${@}}", p.getReplacer()
			.process("${range;[===,+===);${@}}"));
		assertTrue(p.check());
	}

	@Test
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

	@Test
	public void testPackageAttribute() throws Exception {
		Builder builder = new Builder();
		builder.addClasspath(IO.getFile("jar/osgi.jar"));
		builder.setExportPackage("org.osgi.service.event;foo=3");
		builder.setProperty("Header-Version", "${packageattribute;org.osgi.service.event}");
		builder.setProperty("Header-Foo", "${packageattribute;org.osgi.service.event;from:}");
		builder.build();
		assertTrue(builder.check());

		Manifest m = builder.getJar()
			.getManifest();
		String value = m.getMainAttributes()
			.getValue("Header-Version");
		assertEquals("1.0.1", value);
		value = m.getMainAttributes()
			.getValue("Header-Foo");
		assertNotNull(value);
	}
	/*
	 * #722 ${cat;<file>} removes \ before a $
	 */

	@Test
	public void testCat() {
		Processor b = new Processor();
		b.setProperty("tst", "${cat;testresources/macro/cat-test.txt}");
		String tst = b.getProperty("tst");
		assertEquals("This is a \\$ backslashed dollar\n", tst);
	}

	/*
	 * #761 Tstamp consistent
	 */

	@Test
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

	@Test
	public void testFuntionMacrosAndReplace() throws Exception {
		Processor processor = new Processor();
		processor.setProperty("libs", "/lib/a.jar, /lib/b.jar");
		processor.setProperty("foo", "--${1}--");
		processor.setProperty("xlibs", "${replace;${libs};/lib/(.*).jar;$0=${foo;$1}}");

		assertEquals("/lib/a.jar=--a--,/lib/b.jar=--b--", processor.getProperty("xlibs"));
	}

	/**
	 * File name tests
	 *
	 * @throws Exception
	 */

	@Test
	public void testFileNameMacros() throws Exception {
		Processor processor = new Processor();
		File a = IO.getFile("testresources/testfilenamemacros.properties");
		processor.setProperties(a);

		File b = IO.getFile(processor._thisfile(new String[0]));
		assertEquals(a, b);

		assertEquals("properties", processor.getReplacer()
			._extension(new String[] {
				"", "test.resources/test.filenamemacros.properties"
			}));

		assertEquals("testfilenamemacros.properties", processor.getReplacer()
			.process("${basename;testfilenamemacros.properties}"));
		assertEquals("testfilenamemacros", processor.getReplacer()
			.process("${stem;testfilenamemacros.properties}"));
	}

	/**
	 * List functions
	 */
	@Test
	public void testMacroLists() throws Exception {
		Processor processor = new Processor();

		assertEquals("true", processor.getReplacer()
			.process("${apply;isnumber;1,2,3,4}"));
		assertEquals("10", processor.getReplacer()
			.process("${apply;sum;1,2,3,4}"));
		assertEquals("false", processor.getReplacer()
			.process("${apply;isnumber;1,2,3,a,4}"));

		processor.setProperty("double", "${1}${1}");
		processor.setProperty("mulbyindex", "${js;${1}*${2}}");
		assertEquals("A,B,C,D,E,F", processor.getReplacer()
			.process("${map;toupper;a, b, c, d, e, f}"));
		assertEquals("aa,bb,cc,dd,ee,ff", processor.getReplacer()
			.process("${map;double;a, b, c, d, e, f}"));
		assertEquals("0,2,6,12,20,30,42,56,72,90", processor.getReplacer()
			.process("${foreach;mulbyindex;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));

		assertEquals("6", processor.getReplacer()
			.process("${size;a, b, c, d, e, f}"));
		assertEquals("0", processor.getReplacer()
			.process("${size;}"));

		assertEquals("d", processor.getReplacer()
			.process("${get;3;a, b, c, d, e, f}"));
		assertEquals("d", processor.getReplacer()
			.process("${get;-3;a, b, c, d, e, f}"));
		assertEquals("f", processor.getReplacer()
			.process("${get;-1;a, b, c, d, e, f}"));

		assertEquals("b,c", processor.getReplacer()
			.process("${sublist;1;3;a, b, c, d, e, f}"));
		assertEquals("e,f", processor.getReplacer()
			.process("${sublist;-1;-3;a, b, c, d, e, f}"));

		assertEquals("a", processor.getReplacer()
			.process("${first;a, b, c, d, e, f}"));
		assertEquals("", processor.getReplacer()
			.process("${first;}"));
		assertEquals("f", processor.getReplacer()
			.process("${last;a, b, c, d, e, f}"));
		assertEquals("", processor.getReplacer()
			.process("${last;}"));

		assertEquals("5", processor.getReplacer()
			.process("${indexof;6;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));
		assertEquals("-1", processor.getReplacer()
			.process("${indexof;60;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));

		assertEquals("8", processor.getReplacer()
			.process("${lastindexof;7;1, 2, 3, 4, 5, 6, 7, 7, 7, 10}"));

		assertEquals("10,9,8,7,6,5,4,3,2,1", processor.getReplacer()
			.process("${reverse;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));

		assertEquals("55", processor.getReplacer()
			.process("${sum;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));
		assertEquals("55", processor.getReplacer()
			.process("${sum;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));

		assertEquals("5.5", processor.getReplacer()
			.process("${average;1, 2, 3, 4, 5, 6, 7, 8, 9, 10}"));

		assertEquals("-16", processor.getReplacer()
			.process("${nmin;2, 0, -13, 40, 55, -16, 700, -8, 9, 10}"));
		assertEquals("-16", processor.getReplacer()
			.process("${nmin;2; 0; -13; 40 ; 55 ; -16; 700; -8; 9; 10}"));

		assertEquals("700", processor.getReplacer()
			.process("${nmax;2; 0, -13, 40, 55, -16, 700, -8, 9, 10}"));
		assertEquals("700", processor.getReplacer()
			.process("${nmax;2; 0; -13; 40; 55; -16; 700, -8, 9, 10}"));

		assertEquals("-13", processor.getReplacer()
			.process("${min;2; 0; -13; 40; 55; -16; 700, -8, 9, 10}"));
		assertEquals("9", processor.getReplacer()
			.process("${max;2; 0, -13, 40, 55, -16, 700, -8, 9, 10}"));
	}

	/**
	 * String functions
	 */

	@Test
	public void testMacroStrings() throws Exception {
		Processor processor = new Processor();
		processor.setProperty("empty", "");

		assertEquals("6", processor.getReplacer()
			.process("${length;abcdef}"));

		assertEquals("true", processor.getReplacer()
			.process("${is;1.3;1.3;1.3}"));
		assertEquals("false", processor.getReplacer()
			.process("${is;abc;1.3}"));

		assertEquals("true", processor.getReplacer()
			.process("${isnumber;1.3}"));
		assertEquals("false", processor.getReplacer()
			.process("${isnumber;abc}"));

		assertEquals("true", processor.getReplacer()
			.process("${isempty;${empty}}"));
		assertEquals("true", processor.getReplacer()
			.process("${isempty;${empty};${empty};${empty};${empty};}"));
		assertEquals("false", processor.getReplacer()
			.process("${isempty;abc}"));
		assertEquals("false", processor.getReplacer()
			.process("${isempty;${empty};abc}"));

		assertEquals("xyz", processor.getReplacer()
			.process("${trim; \txyz\t  }"));

		assertEquals("bcdef", processor.getReplacer()
			.process("${subst;abacdaef;a}"));
		assertEquals("DEFbDEFcdDEFef", processor.getReplacer()
			.process("${subst;abacdaef;a;DEF}"));
		assertEquals("DEFbacdaef", processor.getReplacer()
			.process("${subst;abacdaef;a;DEF;1}"));
		assertEquals("DEFbDEFcdaef", processor.getReplacer()
			.process("${subst;abacdaef;a;DEF;2}"));
		assertEquals("DEFbDEFcdDEFef", processor.getReplacer()
			.process("${subst;abacdaef;a;DEF;3}"));
		assertEquals("DEFbDEFcdDEFef", processor.getReplacer()
			.process("${subst;abacdaef;a;DEF;300}"));

		assertEquals("true", processor.getReplacer()
			.process("${matches;aaaabcdef;[a]+bcdef}"));
		assertEquals("false", processor.getReplacer()
			.process("${matches;bcdef;[a]+bcdef}"));

		assertEquals("-1", processor.getReplacer()
			.process("${ncompare;2;200}"));
		assertEquals("1", processor.getReplacer()
			.process("${ncompare;200;1}"));
		assertEquals("0", processor.getReplacer()
			.process("${ncompare;200;200}"));

		assertEquals("-1", processor.getReplacer()
			.process("${compare;abc;def}"));
		assertEquals("1", processor.getReplacer()
			.process("${compare;def;abc}"));
		assertEquals("0", processor.getReplacer()
			.process("${compare;abc;abc}"));

		assertEquals("ABCDEF", processor.getReplacer()
			.process("${toupper;abcdef}"));
		assertEquals("abcdef", processor.getReplacer()
			.process("${tolower;ABCDEF}"));

		assertEquals("ab,efab,ef", processor.getReplacer()
			.process("${split;cd;abcdefabcdef}"));
		assertEquals("ab,d,fab,d,f", processor.getReplacer()
			.process("${split;[ce];abcdefabcdef}"));

		assertEquals("3", processor.getReplacer()
			.process("${find;abcdef;def}"));
		assertEquals("-1", processor.getReplacer()
			.process("${find;abc;defxyz}"));
		assertEquals("9", processor.getReplacer()
			.process("${findlast;def;abcdefabcdef}"));

		assertEquals("abcdef", processor.getReplacer()
			.process("${startswith;abcdef;abc}"));
		assertEquals("", processor.getReplacer()
			.process("${startswith;abcdef;xyz}"));

		assertEquals("abcdef", processor.getReplacer()
			.process("${endswith;abcdef;def}"));
		assertEquals("", processor.getReplacer()
			.process("${endswith;abcdef;xyz}"));

		assertEquals("abcdef", processor.getReplacer()
			.process("${endswith;abcdef;def}"));
		assertEquals("", processor.getReplacer()
			.process("${endswith;abcdef;xyz}"));

		assertEquals("def", processor.getReplacer()
			.process("${extension;abcdef.def}"));
		assertEquals("", processor.getReplacer()
			.process("${extension;abcdefxyz}"));
		assertEquals("def", processor.getReplacer()
			.process("${extension;/foo.bar/abcdefxyz.def}"));
		assertEquals("", processor.getReplacer()
			.process("${extension;/foo.bar/abcdefxyz}"));

		assertEquals(".abcdef", processor.getReplacer()
			.process("${basenameext;.abcdef}"));
		assertEquals(".abcdef", processor.getReplacer()
			.process("${basenameext;.abcdef;}"));
		assertEquals(".abcdef", processor.getReplacer()
			.process("${basenameext;.abcdef;bar}"));
		assertEquals("", processor.getReplacer()
			.process("${basenameext;.abcdef;abcdef}"));
		assertEquals("", processor.getReplacer()
			.process("${basenameext;.abcdef;.abcdef}"));

		assertEquals("abcdef.", processor.getReplacer()
			.process("${basenameext;abcdef.}"));
		assertEquals("abcdef.", processor.getReplacer()
			.process("${basenameext;abcdef.;}"));
		assertEquals("abcdef.", processor.getReplacer()
			.process("${basenameext;abcdef.;bar}"));

		assertEquals("abcdef.def", processor.getReplacer()
			.process("${basenameext;abcdef.def}"));
		assertEquals("abcdef.def", processor.getReplacer()
			.process("${basenameext;abcdef.def;bar}"));
		assertEquals("abcdef", processor.getReplacer()
			.process("${basenameext;abcdef.def;def}"));
		assertEquals("abcdef", processor.getReplacer()
			.process("${basenameext;abcdef.def;.def}"));

		assertEquals("abcdefxyz", processor.getReplacer()
			.process("${basenameext;abcdefxyz}"));
		assertEquals("abcdefxyz", processor.getReplacer()
			.process("${basenameext;abcdefxyz;xyz}"));

		assertEquals("abcdefxyz.def", processor.getReplacer()
			.process("${basenameext;/foo.bar/abcdefxyz.def}"));
		assertEquals("abcdefxyz.def", processor.getReplacer()
			.process("${basenameext;/foo.bar/abcdefxyz.def;bar}"));
		assertEquals("abcdefxyz", processor.getReplacer()
			.process("${basenameext;/foo.bar/abcdefxyz.def;def}"));
		assertEquals("abcdefxyz", processor.getReplacer()
			.process("${basenameext;/foo.bar/abcdefxyz.def;.def}"));

		assertEquals("abcdefxyz", processor.getReplacer()
			.process("${basenameext;/foo.bar/abcdefxyz}"));
		assertEquals("abcdefxyz", processor.getReplacer()
			.process("${basenameext;/foo.bar/abcdefxyz;bar}"));
		assertEquals("abcdefxyz", processor.getReplacer()
			.process("${basenameext;/foo.bar/abcdefxyz;xyz}"));

		assertEquals(".abcdefxyz", processor.getReplacer()
			.process("${basenameext;/foo.bar/.abcdefxyz}"));
		assertEquals(".abcdefxyz", processor.getReplacer()
			.process("${basenameext;/foo.bar/.abcdefxyz;bar}"));
		assertEquals("", processor.getReplacer()
			.process("${basenameext;/foo.bar/.abcdefxyz;abcdefxyz}"));
		assertEquals("", processor.getReplacer()
			.process("${basenameext;/foo.bar/.abcdefxyz;.abcdefxyz}"));

		assertEquals("abc", processor.getReplacer()
			.process("${substring;abcdef;0;3}"));
		assertEquals("abc", processor.getReplacer()
			.process("${substring;abcdef;;3}"));
		assertEquals("def", processor.getReplacer()
			.process("${substring;abcdef;-3}"));
		assertEquals("de", processor.getReplacer()
			.process("${substring;abcdef;-3;-1}"));
		assertEquals("def", processor.getReplacer()
			.process("${substring;abcdef;3}"));

		assertEquals("6", processor.getReplacer()
			.process("${length;abcdef}"));
		assertEquals("0", processor.getReplacer()
			.process("${length;}"));

	}

	@Test
	@EnabledOnOs(WINDOWS)
	public void testMacroStringsWindows() throws Exception {
		Processor processor = new Processor();

		assertEquals("def", processor.getReplacer()
			.process("${extension;c:\\foo.bar\\abcdef.def}"));
		assertEquals("", processor.getReplacer()
			.process("${extension;c:\\foo.bar\\abcdef}"));

		assertEquals("abcdef.def", processor.getReplacer()
			.process("${basenameext;c:\\foo.bar\\abcdef.def}"));
		assertEquals("abcdef.def", processor.getReplacer()
			.process("${basenameext;c:\\foo.bar\\abcdef.def;bar}"));
		assertEquals("abcdef", processor.getReplacer()
			.process("${basenameext;c:\\foo.bar\\abcdef.def;def}"));
		assertEquals("abcdef", processor.getReplacer()
			.process("${basenameext;c:\\foo.bar\\abcdef.def;.def}"));

		assertEquals("abcdef", processor.getReplacer()
			.process("${basenameext;c:\\foo.bar\\abcdef}"));
		assertEquals("abcdef", processor.getReplacer()
			.process("${basenameext;c:\\foo.bar\\abcdef;bar}"));
		assertEquals("abcdef", processor.getReplacer()
			.process("${basenameext;c:\\foo.bar\\abcdef;def}"));
	}

	/**
	 * Test rand
	 */

	@Test
	public void testRan() {
		Processor processor = new Processor();
		for (int i = 0; i < 1000; i++) {
			int value = Integer.parseInt(processor.getReplacer()
				.process("${rand;-10;10}"));
			assertTrue(value >= -10 && value <= 10);
		}
	}

	/**
	 * Test Javascript stuff
	 */

	@Test
	public void testJSSimple() {
		Processor processor = new Processor();
		processor.setProperty("alpha", "25");
		assertEquals("3", processor.getReplacer()
			.process("${js;1+2;}"));
		assertEquals("25", processor.getReplacer()
			.process("${js;domain.get('alpha');}"));
		assertEquals("5", processor.getReplacer()
			.process("${js;domain.get('alpha')/5;}"));

	}

	/**
	 * Check if we can initialize
	 */
	@Test
	public void testJSINit() {
		Processor processor = new Processor();
		processor.setProperty("javascript", "function top() { return 13; }");
		assertEquals("16", processor.getReplacer()
			.process("${js;1+2+top()}"));
	}

	/**
	 * See if the initcode is concatenated correctly
	 */
	@Test
	public void testJSINit2() {
		Processor processor = new Processor();
		processor.setProperty("javascript", "function top() { return 1; }");
		processor.setProperty("javascript.1", "function top() { return 2; }");
		processor.setProperty("javascript.2", "function top() { return 3; }");
		assertEquals("3", processor.getReplacer()
			.process("${js;top()}"));
	}

	/**
	 * Test control characters
	 */
	@Test
	public void testControlCharacters() throws Exception {
		Processor p = new Processor();
		p.setProperty("a", "a, b, c");
		String s = p.getReplacer()
			.process("${unescape;${replace;${a};(.+);$0;\\n}}\n");
		assertEquals("a\nb\nc\n", s);
	}

	/**
	 * Test the custom macros
	 *
	 * @throws IOException
	 */

	@Test
	public void testCustomMacros() throws IOException {
		Processor x = new Processor();
		x.setProperty("foo", "Hello ${1}");
		assertEquals("Hello Peter", x.getReplacer()
			.process("${foo;Peter}"));
	}

	@Test
	public void testCustomMacrosExtensive() throws IOException {
		assertTemplate("this is 1 abc, and this is def", "this is 1 ${1}, and this is ${2}", "abc;def");
		assertTemplate("abc,def", "${#}", "abc;def");
		assertTemplate("osgi.ee;filter:='(&(osgi.ee=JavaSE)(version=1.6))'",
			"osgi.ee;filter:='(&(osgi.ee=JavaSE)(version=1.${1}))'", "6");
	}

	void assertTemplate(String result, String template, String params) throws IOException {
		Processor top = new Processor();
		top.setProperty("template", template);
		top.setProperty("macro", "${template;" + params + "}");
		String expanded = top.getProperty("macro");
		assertThat(top.check()).isTrue();
		assertEquals(result, expanded);
	}

	/**
	 * Test replacement of ./ with cwd
	 */

	@Test
	public void testCurrentWorkingDirectory() {
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

		String cwd = IO.absolutePath(top.getBase()) + "/";

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

	@Test
	public void testifDir() {
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

	@Test
	public void testWildcardKeys() {
		Processor top = new Processor();
		top.setProperty("a.3", "a.3");
		top.setProperty("a.1", "a.1");
		top.setProperty("a.2", "a.2");
		top.setProperty("a.4", "a.4");
		top.setProperty("aa", "${a.*}");
		assertEquals("a.1,a.2,a.3,a.4", top.getProperty("a.*"));
		assertEquals("a.1,a.2,a.3,a.4", top.getProperty("aa"));

	}

	@Test
	public void testEnv() {
		Processor proc = new Processor();
		String s = proc.getReplacer()
			.process("${env;PATH}");
		assertNotNull(s);
		assertTrue(s.length() > 0);
		String s2 = proc.getReplacer()
			.process("${env.PATH}");
		assertNotNull(s2);
		assertTrue(s2.length() > 0);
		assertEquals(s, s2);
	}

	@Test
	public void testEnvAlt() {
		Processor proc = new Processor();
		String s = proc.getReplacer()
			.process("${env;FOOBAR;hello}");
		assertEquals("hello", s);
	}

	/**
	 * Test the random macro
	 */
	@Test
	public void testRandom() {
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

	@Test
	public void testSuper() {
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

	@Test
	public void testNesting2() {
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

	@Test
	@DisabledOnOs(WINDOWS)
	public void testSystem() throws Exception {
		Processor p = new Processor();
		Macro macro = new Macro(p);
		assertEquals("Hello World", macro.process("${system;echo Hello World}"));
		assertTrue(macro.process("${system;wc;Hello World}")
			.matches("\\s*[0-9]+\\s+[0-9]+\\s+[0-9]+\\s*"));
	}

	@Test
	public void testSystemFail() throws Exception {
		Processor p = new Processor();
		Macro macro = new Macro(p);
		String cmd = "${system;mostidioticcommandthatwillsurelyfail}";
		assertTrue(macro.process(cmd)
			.startsWith("${system;"));
	}

	/**
	 * Verify system-allow-fail command
	 */

	@Test
	public void testSystemAllowFail() throws Exception {
		Processor p = new Processor();
		Macro macro = new Macro(p);
		assertEquals("", macro.process("${system-allow-fail;mostidioticcommandthatwillsurelyfail}"));
	}

	/**
	 * Check that variables override macros.
	 */
	@Test
	public void testPriority() {
		Processor p = new Processor();
		p.setProperty("now", "not set");
		Macro macro = new Macro(p);
		assertEquals("not set", macro.process("${now}"));

	}

	@Test
	public void testNames() {
		Processor p = new Processor();
		p.setProperty("a", "a");
		p.setProperty("aa", "aa");
		Macro macro = new Macro(p);

		assertEquals("aa", macro.process("${${a}${a}}"));
	}

	@Test
	public void testVersion() throws Exception {
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

		assertEquals(0, proc.getErrors()
			.size());
		assertEquals(0, proc.getWarnings()
			.size());

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

	@Test
	public void testRange() throws Exception {
		Processor proc = new Processor();
		Macro macro = new Macro(proc);
		assertEquals("[1.0,1.0]", macro.process("${range;[==,==];1.0.0}"));
		assertEquals("[1.0.0,1.0.1]", macro.process("${range;[===,==+];1.0.0}"));
		assertEquals("[0.1.0,0.1.2)", macro.process("${range;[=+0,=++);0.0.1}"));
		assertEquals("[0.0.9,0.1.2)", macro.process("${range;[==9,=++);0.0.1}"));

		assertEquals(0, proc.getErrors()
			.size());
		assertEquals(0, proc.getWarnings()
			.size());

		proc.setProperty("@", "1.2.3");
		assertEquals("[1.0.0,2)", macro.process("${range;[=00,+)}"));

		proc.clear();
		macro.process("${range;=+0,=++;0.0.1}");
		assertEquals(1, proc.getErrors()
			.size());
		assertEquals(1, proc.getWarnings()
			.size());

		proc.clear();
		macro.process("${range;[+,=)}");
		assertEquals(1, proc.getErrors()
			.size());
		assertEquals(1, proc.getWarnings()
			.size());
	}

	/**
	 * Test the lsa/lsr macros
	 */
	@Test
	public void testLs() throws IOException {
		String cwdPrefix = IO.absolutePath(new File("")) + "/";
		Predicate<String> absolute = s -> s.startsWith(cwdPrefix);
		try (Processor p = new Processor()) {
			Macro macro = p.getReplacer();
			List<String> a = Strings.split(macro.process("${lsr;test/test;*.java}"));
			assertThat(a).contains("MacroTest.java", "ManifestTest.java")
				.doesNotContain("bnd.info", "com.acme")
				.noneMatch(absolute);

			List<String> b = Strings.split(macro.process("${lsr;test/test}"));
			assertThat(b).contains("MacroTest.java", "ManifestTest.java", "bnd.info", "com.acme")
				.noneMatch(absolute);

			List<String> c = Strings.split(macro.process("${lsa;test/test;*.java}"));
			assertThat(c)
				.contains(IO.absolutePath(new File("test/test/MacroTest.java")),
					IO.absolutePath(new File("test/test/ManifestTest.java")))
				.doesNotContain(IO.absolutePath(new File("test/test/bnd.info")),
					IO.absolutePath(new File("test/test/com.acme")))
				.allMatch(absolute);

			List<String> d = Strings.split(macro.process("${lsa;test/test}"));
			assertThat(d)
				.contains(IO.absolutePath(new File("test/test/MacroTest.java")),
					IO.absolutePath(new File("test/test/ManifestTest.java")),
					IO.absolutePath(new File("test/test/bnd.info")), IO.absolutePath(new File("test/test/com.acme")))
				.allMatch(absolute);
		}
	}

	/**
	 * Check the uniq command
	 */

	@Test
	public void testUniq() {
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

	@Test
	public void testEscapedArgs() {
		Builder builder = new Builder();
		Properties p = new Properties();
		p.setProperty("x", "${replace;1,2,3;.+;$0\\;version=1}");
		builder.setProperties(p);
		assertEquals("1;version=1,2;version=1,3;version=1", builder.getProperty("x"));

	}

	@Test
	public void testListMacro() throws Exception {
		try (Builder builder = new Builder()) {
			Properties p = new Properties();
			p.setProperty("l1", "1;version=\"[1.1,2)\",,2;version=1.2");
			p.setProperty("l2", "3;version=1.3,");
			p.setProperty("x", "${replacelist;${list;l1;l2};$;\\;maven-scope=provided}");
			builder.setProperties(p);
			assertEquals(
				"1;version=\"[1.1,2)\";maven-scope=provided,2;version=1.2;maven-scope=provided,3;version=1.3;maven-scope=provided",
				builder.getProperty("x"));
		}
	}

	/**
	 * Check if variables that contain variables, ad nauseum, really wrk
	 */
	@Test
	public void testNested() {
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

	@Test
	public void testLoop() {
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

	@Test
	public void testTstamp() {
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

	@Test
	public void testIsfile() {
		Processor p = new Processor();
		Macro m = new Macro(p);
		assertEquals("true", m.process("${isfile;.project}"));
		assertEquals("false", m.process("${isfile;thisfiledoesnotexist}"));
	}

	@Test
	public void testParentFile() {
		Processor p = new Processor();
		Macro m = new Macro(p);
		assertTrue(m.process("${dir;.project}")
			.endsWith("biz.aQute.bndlib.tests"));
	}

	@Test
	public void testBasename() {
		Processor p = new Processor();
		Macro m = new Macro(p);
		assertEquals("biz.aQute.bndlib.tests", m.process("${basename;${dir;.project}}"));
	}

	@Test
	public void testMavenVersionMacro() throws Exception {
		Builder builder = new Builder();
		Properties p = new Properties();
		p.setProperty("Export-Package", "org.objectweb.*;version=1.5-SNAPSHOT");
		builder.setProperties(p);
		builder.setClasspath(new File[] {
			IO.getFile("jar/asm.jar")
		});
		Jar jar = builder.build();
		Manifest manifest = jar.getManifest();
		String export = manifest.getMainAttributes()
			.getValue("Export-Package");
		assertNotNull(export);
		assertTrue(export.contains("1.5.0.SNAPSHOT"), "Test snapshot version");
	}

	/**
	 * Check if we can check for the defintion of a variable
	 */

	@Test
	public void testDef() {
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
	@Test
	public void testReplace() {
		Processor p = new Processor();
		p.setProperty("specs", "a0,b0, c0,    d0");
		Macro m = new Macro(p);
		assertEquals("xa0y,xb0y,xc0y,xd0y", m.process("${replace;${specs};([^\\s]+);x$1y}"));
		assertEquals("a,b,c,d", m.process("${replace;${specs};0}"));
	}

	@Test
	public void testReplaceString() {
		Processor p = new Processor();
		p.setProperty("json", "[{\"key1\": \"value\"}, {\"key2\": \"value\"}, {\"key\": \"value\"}]");
		Macro m = new Macro(p);
		assertEquals("[{\"name1key\": \"value\"}, {\"name2key\": \"value\"}, {\"key\": \"value\"}]",
			m.process("${replacestring;${json};key(\\d);name$1key}"));
	}

	@Test
	public void testToClassName() {
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

	@Test
	public void testFindPath() throws IOException {
		try (Analyzer analyzer = new Analyzer()) {
			analyzer.setJar(IO.getFile("jar/asm.jar"));
			Macro m = new Macro(analyzer);

			assertTrue(m.process("${findname;(.*)\\.class;$1.xyz}")
				.contains("FieldVisitor.xyz,"));
			assertTrue(m.process("${findname;(.*)\\.class;$1.xyz}")
				.contains("MethodVisitor.xyz,"));
			assertTrue(m.process("${findpath;(.*)\\.class}")
				.contains("org/objectweb/asm/AnnotationVisitor.class,"));
			assertTrue(m.process("${findpath;(.*)\\.class}")
				.contains("org/objectweb/asm/ByteVector.class, org/objectweb/asm/ClassAdapter.class,"));
			assertEquals("META-INF/MANIFEST.MF", m.process("${findpath;META-INF/MANIFEST.MF}"));
			assertEquals("Label.class", m.process("${findname;Label\\..*}"));
			assertEquals("Adapter, Visitor, Writer", m.process("${findname;Method(.*)\\.class;$1}"));
		}
	}

	@Test
	public void testWarning() {
		Processor p = new Processor();
		p.setProperty("three", "333");
		p.setProperty("empty", "");
		p.setProperty("real", "true");
		Macro m = new Macro(p);

		m.process("    ${warning;xw;1;2;3 ${three}}");
		m.process("    ${error;xe;1;2;3 ${three}}");
		m.process("    ${if;1;$<a>}");

		assertTrue(p.getWarnings()
			.get(0)
			.endsWith("xw"), "xw");
		assertTrue(p.getWarnings()
			.get(1)
			.endsWith("1"), "1");
		assertTrue(p.getWarnings()
			.get(2)
			.endsWith("2"), "2");
		assertTrue(p.getWarnings()
			.get(3)
			.endsWith("3 333"), "3 333");

		assertTrue(p.getErrors()
			.get(0)
			.endsWith("xe"), "xw");
		assertTrue(p.getErrors()
			.get(1)
			.endsWith("1"), "1");
		assertTrue(p.getErrors()
			.get(2)
			.endsWith("2"), "2");
		assertTrue(p.getErrors()
			.get(3)
			.endsWith("3 333"), "3 333");
	}

	@Test
	public void testNestedReplace() {
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

	@Test
	public void testParentheses() {
		Processor p = new Processor();
		Macro m = new Macro(p);
		String value = m.process("$(replace;();(\\(\\));$1)");
		assertEquals("()", value);
	}

	@Test
	public void testSimple() {
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

	@Test
	public void testFilter() {
		Processor p = new Processor();
		p.setProperty("a", "aaaa");
		Macro m = new Macro(p);
		assertEquals("aa,cc,ee", m.process("${filter;aa,bb,cc,dd,ee,ff;[ace]+}"));
		assertEquals("aaaa,cc,ee", m.process("${filter;${a},bb,cc,dd,ee,ff;[ace]+}"));
		assertEquals("bb,dd,ff", m.process("${filter;${a},bb,cc,dd,ee,ff;[^ace]+}"));
	}

	@Test
	public void testFilterOut() {
		Processor p = new Processor();
		p.setProperty("a", "aaaa");
		Macro m = new Macro(p);
		assertEquals("bb,dd,ff", m.process("${filterout;aa,bb,cc,dd,ee,ff;[ace]+}"));
		assertEquals("bb,dd,ff", m.process("${filterout;${a},bb,cc,dd,ee,ff;[ace]+}"));
		assertEquals("aaaa,cc,ee", m.process("${filterout;${a},bb,cc,dd,ee,ff;[^ace]+}"));
	}

	@Test
	public void testSort() {
		Processor p = new Processor();
		p.setProperty("a", "aaaa");
		Macro m = new Macro(p);
		assertEquals("aa,bb,cc,dd,ee,ff", m.process("${sort;aa,bb,cc,dd,ee,ff}"));
		assertEquals("aa,bb,cc,dd,ee,ff", m.process("${sort;ff,ee,cc,bb,dd,aa}"));
		assertEquals("aaaa,bb,cc,dd,ee,ff", m.process("${sort;ff,ee,cc,bb,dd,$<a>}"));
	}

	@Test
	public void testNSort() {
		Processor p = new Processor();
		p.setProperty("a", "02");
		Macro m = new Macro(p);
		assertEquals("1,02,10", m.process("${nsort;$<a>,1,10}"));
	}

	@Test
	public void testJoin() {
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

	@Test
	public void testIf() {
		Processor p = new Processor();
		p.setProperty("a", "aaaa");
		Macro m = new Macro(p);
		assertEquals("aaaa", m.process("${if;1;$<a>}"));
		assertEquals("", m.process("${if;;$<a>}"));
		assertEquals("yes", m.process("${if;;$<a>;yes}"));
		assertEquals("yes", m.process("${if;false;$<a>;yes}"));
	}

	@Test
	public void testLiteral() {
		Processor p = new Processor();
		p.setProperty("a", "aaaa");
		Macro m = new Macro(p);
		assertEquals("${aaaa}", m.process("${literal;$<a>}"));
	}

	@Test
	public void testFilterout() throws Exception {
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

	@Test
	public void testPackagesMacro() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new Jar[] {
			new Jar(IO.getFile("bin_test"))
		});
		b.setProperty("Private-Package",
			"test.packageinfo.annotated,test.packageinfo.notannotated,test.packageinfo.nopackageinfo,test.activator");
		b.setProperty("All-Packages", "${packages}");
		b.setProperty("Annotated", "${packages;annotated;test.packageinfo.annotated.BlahAnnotation}");
		b.setProperty("Named", "${packages;named;*.notannotated}");
		b.setProperty("Negated", "${packages;named;!*.no*}");
		b.setProperty("Versioned", "${packages;versioned}");
		b.build();

		assertEquals(0, b.getErrors()
			.size());

		assertEquals(
			"test.packageinfo.annotated,test.packageinfo.notannotated,test.packageinfo.nopackageinfo,test.activator",
			b.getProperty("All-Packages"));
		assertEquals("test.packageinfo.annotated", b.getProperty("Annotated"));
		assertEquals("test.packageinfo.notannotated", b.getProperty("Named"));
		assertEquals("test.packageinfo.annotated,test.activator", b.getProperty("Negated"));
		assertEquals("test.packageinfo.annotated,test.packageinfo.notannotated", b.getProperty("Versioned"));
	}

	@Test
	public void testBase64() {
		Processor b = new Processor();
		b.setProperty("b64", "${base64;testresources/macro/base64-test.gif}");
		String b64 = "R0lGODlhBwAIAKIAANhCT91bZuN3gOeIkOiQl+ygp////////yH5BAEAAAcALAAAAAAHAAgAAAMXCLoqFUWoYogpKlgS8u4AZWGAAw0MkwAAOw==";
		assertEquals(b64, b.getProperty("b64"));
	}

	@Test
	public void testDigest() {
		Processor b = new Processor();
		b.setProperty("a", "${digest;SHA-256;testresources/macro/digest-test.jar}");
		b.setProperty("b", "${digest;MD5;testresources/macro/digest-test.jar}");

		assertEquals("3B21F1450430C0AFF57E12A338EF6AA1A2E0EE318B8883DD196048450C2FC1FC", b.getProperty("a"));
		assertEquals("F31BAC7F1F70E5D8705B98CC0FBCFF5E", b.getProperty("b"));
	}

	@Test
	public void testProcessNullValue() throws Exception {
		try (Processor b = new Processor()) {
			Macro m = b.getReplacer();
			String tst = m.process(null);
			assertEquals("", tst);
			assertTrue(b.check());
		}
	}

	@Test
	public void testNonStringValue() throws Exception {
		try (Processor b = new Processor()) {
			b.setPedantic(true);
			// getProperty will return null for non-String value
			b.getProperties()
				.put("tst", new StringBuilder("foo"));
			b.getProperties()
				.put("num", 2);
			String tst = b.getProperty("tst");
			assertNull(tst);
			String num = b.getProperty("num");
			assertNull(num);
			assertTrue(b.check("Key 'tst' has a non-String value", "Key 'num' has a non-String value"));
		}
	}

	@Test
	public void testNonStringFlattenedValue() throws Exception {
		try (Processor b = new Processor()) {
			b.setPedantic(true);
			// getProperty will return null for non-String value
			b.getProperties()
				.put("tst", new StringBuilder("foo"));
			b.getProperties()
				.put("num", 2);
			Properties f = b.getFlattenedProperties();
			String tst = f.getProperty("tst");
			assertNull(tst);
			String num = f.getProperty("num");
			assertNull(num);
			assertTrue(b.check("Key 'tst' has a non-String value", "Key 'num' has a non-String value"));
		}
	}

	@Test
	public void testDateFormat() throws IOException {
		try (Processor processor = new Processor()) {
			// keep time constant in build
			processor.setProperty(Constants.TSTAMP, "0");

			assertThat(processor.getReplacer()
				.process("${format;%tY-%<tm-%<td %<tH:%<tM:%<tS %<tZ;0}")).isEqualTo("1970-01-01 00:00:00 UTC");
			assertThat(processor.getReplacer()
				.process("${format;%tY-%<tm-%<td %<tH:%<tM:%<tS %<tZ;${now}}")).isEqualTo("1970-01-01 00:00:00 UTC");
			assertThat(processor.getReplacer()
				.process("${format;%tY-%<tm-%<td %<tH:%<tM:%<tS %<tZ;${tstamp}}")).isEqualTo("1970-01-01 00:00:00 UTC");

			// check indexed forward
			assertEquals("1970/01 Z", processor.getReplacer()
				.process("${format;%2$tY/%2$tm %2$tZ;X;1970-01-01T00:00:00Z}"));

			// check indexed backward
			assertEquals("1970/01 Z", processor.getReplacer()
				.process("${format;%2$tY/%2$tm %2$tZ;X;1970-01-01T00:00:00Z;Y}"));

			assertEquals("1970/01 Z", processor.getReplacer()
				.process("${format;%tY/%<tm %<tZ;1970-01-01T00:00:00Z}"));

			assertEquals("1970/01 Z", processor.getReplacer()
				.process("${format;%tY/%1$tm %1$tZ;1970-01-01T00:00:00Z}"));

			assertEquals("1970/01 Z", processor.getReplacer()
				.process("${format;%2$tY/%2$tm %2$tZ;X;1970-01-01T00:00:00Z}"));

			assertEquals("1970/01 00 +08:00", processor.getReplacer()
				.process("${format;%2$tY/%2$tm %<tH %2$tZ;X;1970-01-01T00:00:00+08:00}"));

			assertEquals("UTC", processor.getReplacer()
				.process("${format;%TZ;20190704}"));

			assertEquals("201907", processor.getReplacer()
				.process("${format;%TY%<tm;20190704}"));

			assertEquals("2019", processor.getReplacer()
				.process("${format;%tY;1562252413579}"));

			assertEquals("201907", processor.getReplacer()
				.process("${format;%tY%Tm;20190704;20190704}"));

			assertEquals("07", processor.getReplacer()
				.process("${format;%tm;201907040000}"));

			assertEquals("04", processor.getReplacer()
				.process("${format;%td;20190704000000}"));
		}
	}

	@Test
	public void testIndexedFormat() throws IOException {
		try (Processor processor = new Processor()) {
			assertEquals("3", processor.getReplacer()
				.process("${format;%3$s;1;2;3}"));

			assertEquals("1 2 1", processor.getReplacer()
				.process("${format;%s %s %1$s;1;2;3}"));

			assertEquals("1 2 2", processor.getReplacer()
				.process("${format;%s %s %<s;1;2;3}"));

		}
	}

	@Test
	public void testFormat() throws IOException {
		try (Processor processor = new Processor()) {

			assertEquals("ff", processor.getReplacer()
				.process("${format;%x;255}"));

			assertEquals("\\u00FF", processor.getReplacer()
				.process("${format;\\u%04X;255}"));


			assertEquals("foo", processor.getReplacer()
				.process("${format;%s;foo}"));

			assertEquals("18cc6", processor.getReplacer()
				.process("${format;%h;foo}"));

			assertEquals("18CC6", processor.getReplacer()
				.process("${format;%H;foo}"));

			assertEquals("false", processor.getReplacer()
				.process("${format;%b;(foo=bar)}"));

			assertEquals("10.30", processor.getReplacer()
				.process("${format;%.2f;10.3}"));

			assertEquals("µ", processor.getReplacer()
				.process("${format;%c;" + (int) 'µ' + "}"));

			assertEquals("µ", processor.getReplacer()
				.process("${format;%c;µ}"));

			assertEquals("\n000010", processor.getReplacer()
				.process("${format;\n%06d;10}"));
			assertEquals("000010", processor.getReplacer()
				.process("${format;%1$06d;10}"));
			assertEquals("2e C8 300 620", processor.getReplacer()
				.process("${format;%x %X %d %o;46;200;300;400;500}"));
			assertEquals("+00010", processor.getReplacer()
				.process("${format;%+06d;10}"));
			assertEquals(String.format("%,6d", 100000), processor.getReplacer()
				.process("${format;%,6d;100000}"));
		}
	}
}
