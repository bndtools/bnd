package aQute.libg.glob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class GlobTest {

	@Test
	public void testCaseInsensitive() {
		match("xx(?i)xx(?i)xx", "xx(?i)xx(?i)xx", "xxXXxx", "xxXxxx", "xxxxxx", "xxxxXX");

	}

	@Test
	public void testtail() {
		match("com.foo.*", "com\\.foo\\..*", "~com.foo", "com.foo.bar");
	}

	@Test
	public void testDifferentMappings() {

		match("[\\p{Lower}]", "[\\p{Lower}]", "e", "f", "~B");
		match("[a-z&&[^bc]]", "[a-z&&[^bc]]", "e", "f", "~b");

		match(".", "\\.", ".", "~x");
		match("^", "\\^", "^", "~x");
		match("$", "\\$", "$", "~x");
		match("@", "\\@", "@", "~x");
		match("%", "\\%", "%", "~x");

		match("\\.", "\\.", ".", "~x");
		match("\\^", "\\^", "^", "~x");
		match("\\$", "\\$", "$", "~x");
		match("\\@", "\\@", "@", "~x");
		match("\\%", "\\%", "%", "~x");

		match("(a).", "(a)\\.", "a.", "~ax");
		match("(a)^", "(a)\\^", "a^", "~ax");
		match("(a)$", "(a)\\$", "a$", "~ax");
		match("(a)@", "(a)\\@", "a@", "~ax");
		match("(a)%", "(a)\\%", "a%", "~ax");

		match("[a].", "[a]\\.", "a.", "~ax");
		match("[a]^", "[a]\\^", "a^", "~ax");
		match("[a]$", "[a]\\$", "a$", "~ax");
		match("[a]@", "[a]\\@", "a@", "~ax");
		match("[a]%", "[a]\\%", "a%", "~ax");

		match("{a}.", "(?:a)\\.", "a.", "~ax");
		match("{a}^", "(?:a)\\^", "a^", "~ax");
		match("{a}$", "(?:a)\\$", "a$", "~ax");
		match("{a}@", "(?:a)\\@", "a@", "~ax");
		match("{a}%", "(?:a)\\%", "a%", "~ax");

		match("?", ".", "a", "?");
		match("(a|b)?", "(a|b)?", "", "a", "b", "~abc");
		match("[ab]?", "[ab]?");
		match("{a,b}?", "(?:a|b)?", "", "a", "b");
		match("{a?,b}?", "(?:a.|b)?", "", "ax", "b");
		match("(a){1}?", "(a){1}.", "ax", "~a");

		match("*", ".*", "foobar", "");
		match("(a|b)*", "(a|b)*", "abbbababababa");
		match("[ab]*", "[ab]*", "abababababa", "~axaaabbabb");
		match("{a,b}*", "(?:a|b)*", "ababababb", "~axababab");
		match("{a*,b}?", "(?:a.*|b)?", "", "ax", "b");
		match("(a){1}*", "(a){1}.*", "a", "ax");

		match("+", "\\+", "+");
		match("(a|b)+", "(a|b)+", "a", "b", "~");
		match("[ab]+", "[ab]+", "aaaa", "~");
		match("{a,b}+", "(?:a|b)+", "abababa", "aaa", "bbb", "~x");
		match("{a+,b}+", "(?:a\\+|b)+", "a+a+");
		match("(a){1}+", "(a){1}\\+", "a+");

		match("{a}", "(?:a)", "a");
		match("(a){1}", "(a){1}", "a", "~aa");
		match("(a){1,2}", "(a){1,2}", "a", "aa");
		match("{1,2}", "(?:1|2)", "1", "2");
	}

	@Test
	public void testEscape() {
		match("[*]", "[*]", "*");
		match("\\*", "\\*", "*");
		match("\\?", "\\?", "?");
		match("\\+", "\\+", "+");

		match("\\(x\\)?", "\\(x\\).", "(x)X");
		match("\\(x\\)+", "\\(x\\)\\+", "(x)+");
		match("\\(x\\)*", "\\(x\\).*", "(x)foobar");

		match("\\[x\\]?", "\\[x\\].", "[x]X");
		match("\\[x\\]+", "\\[x\\]\\+", "[x]+");
		match("\\[x\\]*", "\\[x\\].*", "[x]foobar");

		match("\\{x\\}?", "\\{x\\}.", "{x}X");
		match("\\{x\\}+", "\\{x\\}\\+", "{x}+");
		match("\\{x\\}*", "\\{x\\}.*", "{x}foobar");
	}

	@Test
	public void testQuoted() {
		match("abc\\Q?*&%$#\\Edef", "abc\\Q?*&%$#\\Edef", "abc?*&%$#def");

	}

	@Test
	public void testBracketed() {

		match("[A-F]+", "[A-F]+", "AFFAED");

		match("[*]", "[*]", "*");
		match("[^x]", "[^x]", "y", "~x");
		match("[$]", "[$]", "$", "~€");
		match("[@]", "[@]", "@", "~€");
		match("[%]", "[%]", "%", "~€");

		match("[?]", "[?]", "?");
		match("[+]", "[+]", "+");
		match("[{}]", "[{}]", "{", "}");

		match("[\\\\*]", "[\\\\*]", "*", "\\");
		match("[\\\\?]", "[\\\\?]", "?", "\\");
		match("[\\\\+]", "[\\\\+]", "+", "\\");
		match("[\\\\{}]", "[\\\\{}]", "{", "}");
	}

	@Test
	public void testSimple() {
		match("*foo*", ".*foo.*", "foo", "aaaafooxxx", "food", "Xfood", "~ood");
	}

	@Test
	public void testCurlies() {
		match("(a){1,2}?", "(a){1,2}.");
		match("{[a],[b]}", "(?:[a]|[b])", "b");
		match("{[a]?,[b]}", "(?:[a]?|[b])", "b", "", "a");
		match("foobar.{java,groovy}", "foobar\\.(?:java|groovy)", "foobar.java", "foobar.groovy", "~foobar.cxx");
		match("{a,{b,c}}", "(?:a|(?:b|c))", "a", "b", "c");
		match("{a,{b,{c,d}}}", "(?:a|(?:b|(?:c|d)))", "a", "b", "c", "d");
		match("{a,b}{1,2}", "(?:a|b){1,2}", "b", "bb", "~bbb");
	}

	@Test
	public void testOr() {
		match("abc|def|ghi", "abc|def|ghi", "abc", "ghi");
		match("{abc,def,ghi}", "(?:abc|def|ghi)", "abc", "ghi");
	}

	@Test
	public void testUrl() {
		Glob glob;

		glob = new Glob("http://www.example.com/*");
		assertTrue(glob.matcher("http://www.example.com/repository.xml")
			.matches());
		assertFalse(glob.matcher("https://www.example.com/repository.xml")
			.matches());

		glob = new Glob("http://*.example.com/*");
		assertTrue(glob.matcher("http://www.example.com/repository.xml")
			.matches());
		assertTrue(glob.matcher("http://foo.example.com/repository.xml")
			.matches());
		assertFalse(glob.matcher("http://example.com/repository.xml")
			.matches());
		assertFalse(glob.matcher("http://foo.exampleXcom/repository.xml")
			.matches());
	}

	private void match(String glob, String pattern, String... match) {

		Pattern p = Glob.toPattern(glob);
		assertEquals(pattern, p.pattern());

		for (String m : match) {
			if (m.startsWith("~")) {
				assertFalse(p.matcher(m.substring(1))
					.matches(), "Failed to match " + glob + "~" + m.substring(1));

			} else
				assertTrue(p.matcher(m)
					.matches(), "Failed to match " + glob + "~" + m);
		}
		if (!glob.contains("\\Q")) {
			String quoted = "\\Q" + glob + "\\E";
			assertEquals(quoted, Glob.toPattern(quoted)
				.pattern());
		}
	}

}
