package aQute.lib.strings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class StringsTest {

	@Test
	public void testTimes() {
		assertEquals("----", Strings.times("-", 4));
		assertEquals("-", Strings.times("-", -100));

	}

	@Test
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

	@Test
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

	@Test
	public void testCompareExceptWithNoFirstMatch() {

		assertThat(Strings.compareExcept("foo", "xfoo", Pattern.compile("(x+)"))).isFalse();
	}

	@Test
	public void testCompareExceptWithNoSecondMatch() {

		assertThat(Strings.compareExcept("foox", "foo", Pattern.compile("(x+)"))).isFalse();
	}

	@Test
	public void testCompareExceptWithPrefixDifferentLength() {

		assertThat(Strings.compareExcept("abcxfoo", "abxfoo", Pattern.compile("(x+)"))).isFalse();
	}

	@Test
	public void testCompareExceptWithPrefixDifference() {

		assertThat(Strings.compareExcept("abcxfoo", "abdxfoo", Pattern.compile("(x+)"))).isFalse();
	}

	@Test
	public void testCompareExceptWithSuffixDifference() {

		assertThat(Strings.compareExcept("abcxfoo", "abcxfox", Pattern.compile("(x+)"))).isFalse();
	}

	@Test
	public void testStrip() {
		assertEquals("abcd", Strings.stripSuffix("abcdef", "ef"));
		assertEquals("a", Strings.stripSuffix("abcdef", "b.*f"));
		assertEquals("", Strings.stripPrefix("abcdef", "a.*f"));
		assertEquals("cdef", Strings.stripPrefix("abcdef", "ab"));
	}

	@Test
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

	@Test
	public void testTrim() {
		assertEquals("", Strings.trim(""));
		assertEquals("", Strings.trim("    "));
		assertEquals("", Strings.trim("\r\n\r\n\t\f\n\r"));
		assertEquals("a", Strings.trim("  a "));
		assertEquals("a", Strings.trim("  a\n\r"));
		assertEquals("a", Strings.trim("\r\n\r\na\t\f\n\r"));
		assertEquals("a b", Strings.trim("\r\n\r\na b\t\f\n\r"));
	}

	@Test
	public void testSplit() {
		assertThat(Strings.split("  a,  b ,,c  ")).containsSequence("a", "b", "c");
		assertThat(Strings.split("  a; version=\"[1,2)\",  'b' ,,c  ")).containsSequence("a; version=\"[1", "2)\"",
			"'b'", "c");
		assertThat(Strings.splitAsStream("  a,  b ,,c  ")).containsSequence("a", "b", "c");
		assertThat(Strings.splitAsStream("  a; version=\"[1,2)\",  'b' ,,c  ")).containsSequence("a; version=\"[1",
			"2)\"", "'b'", "c");
	}

	@Test
	public void testSplitLines() {
		assertThat(Strings.splitLinesAsStream("x")).containsSequence("x");
		assertThat(Strings.splitLinesAsStream("a\nb\r\n\nc  d")).containsSequence("a", "b", "", "c  d");
		assertThat(Strings.splitLinesAsStream(" a \n b \r\n\n c  d \r\n")).containsSequence(" a ", " b ", "", " c  d ");
	}

	@Test
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

	@Test
	public void testEscapeSlash() {
		Pattern p1 = Pattern.compile("[/\\$]");
		assertThat(Strings.escape("foobar", p1, '$')).isEqualTo("foobar");
		assertThat(Strings.escape("foo/bar", p1, '$')).isEqualTo("foo$002Fbar");
		assertThat(Strings.escape("foo/", p1, '$')).isEqualTo("foo$002F");
		assertThat(Strings.escape("/bar", p1, '$')).isEqualTo("$002Fbar");
		assertThat(Strings.escape("$/bar", p1, '$')).isEqualTo("$0024$002Fbar");
	}

	@Test
	public void testUnEscape() {
		Pattern p1 = Pattern.compile("[/\\$]");
		assertThat(Strings.unescape("foobar", '$')).isPresent()
			.get()
			.isEqualTo("foobar");
		assertThat(Strings.unescape("foo$002Fbar", '$')).isPresent()
			.get()
			.isEqualTo("foo/bar");
		assertThat(Strings.unescape("$002Fbar", '$')).isPresent()
			.get()
			.isEqualTo("/bar");
		assertThat(Strings.unescape("foo$002F", '$')).isPresent()
			.get()
			.isEqualTo("foo/");
		assertThat(Strings.unescape("foo$002F$002Fbar", '$')).isPresent()
			.get()
			.isEqualTo("foo//bar");
		assertThat(Strings.unescape("foo$0024$002F$002F$0024bar", '$')).isPresent()
			.get()
			.isEqualTo("foo$//$bar");
	}

	@Test
	public void testError() {
		Pattern p1 = Pattern.compile("[/\\$]");
		assertThat(Strings.unescape("foo$002Xbar", '$')).isNotPresent();
		assertThat(Strings.unescape("foo$002", '$')).isNotPresent();
		assertThat(Strings.unescape("foo$", '$')).isNotPresent();
		assertThat(Strings.unescape("foo$$", '$')).isNotPresent();
	}

	@ParameterizedTest(name = "Validate startsWithIgnoreCase {arguments}")
	@CsvSource(value = {
		"'foobar',''", //
		"'foobar','foo'", //
		"'foobar','Foo'", //
		"'foobar','FOO'", //
		"'FooBar','foo'", //
		"'FOOBAR','Foo'", //
		"'fOObar','FOO'", //
		"'foobar','foof'", //
		"'foobar','foobar'", //
		"'foobar','FooBar'", //
		"'foobar','foobarf'" //
	})
	@DisplayName("Validate startsWithIgnoreCase")
	void starts_with_ignore_case(String target, String prefix) {
		boolean expected = target.toLowerCase(Locale.ROOT)
			.startsWith(prefix.toLowerCase(Locale.ROOT));
		assertThat(Strings.startsWithIgnoreCase(target, prefix))
			.isEqualTo(expected);
	}

	@ParameterizedTest(name = "Validate startsWithIgnoreCase index {arguments}")
	@CsvSource(value = {
		"'foobar','',2", //
		"'foobar','',8", //
		"'foobar','oba',2", //
		"'foobar','oBa',2", //
		"'foobar','OBA',2", //
		"'FooBar','oba',2", //
		"'FOObar','oBa',2", //
		"'FooBar','OBA',2", //
		"'foobar','obaf',2", //
		"'foobar','bar',3", //
		"'FOOBAR','bar',3", //
		"'foobar','bar',4", //
		"'foobar','bar',8" //
	})
	@DisplayName("Validate startsWithIgnoreCase index")
	void starts_with_ignore_case_offset(String target, String prefix, int offset) {
		boolean expected = target.toLowerCase(Locale.ROOT)
			.startsWith(prefix.toLowerCase(Locale.ROOT), offset);
		assertThat(Strings.startsWithIgnoreCase(target, prefix, offset))
			.isEqualTo(expected);
	}

	@ParameterizedTest(name = "Validate endsWithIgnoreCase {arguments}")
	@CsvSource(value = {
		"'foobar',''", //
		"'foobar','bar'", //
		"'foobar','Bar'", //
		"'foobar','BAR'", //
		"'FooBar','bar'", //
		"'FOOBAR','Bar'", //
		"'foobAR','BAR'", //
		"'foobar','fbar'", //
		"'foobar','foobar'", //
		"'foobar','FooBar'", //
		"'foobar','ffoobar'" //
	})
	@DisplayName("Validate endsWithIgnoreCase")
	void ends_with_ignore_case(String target, String suffix) {
		boolean expected = target.toLowerCase(Locale.ROOT)
			.endsWith(suffix.toLowerCase(Locale.ROOT));
		assertThat(Strings.endsWithIgnoreCase(target, suffix)).isEqualTo(expected);
	}

}
