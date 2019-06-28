package aQute.lib.strings;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class StringsTest extends TestCase {

	public void testTimes() {
		assertEquals("----", Strings.times("-", 4));
		assertEquals("-", Strings.times("-", -100));

	}

	public void testJoin() {
		assertThat(Strings.join("x", (Iterable<Object>) null)).isEqualTo("");
		assertThat(Strings.join("x", Collections.emptyList())).isEqualTo("");
		assertThat(Strings.join("x", Arrays.asList("a"))).isEqualTo("a");
		assertThat(Strings.join("x", Arrays.asList("a", "b"))).isEqualTo("axb");

		assertThat(Strings.join((Iterable<Object>) null)).isEqualTo("");
		assertThat(Strings.join(Collections.emptyList())).isEqualTo("");
		assertThat(Strings.join(Arrays.asList("a"))).isEqualTo("a");
		assertThat(Strings.join(Arrays.asList("a", "b"))).isEqualTo("a,b");
	}

	public void testCompareExcept() {

		assertThat(Strings.compareExcept("foox", "foo", Pattern.compile("(x+)"))).isFalse();

		assertThat(
			Strings.compareExcept("xfooxfooxxfooxxxfooxxxx", "xxxxxfooxfooxxxxxxxfooxfoox", Pattern.compile("(x+)")))
				.isTrue();
		assertThat(Strings.compareExcept("fooxy", "fooxy", Pattern.compile("(x+)"))).isTrue();
		assertThat(Strings.compareExcept("foox", "foox", Pattern.compile("(x+)"))).isTrue();
		assertThat(Strings.compareExcept("foo", "foo", Pattern.compile("(x+)"))).isTrue();
		assertThat(Strings.compareExcept("xxxxfoo", "xfoo", Pattern.compile("(x+)"))).isTrue();
		assertThat(Strings.compareExcept("fooxxxxxxxx", "foox", Pattern.compile("(x+)"))).isTrue();
		assertThat(Strings.compareExcept("abcxxxxxxxdefxxxxxghixxjkl",
			"abcxdefxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxghixxjkl", Pattern.compile("(x+)"))).isTrue();

		assertThat(Strings.compareExcept("<foo increment=\"1234567\"><xyz/></foo>",
			"<foo increment=\"8907\"><xyz/></foo>", Pattern.compile("increment=\"([0-9]+)\""))).isTrue();

		assertThat(Strings.compareExcept("abcxxxxxxxdef", "abcxdef", Pattern.compile("(x+)"))).isTrue();

	}

	public void testCompareExceptWithNoFirstMatch() {

		assertThat(Strings.compareExcept("foo", "xfoo", Pattern.compile("(x+)"))).isFalse();
	}

	public void testCompareExceptWithNoSecondMatch() {

		assertThat(Strings.compareExcept("foox", "foo", Pattern.compile("(x+)"))).isFalse();
	}

	public void testCompareExceptWithPrefixDifferentLength() {

		assertThat(Strings.compareExcept("abcxfoo", "abxfoo", Pattern.compile("(x+)"))).isFalse();
	}

	public void testCompareExceptWithPrefixDifference() {

		assertThat(Strings.compareExcept("abcxfoo", "abdxfoo", Pattern.compile("(x+)"))).isFalse();
	}

	public void testCompareExceptWithSuffixDifference() {

		assertThat(Strings.compareExcept("abcxfoo", "abcxfox", Pattern.compile("(x+)"))).isFalse();
	}

	public void testStrip() {
		assertEquals("abcd", Strings.stripSuffix("abcdef", "ef"));
		assertEquals("a", Strings.stripSuffix("abcdef", "b.*f"));
		assertEquals("", Strings.stripPrefix("abcdef", "a.*f"));
		assertEquals("cdef", Strings.stripPrefix("abcdef", "ab"));
	}

	public void testSubstrings() {
		assertEquals("abcdef", Strings.from("abcdef", 0));
		assertEquals("bcdef", Strings.from("abcdef", 1));
		assertEquals("f", Strings.from("abcdef", -1));
		assertEquals("cdef", Strings.from("abcdef", -4));
		assertEquals("ab", Strings.to("abcdef", -4));
		assertEquals("abcdef", Strings.to("abcdef", 0));
		assertEquals("abcd", Strings.to("abcdef", 4));
		assertEquals("bc", Strings.substring("abcdef", 1, 3));
		assertEquals("bcde", Strings.substring("abcdef", 1, -1));
		assertEquals("abcdef", Strings.substring("abcdef", 0, 0));
		assertEquals("af", Strings.delete("abcdef", 1, -1));
		assertEquals("", Strings.delete("abcdef", 0, 0));
		assertEquals("ace", Strings.substring("abcdef", 0, 0, 2));
		assertEquals("fedcba", Strings.substring("abcdef", 0, 0, -1));
		assertEquals("fdb", Strings.substring("abcdef", 0, 0, -2));
	}

	public void testTrim() {
		assertEquals("", Strings.trim(""));
		assertEquals("", Strings.trim("    "));
		assertEquals("", Strings.trim("\r\n\r\n\t\f\n\r"));
		assertEquals("a", Strings.trim("  a "));
		assertEquals("a", Strings.trim("  a\n\r"));
		assertEquals("a", Strings.trim("\r\n\r\na\t\f\n\r"));
		assertEquals("a b", Strings.trim("\r\n\r\na b\t\f\n\r"));
	}

	public void testSplit() {
		assertThat(Strings.split("  a,  b ,,c  ")).containsSequence("a", "b", "c");
		assertThat(Strings.split("  a; version=\"[1,2)\",  'b' ,,c  ")).containsSequence("a; version=\"[1", "2)\"",
			"'b'", "c");
		assertThat(Strings.splitAsStream("  a,  b ,,c  ")).containsSequence("a", "b", "c");
		assertThat(Strings.splitAsStream("  a; version=\"[1,2)\",  'b' ,,c  ")).containsSequence("a; version=\"[1",
			"2)\"", "'b'", "c");
	}

	public void testSplitLines() {
		assertThat(Strings.splitLinesAsStream("x")).containsSequence("x");
		assertThat(Strings.splitLinesAsStream("a\nb\r\n\nc  d")).containsSequence("a", "b", "", "c  d");
		assertThat(Strings.splitLinesAsStream(" a \n b \r\n\n c  d \r\n")).containsSequence(" a ", " b ", "", " c  d ");
	}

	public void testSplitQuoted() {
		assertThat(Strings.splitQuoted("  a,  b ,,c  ")).containsSequence("a", "b", "c");
		assertThat(Strings.splitQuoted("  a; version=\"[1,2)\",  'b' ,,\"c\"  "))
			.containsSequence("a; version=\"[1,2)\"", "'b'", "\"c\"");
		assertThat(Strings.splitQuoted("someone,quote=\"He said, \\\"What!?\\\"\"")).containsSequence("someone",
			"quote=\"He said, \\\"What!?\\\"\"");
		assertThat(Strings.splitQuotedAsStream("  a,  b ,,c  ")).containsSequence("a", "b", "c");
		assertThat(Strings.splitQuotedAsStream("  a; version=\"[1,2)\",  'b' ,,\"c\"  "))
			.containsSequence("a; version=\"[1,2)\"", "'b'", "\"c\"");
		assertThat(Strings.splitQuotedAsStream("someone,quote=\"He said, \\\"What!?\\\"\"")).containsSequence("someone",
			"quote=\"He said, \\\"What!?\\\"\"");
	}
}
