package aQute.lib.strings;

import static org.assertj.core.api.Assertions.assertThat;

import junit.framework.TestCase;

public class StringsTest extends TestCase {

	public void testTimes() {
		assertEquals("----", Strings.times("-", 4));
		assertEquals("-", Strings.times("-", -100));

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
