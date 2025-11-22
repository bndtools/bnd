package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;

/**
 * Test cases for macro examples documented in docs/_macros/*.md files. Each
 * test method is annotated with the corresponding markdown file. This ensures
 * that documentation examples are correct and functional. See also
 * /DEV_README.md#macro-documentation-testing
 */
@SuppressWarnings("resource")
public class MacroTestsForDocsExamples {

	// ===== apply.md =====
	@Test
	public void testApply() throws IOException {
		try (Processor p = new Processor()) {
			p.setProperty("args", "com.example.foo, 3.12, HIGHEST");
			String result = p.getReplacer()
				.process("${apply;repo;${args}}");
			assertThat(result).isEqualTo("${repo;com.example.foo;3.12;HIGHEST}");
		}
	}
	// ===== average.md =====
	@Test
	public void testAverage_SimpleList() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${average;1,2,3,4,5}");
			assertThat(result).isEqualTo("3"); // Returns "3" not "3.0" due to toString() stripping .0
		}
	}

	@Test
	public void testAverage_MultipleLists() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${average;10,20,30;40,50}");
			assertThat(result).isEqualTo("30"); // Returns "30" not "30.0"
		}
	}

	@Test
	public void testAverage_DecimalValues() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${average;1.5,2.5,3.5}");
			assertThat(result).isEqualTo("2.5");
		}
	}

	// ===== base64.md =====
	@Test
	public void testBase64_EncodeFile(@InjectTemporaryDirectory File tempDir) throws IOException {
		try (Processor p = new Processor()) {
			File testFile = tempDir.toPath().resolve("test.txt").toFile();
			IO.store("Hello World", testFile);
			p.setBase(tempDir);

			String result = p.getReplacer().process("${base64;test.txt}");
			// "Hello World" in Base64 is "SGVsbG8gV29ybGQ="
			assertThat(result).isEqualTo("SGVsbG8gV29ybGQ=");
		}
	}

	// ===== basedir.md =====
	@Test
	public void testBasedir() throws IOException {
		try (Processor p = new Processor()) {
			File base = new File("/tmp/test");
			p.setBase(base);

			String result = p.getReplacer().process("${basedir}");
			assertThat(result).isEqualTo(IO.normalizePath(base.getAbsolutePath()));
		}
	}

	// ===== bytes.md =====
	@Test
	public void testBytes_Various() throws IOException {
		try (Processor p = new Processor()) {
			// bytes macro formats with units and space
			assertThat(p.getReplacer()
				.process("${bytes;512}")).isEqualTo("512.0 b");
			assertThat(p.getReplacer()
				.process("${bytes;1024}")).isEqualTo("1.0 Kb");
			assertThat(p.getReplacer()
				.process("${bytes;1048576}")).isEqualTo("1.0 Mb");
			assertThat(p.getReplacer()
				.process("${bytes;5242880}")).isEqualTo("5.0 Mb");
			assertThat(p.getReplacer()
				.process("${bytes;1073741824}")).isEqualTo("1.0 Gb");
			assertThat(p.getReplacer()
				.process("${bytes;10737418240}")).isEqualTo("10.0 Gb");
			assertThat(p.getReplacer()
				.process("${bytes;1048576;10000048576}")).isEqualTo("1.0 Mb 9.3 Gb");
		}
	}

	// ===== cat.md =====
	@Test
	public void testCat_ReadFile(@InjectTemporaryDirectory
	File tempDir) throws IOException {
		try (Processor p = new Processor()) {
			File testFile = tempDir.toPath()
				.resolve("config.txt")
				.toFile();
			IO.store("my-value", testFile);
			p.setBase(tempDir);

			String result = p.getReplacer().process("${cat;config.txt}");
			assertThat(result).isEqualTo("my-value");
		}
	}

	// ===== currenttime.md =====
	@Test
	public void testCurrenttime() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${currenttime}");
			// Should be a number representing milliseconds since epoch
			long timestamp = Long.parseLong(result);
			assertThat(timestamp).isGreaterThan(0);
		}
	}

	// ===== def.md =====
	@Test
	public void testDef() throws IOException {
		try (Processor p = new Processor()) {
			p.setProperty("version", "1.0.0");
			// def returns the property value or default, not boolean
			assertThat(p.getReplacer().process("${def;version}")).isEqualTo("1.0.0");
			assertThat(p.getReplacer().process("${def;missing}")).isEmpty();
			assertThat(p.getReplacer().process("${def;missing;default}")).isEqualTo("default");
		}
	}

	// ===== digest.md =====
	@Test
	public void testDigest_MD5(@InjectTemporaryDirectory
	File tempDir) throws IOException {
		try (Processor p = new Processor()) {
			File testFile = tempDir.toPath()
				.resolve("data.txt")
				.toFile();
			IO.store("test data", testFile);
			p.setBase(tempDir);

			String result = p.getReplacer().process("${digest;MD5;data.txt}");
			// MD5 of "test data" - digest returns uppercase
			assertThat(result.toLowerCase()).isEqualTo("eb733a00c0c9d336e65691a37ab54293");
		}
	}

	// ===== dir.md =====
	@Test
	public void testDir(@InjectTemporaryDirectory
	File tempDir) throws IOException {
		try (Processor p = new Processor()) {
			// dir only works with actual files on disk, returns parent directory
			File testFile = tempDir.toPath()
				.resolve("testfile.txt")
				.toFile();
			IO.store("test", testFile);
			String result = p.getReplacer().process("${dir;" + testFile.getAbsolutePath() + "}");
			assertThat(result).isEqualTo(IO.normalizePath(testFile.getParent()));
		}
	}

	// ===== endswith.md =====
	@Test
	public void testEndswith() throws IOException {
		try (Processor p = new Processor()) {
			// endswith returns the string if it matches, empty string if not
			assertThat(p.getReplacer().process("${endswith;hello.txt;.txt}")).isEqualTo("hello.txt");
			assertThat(p.getReplacer().process("${endswith;hello.txt;.md}")).isEmpty();
		}
	}

	// ===== env.md =====
	@Test
	public void testEnv() throws IOException {
		try (Processor p = new Processor()) {
			// Set a test environment variable through system properties
			String javaHome = System.getenv("JAVA_HOME");
			if (javaHome != null) {
				String result = p.getReplacer().process("${env;JAVA_HOME}");
				assertThat(result).isEqualTo(javaHome);
			}
			// Test default value
			assertThat(p.getReplacer().process("${env;NONEXISTENT;default_value}"))
				.isEqualTo("default_value");
		}
	}

	// ===== filter.md =====
	@Test
	public void testFilter() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${filter;a,b,c,d;[ac]}"))
				.isEqualTo("a,c");
			assertThat(p.getReplacer().process("${filter;apple,banana,apricot;^a.*}"))
				.isEqualTo("apple,apricot");
		}
	}

	// ===== filterout.md =====
	@Test
	public void testFilterout() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${filterout;a,b,c,d;[ac]}"))
				.isEqualTo("b,d");
			assertThat(p.getReplacer().process("${filterout;apple,banana,apricot;^a.*}"))
				.isEqualTo("banana");
		}
	}

	// ===== find.md =====
	@Test
	public void testFind() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${find;hello world;world}")).isEqualTo("6");
			assertThat(p.getReplacer().process("${find;hello world;missing}")).isEqualTo("-1");
		}
	}

	// ===== findlast.md =====
	@Test
	public void testFindlast() throws IOException {
		try (Processor p = new Processor()) {
			// findlast syntax is ${findlast;<search>;<string>}
			assertThat(p.getReplacer().process("${findlast;hello;hello hello}")).isEqualTo("6");
			assertThat(p.getReplacer().process("${findlast;missing;test}")).isEqualTo("-1");
		}
	}

	// ===== first.md =====
	@Test
	public void testFirst() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${first;a,b,c}")).isEqualTo("a");
			assertThat(p.getReplacer().process("${first;single}")).isEqualTo("single");
		}
	}

	// ===== foreach.md =====
	@Test
	public void testForeach() throws IOException {
		try (Processor p = new Processor()) {
			// foreach calls a macro with ${macro;value;index}
			// Define a macro that formats items - use ${1} for first arg
			p.setProperty("template", "item-${1}");
			String result = p.getReplacer().process("${foreach;template;a,b,c}");
			assertThat(result).isEqualTo("item-a,item-b,item-c");
		}
	}

	// ===== format.md =====
	@Test
	public void testFormat() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${format;%s-%s;hello;world}"))
				.isEqualTo("hello-world");
			assertThat(p.getReplacer().process("${format;%d items;42}"))
				.isEqualTo("42 items");
		}
	}

	// ===== get.md =====
	@Test
	public void testGet() throws IOException {
		try (Processor p = new Processor()) {
			// get gets an item from a list by index: ${get;<index>;<list>}
			assertThat(p.getReplacer().process("${get;2;a,b,c,d}")).isEqualTo("c");
			assertThat(p.getReplacer().process("${get;0;first,second,third}")).isEqualTo("first");
			assertThat(p.getReplacer().process("${get;-1;a,b,c}")).isEqualTo("c"); // negative index from end
		}
	}

	// ===== indexof.md =====
	@Test
	public void testIndexof() throws IOException {
		try (Processor p = new Processor()) {
			// Syntax is ${indexof;<value>;<list>} not ${indexof;<list>;<value>}
			assertThat(p.getReplacer().process("${indexof;c;a,b,c,d}")).isEqualTo("2");
			assertThat(p.getReplacer().process("${indexof;missing;a,b,c}")).isEqualTo("-1");
		}
	}

	// ===== is.md =====
	@Test
	public void testIs() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${is;foo;foo}")).isEqualTo("true");
			assertThat(p.getReplacer().process("${is;foo;bar}")).isEqualTo("false");
			assertThat(p.getReplacer().process("${is;a;a;a;a}")).isEqualTo("true");
		}
	}

	// ===== isdir.md =====
	@Test
	public void testIsdir(@InjectTemporaryDirectory
	File tempDir) throws IOException {
		try (Processor p = new Processor()) {
			File dir = tempDir.toPath()
				.resolve("testdir")
				.toFile();
			dir.mkdir();
			File file = tempDir.toPath()
				.resolve("testfile.txt")
				.toFile();
			IO.store("test", file);
			p.setBase(tempDir);

			// isdir requires absolute paths or paths relative to base
			assertThat(p.getReplacer().process("${isdir;" + dir.getAbsolutePath() + "}")).isEqualTo("true");
			assertThat(p.getReplacer().process("${isdir;" + file.getAbsolutePath() + "}")).isEqualTo("false");
		}
	}

	// ===== isempty.md =====
	@Test
	public void testIsempty() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${isempty;}")).isEqualTo("true");
			assertThat(p.getReplacer().process("${isempty;  }")).isEqualTo("true");
			assertThat(p.getReplacer().process("${isempty;text}")).isEqualTo("false");
		}
	}

	// ===== isfile.md =====
	@Test
	public void testIsfile(@InjectTemporaryDirectory
	File tempDir) throws IOException {
		try (Processor p = new Processor()) {
			File file = tempDir.toPath()
				.resolve("test.txt")
				.toFile();
			IO.store("test", file);
			File dir = tempDir.toPath()
				.resolve("testdir")
				.toFile();
			dir.mkdir();
			p.setBase(tempDir);

			// isfile needs absolute path
			assertThat(p.getReplacer().process("${isfile;" + file.getAbsolutePath() + "}")).isEqualTo("true");
			assertThat(p.getReplacer().process("${isfile;" + dir.getAbsolutePath() + "}")).isEqualTo("false");
		}
	}

	// ===== isnumber.md =====
	@Test
	public void testIsnumber() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${isnumber;123}")).isEqualTo("true");
			assertThat(p.getReplacer().process("${isnumber;12.34}")).isEqualTo("true");
			assertThat(p.getReplacer().process("${isnumber;abc}")).isEqualTo("false");
		}
	}

	// ===== join.md =====
	@Test
	public void testJoin() throws IOException {
		try (Processor p = new Processor()) {
			// join combines lists with commas, use sjoin for custom separators
			assertThat(p.getReplacer().process("${join;apple,banana;cherry,date}"))
				.isEqualTo("apple,banana,cherry,date");
			assertThat(p.getReplacer().process("${join;red,green;blue;yellow,orange}"))
				.isEqualTo("red,green,blue,yellow,orange");
		}
	}

	// ===== last.md =====
	@Test
	public void testLast() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${last;a,b,c}")).isEqualTo("c");
			assertThat(p.getReplacer().process("${last;single}")).isEqualTo("single");
		}
	}

	// ===== lastindexof.md =====
	@Test
	public void testLastindexof() throws IOException {
		try (Processor p = new Processor()) {
			// Syntax is ${lastindexof;<value>;<list>} not ${lastindexof;<list>;<value>}
			assertThat(p.getReplacer().process("${lastindexof;b;a,b,c,b,d}")).isEqualTo("3");
			assertThat(p.getReplacer().process("${lastindexof;missing;a,b,c}")).isEqualTo("-1");
		}
	}

	// ===== length.md =====
	@Test
	public void testLength() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${length;hello}")).isEqualTo("5");
			assertThat(p.getReplacer().process("${length;}")).isEqualTo("0");
		}
	}

	// ===== literal.md =====
	@Test
	public void testLiteral() throws IOException {
		try (Processor p = new Processor()) {
			// literal wraps the text to make it a macro reference
			assertThat(p.getReplacer().process("${literal;foo}")).isEqualTo("${foo}");
			assertThat(p.getReplacer().process("${literal;version}")).isEqualTo("${version}");
		}
	}

	// ===== map.md =====
	@Test
	public void testMap() throws IOException {
		try (Processor p = new Processor()) {
			// map calls a macro with ${macro;value} for each element - use ${1}
			p.setProperty("template", "value-${1}");
			assertThat(p.getReplacer().process("${map;template;a,b,c}"))
				.isEqualTo("value-a,value-b,value-c");
		}
	}

	// ===== matches.md =====
	@Test
	public void testMatches() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${matches;hello123;.*\\d+.*}")).isEqualTo("true");
			assertThat(p.getReplacer().process("${matches;hello;.*\\d+.*}")).isEqualTo("false");
		}
	}

	// ===== max.md =====
	@Test
	public void testMax() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${max;apple,zebra,banana}")).isEqualTo("zebra");
		}
	}

	// ===== min.md =====
	@Test
	public void testMin() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${min;apple,zebra,banana}")).isEqualTo("apple");
		}
	}

	// ===== nmax.md =====
	@Test
	public void testNmax() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${nmax;5,20,15,3}")).isEqualTo("20");
		}
	}

	// ===== nmin.md =====
	@Test
	public void testNmin() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${nmin;5,20,15,3}")).isEqualTo("3");
		}
	}

	// ===== nsort.md =====
	@Test
	public void testNsort() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${nsort;5,20,3,15}")).isEqualTo("3,5,15,20");
		}
	}

	// ===== osfile.md =====
	@Test
	@EnabledOnOs(WINDOWS)
	public void testOsfile_Windows() throws IOException {
		try (Processor p = new Processor()) {
			// On Windows, backslashes in the path parameter need to be escaped
			String result = p.getReplacer().process("${osfile;C:/Users/user;project\\\\src\\\\Main.java}");
			assertThat(result).contains("\\project\\src\\Main.java");
		}
	}

	@Test
	@DisabledOnOs(WINDOWS)
	public void testOsfile_Unix() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${osfile;/home/user;project/src/Main.java}");
			assertThat(result).isEqualTo("/home/user/project/src/Main.java");
		}
	}

	// ===== random.md and rand.md =====
	@Test
	public void testRand() throws IOException {
		try (Processor p = new Processor()) {
			// rand with arg generates in range
			String result = p.getReplacer().process("${rand;100}");
			// Should be a random integer between 0 and 99
			int value = Integer.parseInt(result);
			assertThat(value).isBetween(0, 99);
		}
	}

	@Test
	public void testRand_WithMax() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${rand;50}");
			int value = Integer.parseInt(result);
			assertThat(value).isBetween(0, 50);
		}
	}

	// ===== replace.md =====
	@Test
	public void testReplace() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${replace;hello world;world;Java}"))
				.isEqualTo("hello Java");
			assertThat(p.getReplacer().process("${replace;test123test;\\d+;NUM}"))
				.isEqualTo("testNUMtest");
		}
	}

	// ===== replacelist.md =====
	@Test
	public void testReplacelist() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${replacelist;a,b,c;b;x}"))
				.isEqualTo("a,x,c");
		}
	}

	// ===== replacestring.md =====
	@Test
	public void testReplacestring() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${replacestring;hello;l;r}"))
				.isEqualTo("herro");
		}
	}

	// ===== reverse.md =====
	@Test
	public void testReverse() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${reverse;a,b,c}")).isEqualTo("c,b,a");
		}
	}

	// ===== size.md =====
	@Test
	public void testSize() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${size;a,b,c,d}")).isEqualTo("4");
			assertThat(p.getReplacer().process("${size;}")).isEqualTo("0");
		}
	}

	// ===== sort.md =====
	@Test
	public void testSort() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${sort;banana,apple,cherry}"))
				.isEqualTo("apple,banana,cherry");
		}
	}

	// ===== split.md =====
	@Test
	public void testSplit() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${split;-;hello-world-test}"))
				.isEqualTo("hello,world,test");
		}
	}

	// ===== startswith.md =====
	@Test
	public void testStartswith() throws IOException {
		try (Processor p = new Processor()) {
			// startswith returns the string if it starts with prefix, empty string otherwise
			assertThat(p.getReplacer().process("${startswith;hello.txt;hello}")).isEqualTo("hello.txt");
			assertThat(p.getReplacer().process("${startswith;hello.txt;world}")).isEmpty();
		}
	}

	// ===== sublist.md =====
	@Test
	public void testSublist() throws IOException {
		try (Processor p = new Processor()) {
			// Syntax is ${sublist;<start>;<end>;<list>} - start and end come BEFORE list
			assertThat(p.getReplacer().process("${sublist;1;3;a,b,c,d,e}")).isEqualTo("b,c");
			assertThat(p.getReplacer().process("${sublist;2;5;a,b,c,d,e}")).isEqualTo("c,d,e");
		}
	}

	// ===== substring.md =====
	@Test
	public void testSubstring() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${substring;hello world;0;5}")).isEqualTo("hello");
			assertThat(p.getReplacer().process("${substring;hello world;6}")).isEqualTo("world");
		}
	}

	// ===== sum.md =====
	@Test
	public void testSum() throws IOException {
		try (Processor p = new Processor()) {
			// sum returns integer when result is whole number
			assertThat(p.getReplacer().process("${sum;1,2,3,4,5}")).isEqualTo("15");
			assertThat(p.getReplacer().process("${sum;10,20;30}")).isEqualTo("60");
		}
	}

	// ===== tolower.md =====
	@Test
	public void testTolower() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${tolower;HELLO}")).isEqualTo("hello");
		}
	}

	// ===== toupper.md =====
	@Test
	public void testToupper() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${toupper;hello}")).isEqualTo("HELLO");
		}
	}

	// ===== trim.md =====
	@Test
	public void testTrim() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${trim;  hello  }")).isEqualTo("hello");
		}
	}

	// ===== uniq.md =====
	@Test
	public void testUniq() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${uniq;a,b,a,c,b}")).isEqualTo("a,b,c");
		}
	}

	// ===== vmax.md =====
	@Test
	public void testVmax() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${vmax;1.0.0,2.1.0,1.5.0}")).isEqualTo("2.1.0");
		}
	}

	// ===== vmin.md =====
	@Test
	public void testVmin() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${vmin;1.0.0,2.1.0,1.5.0}")).isEqualTo("1.0.0");
		}
	}

	// ===== warning.md =====
	@Test
	public void testWarning() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${warning;This is a warning message}");
			assertThat(result).isEmpty();
			// Check that warning was added to processor
			assertThat(p.getWarnings()).hasSize(1);
			assertThat(p.getWarnings().get(0)).contains("This is a warning message");
		}
	}

	// ===== basename.md =====
	@Test
	public void testBasename(@InjectTemporaryDirectory
	File tempDir) throws IOException {
		try (Processor p = new Processor()) {
			File testFile = new File(tempDir, "test.txt");
			IO.store("content", testFile);

			assertThat(p.getReplacer()
				.process("${basename;" + testFile.getPath() + "}")).isEqualTo("test.txt");
			assertThat(p.getReplacer()
				.process("${basename;" + testFile.getParent() + "}")).isEqualTo("testBasename");
		}
	}

	// ===== bndversion.md =====
	@Test
	public void testBndversion() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${bndversion}");
			// Should return a version string like "7.0.0" or similar
			assertThat(result).matches("\\d+\\.\\d+\\.\\d+.*");
		}
	}

	// ===== bsn.md =====
	@Test
	public void testBsn() throws Exception {
		try (Analyzer p = new Analyzer()) {
			p.setJar(IO.getFile("jar/osgi.jar"));
			p.setProperty("Bundle-SymbolicName", "com.example.mybundle");
			p.analyze();
			String result = p.getReplacer().process("${bsn}");
			assertThat(result).isEqualTo("com.example.mybundle");
		}
	}

	// ===== compare.md =====
	@Test
	public void testCompare() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${compare;apple;banana}")).isEqualTo("-1");
			assertThat(p.getReplacer().process("${compare;banana;apple}")).isEqualTo("1");
			assertThat(p.getReplacer().process("${compare;apple;apple}")).isEqualTo("0");
		}
	}

	// ===== decorated.md =====
	@Test
	public void testDecorated() throws IOException {
		try (Processor p = new Processor()) {
			p.setProperty("mykey", "value");
			p.setProperty("mykey.decorator", "decorated_value");
			String result = p.getReplacer().process("${decorated;mykey}");
			assertThat(result).isEqualTo("value,decorated_value");
		}
	}

	// ===== driver.md =====
	@Test
	public void testDriver() throws IOException {
		// see test test.WorkspaceTest.testDriver()
		// we just keep it here so that we know there are tests for the examples
		// in the docs
	}

	// ===== ee.md =====
	@Test
	public void testEe() throws Exception {
		try (Analyzer p = new Analyzer()) {
			p.setJar(IO.getFile("jar/osgi.jar"));
			p.analyze();
			String result = p.getReplacer().process("${ee}");
			// Returns the highest EE in the jar
			assertThat(result).isEqualTo("J2SE-1.2");
		}
	}

	// ===== error.md =====
	@Test
	public void testError() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${error;This is an error message}");
			assertThat(result).isEmpty();
			// Check that error was added to processor
			assertThat(p.getErrors()).hasSize(1);
			assertThat(p.getErrors().get(0)).contains("This is an error message");
		}
	}

	// ===== exporters.md =====
	@Test
	public void testExporters() throws Exception {
		try (Analyzer p = new Analyzer()) {
			p.addClasspath(IO.getFile("jar/osgi.jar"));
			p.setJar(IO.getFile("jar/osgi.jar"));
			p.setProperty("Export-Package", "*");
			p.analyze();
			String result = p.getReplacer()
				.process("${exporters;org.osgi.framework}");
			// Returns comma-separated list of exporters
			assertThat(result).contains("osgi");
		}
	}

	// ===== exports.md =====
	@Test
	public void testExports() throws Exception {
		try (Analyzer p = new Analyzer()) {
			p.setJar(IO.getFile("jar/osgi.jar"));
			p.setProperty("Export-Package", "*");
			p.analyze();
			String result = p.getReplacer().process("${exports}");
			// Returns comma-separated list of exported packages contains a few
			// of expected
			assertThat(result).containsAnyOf("org.osgi.application", "org.osgi.framework");
		}
	}

	// ===== extension.md =====
	@Test
	public void testExtension() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${extension;file.txt}")).isEqualTo("txt");
			assertThat(p.getReplacer().process("${extension;file.tar.gz}")).isEqualTo("gz");
			assertThat(p.getReplacer().process("${extension;noext}")).isEmpty();
		}
	}

	// ===== fileuri.md =====
	@Test
	public void testFileuri() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${fileuri;/path/to/file.txt}");
			assertThat(result).startsWith("file:");
			assertThat(result).endsWith("/path/to/file.txt");
		}
	}

	// ===== findfile.md =====
	@Test
	public void testFindfile(@InjectTemporaryDirectory File tempDir) throws IOException {
		try (Processor p = new Processor()) {
			File testFile = new File(tempDir, "test.txt");
			IO.store("content", testFile);
			p.setBase(tempDir);

			String result = p.getReplacer().process("${findfile;test.txt}");
			assertThat(result).contains("test.txt");
		}
	}

	// ===== findname.md =====
	@Test
	public void testFindname() throws Exception {
		try (Analyzer p = new Analyzer()) {
			p.setJar(IO.getFile("jar/osgi.jar"));
			p.analyze();
			// This macro searches bundle resources
			String result = p.getReplacer()
				.process("${findname;.*\\.MF}");
			// Returns list of matching resource names
			assertThat(result).isEqualTo("MANIFEST.MF");
			// return all resources in the jar. test just for some
			assertThat(p.getReplacer()
				.process("${findname}")).containsAnyOf("LICENSE", "MANIFEST.MF");
		}
	}

	// ===== findpath.md =====
	@Test
	public void testFindpath() throws Exception {
		try (Analyzer p = new Analyzer()) {
			p.setJar(IO.getFile("jar/osgi.jar"));
			p.analyze();
			// This macro searches bundle resources
			String result = p.getReplacer()
				.process("${findpath;.*\\.MF}");
			// Returns list of matching resource paths
			assertThat(result).isEqualTo("META-INF/MANIFEST.MF");
			// return all resources in the jar. test just for some
			assertThat(p.getReplacer()
				.process("${findname}")).containsAnyOf("LICENSE", "META-INF/MANIFEST.MF");
		}
	}

	// ===== fmodified.md =====
	@Test
	public void testFmodified(@InjectTemporaryDirectory File tempDir) throws IOException {
		try (Processor p = new Processor()) {
			File testFile = new File(tempDir, "test.txt");
			IO.store("content", testFile);
			p.setBase(tempDir);

			String result = p.getReplacer()
				.process("${fmodified;" + testFile.getPath() + "}");
			// Returns timestamp as long
			assertThat(result).matches("\\d+");
			assertThat(Long.parseLong(result)).isGreaterThan(0);
		}
	}

	// ===== frange.md =====
	@Test
	public void testFrange() throws IOException {
		try (Processor p = new Processor()) {
			// frange converts version range to OSGi filter
			String result = p.getReplacer().process("${frange;[1.2.3,2)}");
			assertThat(result).contains("version>=1.2.3");
			assertThat(result).contains("version>=2");
		}
	}

	// ===== gestalt.md =====

	/**
	 * See {@link WorkspaceTest#testGestaltGlobal()} for more tests
	 */
	@Test
	public void testGestalt(@InjectTemporaryDirectory
	File tempDir) throws Exception {
		IO.copy(new File("testresources/ws-gestalt"), tempDir);
		Workspace.resetStatic();
		Attrs attrs = new Attrs();
		attrs.put("x", "10");
		Workspace.addGestalt("peter", attrs);
		try (Workspace w = Workspace.getWorkspace(tempDir)) {
			w.refresh(); // remove previous tests
			assertEquals("peter", w.getReplacer()
				.process("${gestalt;peter}"));
		} finally {
			Workspace.resetStatic();
		}
	}

	// ===== githead.md =====
	@Test
	public void testGithead() throws IOException {

		try (Builder b = new Builder();) {
			String s = b.getReplacer()
				.process("${githead}");
			assertTrue(Hex.isHex(s));
		}
	}

	// ===== glob.md =====
	@Test
	public void testGlob(@InjectTemporaryDirectory File tempDir) throws IOException {
		try (Processor p = new Processor()) {
			File testFile = new File(tempDir, "test.txt");
			IO.store("content", testFile);
			p.setBase(tempDir);

			String result = p.getReplacer().process("${glob;*.txt}");
			assertThat(result).contains(".*\\.txt");
		}
	}

	// ===== global.md =====
	/**
	 * TODO not sure how to test this. Test currently does not really test
	 * anything useful
	 */
	@Disabled
	public void testGlobal() throws IOException {
		try (Processor p = new Processor()) {
			// global requires workspace context
			String result = p.getReplacer().process("${global;key}");
			// May not expand in test context
			ensureDoesNotContainMacroLiteral(result);
		}
	}

	// ===== ide.md =====
	/**
	 * TODO need better test
	 */
	@Disabled
	public void testIde() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${ide}");
			// Returns current IDE or empty
			ensureDoesNotContainMacroLiteral(result);
		}
	}

	// ===== imports.md =====
	@Test
	public void testImports() throws Exception {

		try (Analyzer p = new Analyzer()) {
			p.addClasspath(IO.getFile("jar/osgi.jar"));
			p.setJar(IO.getFile("jar/osgi.jar"));
			p.analyze();
			String result = p.getReplacer()
				.process("${imports}");
			// Returns comma-separated list of imports
			assertThat(result).isEqualTo("javax.microedition.io,javax.servlet,javax.servlet.http,javax.xml.parsers");
		}

	}

	// ===== js.md =====
	@Test
	@Disabled("Deprecated: javascript script engine removed in Java 15")
	public void testJs() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${js;2 + 3}");
			assertThat(result).isEqualTo("5");

			result = p.getReplacer().process("${js;'hello'.toUpperCase()}");
			assertThat(result).isEqualTo("HELLO");
		}
	}

	// ===== list.md =====
	@Test
	public void testList() throws IOException {
		try (Processor p = new Processor()) {
			p.setProperty("key1", "value1");
			p.setProperty("key2", "value2");
			String result = p.getReplacer()
				.process("${list;key1;key2}");
			// Returns all property keys
			assertThat(result).contains("value1", "value2");
		}
	}

	// ===== long2date.md =====
	@Test
	public void testLong2date() throws IOException {
		try (Processor p = new Processor()) {
			// 0 is January 1, 1970
			String result = p.getReplacer().process("${long2date;0}");
			assertThat(result).contains("Thu Jan 01 00:00:00 UTC 1970");
		}
	}

	// ===== lsa.md =====
	@Test
	public void testLsa(@InjectTemporaryDirectory File tempDir) throws IOException {
		try (Processor p = new Processor()) {
			File testFile = new File(tempDir, "test.txt");
			IO.store("content", testFile);
			p.setBase(tempDir);

			String result = p.getReplacer().process("${lsa;.}");
			// Returns absolute paths
			assertThat(result).contains("test.txt");
		}
	}

	// ===== lsr.md =====
	@Test
	public void testLsr(@InjectTemporaryDirectory File tempDir) throws IOException {
		try (Processor p = new Processor()) {
			File testFile = new File(tempDir, "test.txt");
			IO.store("content", testFile);
			p.setBase(tempDir);

			String result = p.getReplacer().process("${lsr;.}");
			// Returns relative paths
			assertThat(result).contains("test.txt");
		}
	}

	// ===== maven_version.md =====
	@Test
	public void testMavenVersion() throws IOException {
		try (Builder p = new Builder()) {
			assertThat(p.getReplacer().process("${maven_version;1.2.3}")).isEqualTo("1.2.3");
			assertThat(p.getReplacer()
				.process("${maven_version;1.2.3-SNAPSHOT}")).isEqualTo("1.2.3.SNAPSHOT");
		}
	}

	// ===== md5.md =====
	@Test
	public void testMd5(@InjectTemporaryDirectory
	File tempDir) throws Exception {

		try (Builder p = new Builder()) {
			p.setJar(IO.getFile("jar/osgi.jar"));
			p.setBase(tempDir);
			p.analyze();
			String result = p.getReplacer()
				.process("${md5;LICENSE}");
			// MD5 of the LICENSE file
			assertThat(result).isEqualTo("O4Pvljh/FGVfyFTdw8a9Vw==");

			String resultHex = p.getReplacer()
				.process("${md5;LICENSE;hex}");
			// MD5 as hex
			assertThat(resultHex).isEqualTo("3B83EF96387F14655FC854DDC3C6BD57");
		}
	}

	// ===== native_capability.md =====
	@Test
	public void testNativeCapability() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${native_capability}");
			// Returns native capability based on current OS
			assertThat(result).contains("osgi.native");
		}
	}

	// ===== ncompare.md =====
	@Test
	public void testNcompare() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${ncompare;10;20}")).isEqualTo("-1");
			assertThat(p.getReplacer().process("${ncompare;20;10}")).isEqualTo("1");
			assertThat(p.getReplacer().process("${ncompare;15;15}")).isEqualTo("0");
		}
	}

	// ===== now.md =====
	@Test
	public void testNow() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer()
				.process("${now;long}");
			// Should return current timestamp as long
			assertThat(result).matches("\\d+");
			assertThat(Long.parseLong(result)).isGreaterThan(0);

			String resultFormat = p.getReplacer()
				.process("${now;yyyy-MM-dd HH:mm:ss}");
			// Should return current timestamp as formatted string
			String regex = "^(?:\\d{4})-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01]) (?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$";
			assertThat(resultFormat).matches(regex);
		}
	}

	// ===== p_allsourcepath.md =====
	@Test
	public void testPAllsourcepath(@InjectTemporaryDirectory
	File tempDir) throws Exception {
		assertWorkspaceAndProject(tempDir, "testresources/ws", "p-stale", p -> {
			assertThat(IO.normalizePath(p.getReplacer()
				.process("${p_allsourcepath}"))).containsAnyOf("p-stale/src", "p-stale-dep/src");
		});
	}

	// ===== p_bootclasspath.md =====
	/**
	 * Don't know how to test this.
	 */
	@Disabled
	public void testPBootclasspath() throws Exception {
		// TODO don't know how to test setting the bootclasspath
	}

	// ===== p_buildpath.md =====
	@Test
	public void testPBuildpath(@InjectTemporaryDirectory
	File tempDir) throws Exception {
		assertWorkspaceAndProject(tempDir, "testresources/ws", "p3", p -> {
			assertThat(p.getReplacer()
				.process("${p_buildpath}")).containsAnyOf("org.apache.felix.configadmin-1.0.1.jar");
		});

	}

	// ===== p_dependson.md =====
	@Test
	public void testPDependson(@InjectTemporaryDirectory
	File tempDir) throws Exception {
		assertWorkspaceAndProject(tempDir, "testresources/ws", "p-stale", p -> {
			assertThat(p.getReplacer()
				.process("${p_dependson}")).endsWith("p-stale-dep");
		});

	}

	// ===== p_output.md =====
	@Test
	public void tetstPOutput(@InjectTemporaryDirectory
	File tempDir) throws Exception {
		assertWorkspaceAndProject(tempDir, "testresources/ws-repo-test", "p1", p -> {
			assertThat(p.getReplacer().process("${p_output}")).endsWith("p1/bin");
		});

	}

	private void assertWorkspaceAndProject(File tempDir, String wspath, String project, Consumer<Project> s)
		throws Exception {

		IO.copy(IO.getFile(wspath), tempDir);
		try (Workspace ws = new Workspace(tempDir)) {
			Project p = ws.getProject(project);
			s.accept(p);
		}
	}

	// ===== p_sourcepath.md =====
	@Test
	public void testPSourcepath(@InjectTemporaryDirectory
	File tempDir) throws Exception {
		assertWorkspaceAndProject(tempDir, "testresources/ws-repo-test", "p1", p -> {
			assertThat(IO.normalizePath(p.getReplacer()
				.process("${p_sourcepath}"))).endsWith("p1/src");
		});
	}

	// ===== p_testpath.md =====
	@Test
	public void testPTestpath(@InjectTemporaryDirectory
	File tempDir) throws Exception {
		assertWorkspaceAndProject(tempDir, "testresources/ws", "multipath", p -> {
			// just test for some jars
			assertThat(p.getReplacer()
				.process("${p_testpath}")).containsAnyOf("org.apache.felix.configadmin-1.8.8.jar",
					"org.apache.felix.ipojo-1.0.0.jar");
		});
	}

	// ===== path.md =====
	@Test
	public void testPath() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer()
				.process("${path;/path/to/file.txt}")).isEqualTo("/path/to/file.txt");
			assertThat(p.getReplacer()
				.process("${path;/path/to/}")).isEqualTo("/path/to/");
		}
	}

	// ===== pathseparator.md =====
	@Test
	@EnabledOnOs(WINDOWS)
	public void testPathseparatorWindows() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${pathseparator}");
			assertThat(result).isEqualTo(";");
		}
	}

	@Test
	@DisabledOnOs(WINDOWS)
	public void testPathseparatorUnix() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${pathseparator}");
			assertThat(result).isEqualTo(":");
		}
	}

	// ===== permissions.md =====
	@Test
	public void testPermissions() throws IOException {
		// see tests in PermissionGeneratorTest
		// we just keep it here so that we know there are tests for the examples
		// in the docs
	}

	// ===== propertiesdir.md =====
	@Test
	public void testPropertiesdir() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${propertiesdir}");
			// Returns directory containing bnd properties file
			ensureDoesNotContainMacroLiteral(result);
		}
	}

	// ===== propertiesname.md =====
	@Test
	public void testPropertiesname() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${propertiesname}");
			// Returns name of bnd properties file
			ensureDoesNotContainMacroLiteral(result);
		}
	}

	// ===== range.md =====
	@Test
	public void testRange() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer()
				.process("${range;[==,=+);1.0.0}")).isEqualTo("[1.0,1.1)");
			assertThat(p.getReplacer()
				.process("${range;[===,+==);1.0.0}")).isEqualTo("[1.0.0,2.0.0)");
		}
	}

	// ===== reject.md =====
	@Test
	public void testReject() throws IOException {
		try (Processor p = new Processor()) {
			// reject uses pattern matching, not literal matching
			String result = p.getReplacer().process("${reject;a,b,c,d;.*b.*}");
			assertThat(result).isEqualTo("a,c,d");
		}
	}

	// ===== removeall.md =====
	@Test
	public void testRemoveall() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${removeall;a,b,c,d,e;b,d}");
			assertThat(result).isEqualTo("a,c,e");
		}
	}

	// ===== repodigests.md =====
	/**
	 * Don't know how to test this.
	 */
	@Disabled
	public void testRepodigests(@InjectTemporaryDirectory
	File tempDir) throws Exception {
		// TODO come up with a test, but have not found no class implementing
		// RepositoryDigest interface

	}

	// ===== repos.md =====
	@Test
	public void testRepos(@InjectTemporaryDirectory
	File tempDir) throws Exception {
		IO.copy(IO.getFile("testresources/ws-repo-test"), tempDir);
		try (Workspace ws = new Workspace(tempDir)) {
			Project p = ws.getProject("p1");
			String result = p.getReplacer()
				.process("${repos}");
			// Returns list of repositories - may be empty in test context
			ensureDoesNotContainMacroLiteral(result);
			assertThat(result).containsAnyOf("bnd-cache", "Release", "Repo");
		}
	}

	// ===== retainall.md =====
	@Test
	public void testRetainall() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${retainall;a,b,c,d,e;b,d,f}");
			assertThat(result).isEqualTo("b,d");
		}
	}

	// ===== select.md =====
	@Test
	public void testSelect() throws IOException {
		try (Processor p = new Processor()) {
			// select uses pattern matching, not literal matching
			String result = p.getReplacer().process("${select;a,b,c,d;.*b.*}");
			assertThat(result).isEqualTo("b");
		}
	}

	// ===== separator.md =====
	@Test
	@EnabledOnOs(WINDOWS)
	public void testSeparatorWindows() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${separator}");
			assertThat(result).isEqualTo("\\");
		}
	}

	@Test
	@DisabledOnOs(WINDOWS)
	public void testSeparatorUnix() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${separator}");
			assertThat(result).isEqualTo("/");
		}
	}

	// ===== sha1.md =====
	@Test
	public void testSha1(@InjectTemporaryDirectory
	File tempDir) throws Exception {

		try (Builder p = new Builder()) {
			p.setJar(IO.getFile("jar/osgi.jar"));
			p.setBase(tempDir);
			p.analyze();
			String result = p.getReplacer()
				.process("${sha1;LICENSE}");
			// SHA1 of the LICENSE file
			assertThat(result).isEqualTo("K4uBUimqimHkg/tLoFiLi2xJGJA=");

			String resultHex = p.getReplacer()
				.process("${sha1;LICENSE;hex}");
			// sha1as hex
			assertThat(resultHex).isEqualTo("2B8B815229AA8A61E483FB4BA0588B8B6C491890");
		}


	}

	// ===== sjoin.md =====
	@Test
	public void testSjoin() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${sjoin;|;a;b;c}");
			assertThat(result).isEqualTo("a|b|c");
		}
	}

	// ===== stem.md =====
	@Test
	public void testStem() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer()
				.process("${stem;foo.bar}")).isEqualTo("foo");
			assertThat(p.getReplacer()
				.process("${stem;archive.tar.gz}")).isEqualTo("archive");
		}
	}

	// ===== subst.md =====
	@Test
	public void testSubst() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${subst;hello world;world;universe}");
			assertThat(result).isEqualTo("hello universe");
		}
	}

	// ===== system_allow_fail.md =====
	@Test
	public void testSystemAllowFail() throws IOException {
		try (Processor p = new Processor()) {
			// Execute command that should succeed
			String result = p.getReplacer().process("${system_allow_fail;echo test}");
			assertThat(result).contains("test");

			// Execute command that fails - should not throw
			result = p.getReplacer().process("${system_allow_fail;false}");
			ensureDoesNotContainMacroLiteral(result);
		}
	}

	// ===== system.md =====
	@Test
	public void testSystem() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${system;echo test}");
			assertThat(result).contains("test");
		}
	}

	// ===== thisfile.md =====
	@Test
	public void testThisfile(@InjectTemporaryDirectory
	File tempDir) throws IOException {
		try (Processor p = new Processor()) {
			File testFile = tempDir.toPath()
				.resolve("test.txt")
				.toFile();
			IO.store("Hello World", testFile);
			p.setProperties(testFile);
			String result = p.getReplacer().process("${thisfile}");
			// Returns current bnd file being processed
			assertThat(result).isEqualTo(IO.normalizePath(testFile));
		}
	}

	// ===== toclaspath.md =====
	@Test
	public void testToclasspath() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer()
				.process("${toclasspath;com.example.MyClass}")).isEqualTo("com/example/MyClass.class");
		}
	}

	// ===== toclassname.md =====
	@Test
	public void testToclassname() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer()
				.process("${toclassname;com/example/MyClass.class}"))
				.isEqualTo("com.example.MyClass");
			assertThat(p.getReplacer().process("${toclassname;com/example/MyClass.class}"))
				.isEqualTo("com.example.MyClass");
		}
	}

	// ===== tstamp.md =====
	@Test
	public void testTstamp() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${tstamp}");
			// Returns formatted timestamp
			assertThat(result).matches("\\d{12}");
		}
	}

	// ===== unescape.md =====
	@Test
	public void testUnescape() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${unescape;\\n}")).isEqualTo("\n");
			assertThat(p.getReplacer().process("${unescape;\\t}")).isEqualTo("\t");
			assertThat(p.getReplacer()
				.process("${unescape;\\\\}")).isEqualTo("\\\\");
		}
	}

	// ===== uri.md =====
	@Test
	public void testUri() throws IOException {
		try (Processor p = new Processor()) {
			String result = p.getReplacer().process("${uri;/path/to/file.txt}");
			assertThat(result).startsWith("file:");
		}
	}

	// ===== vcompare.md =====
	@Test
	public void testVcompare() throws IOException {
		try (Processor p = new Processor()) {
			assertThat(p.getReplacer().process("${vcompare;1.0.0;2.0.0}")).isEqualTo("-1");
			assertThat(p.getReplacer().process("${vcompare;2.0.0;1.0.0}")).isEqualTo("1");
			assertThat(p.getReplacer().process("${vcompare;1.0.0;1.0.0}")).isEqualTo("0");
		}
	}

	// ===== workspace.md =====
	@Test
	public void testWorkspace(@InjectTemporaryDirectory
	File tempDir) throws Exception {
		IO.copy(new File("testresources/ws-gestalt"), tempDir);
		Workspace.resetStatic();
		Attrs attrs = new Attrs();
		try (Workspace w = Workspace.getWorkspace(tempDir)) {
			w.refresh(); // remove previous tests

			String result = w.getReplacer()
				.process("${workspace}");
			assertThat(result).contains(IO.normalizePath(tempDir));

		} finally {
			Workspace.resetStatic();
		}

	}

	private void ensureDoesNotContainMacroLiteral(String result) {
		assertThat(result).isNotNull()
			.doesNotContain("${");
	}

	// Additional tests can be added here for other macros
}
