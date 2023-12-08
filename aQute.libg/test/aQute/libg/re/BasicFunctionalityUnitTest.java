package aQute.libg.re;

import static aQute.libg.re.Catalog.caseInsenstive;
import static aQute.libg.re.Catalog.caseInsenstiveOff;
import static aQute.libg.re.Catalog.g;
import static aQute.libg.re.Catalog.lit;
import static aQute.libg.re.Catalog.maybe;
import static aQute.libg.re.Catalog.someAll;
import static aQute.libg.re.Catalog.unicodeCase;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;

import org.junit.Test;


public class BasicFunctionalityUnitTest {
	@Test
	public void testSomething() {
		assertThat(someAll.matches(null)).isNotPresent();
		assertThat(someAll.matches("")).isNotPresent();
		assertThat(someAll.matches("a")).isPresent();
	}

	@Test
	public void testAnything() {
		class X extends Catalog {
			RE test = g(startOfLine, setAll);
		}
		X x = new X();
		assertThat(x.test.matches("what")).isPresent();
		assertThat(x.test.matches("")).isPresent();
		assertThat(x.test.matches(" ")).isPresent();
	}

	@Test
	public void testAnythingBut() {
		class X extends Catalog {
			RE	test	= g(startOfLine, anythingBut("w"));
			RE	test2	= g(startOfLine, anythingBut("ha"));
		}
		X x = new X();
		assertThat(x.test.matches("what")).describedAs(x.test.toString())
			.isNotPresent();
		assertThat(x.test.matches("that")).describedAs(x.test.toString())
			.isPresent();
		assertThat(x.test.matches(" ")).describedAs(x.test.toString())
			.isPresent();
		assertThat(x.test.matches(null)).describedAs(x.test.toString())
			.isNotPresent();
		assertThat(x.test2.matches("what")).describedAs(x.test.toString())
			.isNotPresent();
		assertThat(x.test2.matches("that")).describedAs(x.test.toString())
			.isNotPresent();
		assertThat(x.test.matches(" ")).describedAs(x.test.toString())
			.isPresent();
		assertThat(x.test.matches(null)).describedAs(x.test.toString())
			.isNotPresent();

	}

	@Test
	public void testUrl() {
		class X extends Catalog {
			RE test = g(lit("http"), opt("s"), lit("://"), opt("www\\."), set(ws.not()));
		}
		X x = new X();
		String testUrl = "https://www.google.com";
		assertThat(x.test.matches(testUrl)).isPresent(); // True
	}

	@Test
	public void testMultipleRanges() throws Exception {
		class X extends Catalog {
			RE test = cc("a-zA-Z");
		}
		X x = new X();
		assertThat(x.test.toString()).isEqualTo("[a-zA-Z]");
		assertThat(x.test.matches("c")).isPresent();
		assertThat(x.test.matches("1")).isNotPresent();
	}

	@Test
	public void testEndOfLine() {
		class X extends Catalog {
			RE test = g(lit("a"), endOfLine);
		}
		X x = new X();
		assertThat(x.test.toString()).isEqualTo("(?:a$)");
		assertThat(x.test.find("bba")).isPresent();
		assertThat(x.test.matches("a")).isPresent();
		assertThat(x.test.matches("ab")).isNotPresent();

	}

	@Test
	public void testMaybe() {
		RE test = g(lit("a"), maybe("b"));

		assertThat(test.toString()).isEqualTo("(?:a(?:.*b?))");
		assertThat(test.find("bba")).isPresent();
		assertThat(test.matches("a")).isPresent();
		assertThat(test.matches("ab")).isPresent();
		assertThat(test.matches("acb")).isPresent();
		assertThat(test.matches("abc")).isPresent();
		assertThat(test.matches("cba")).isNotPresent();
	}

	@Test
	public void testAnyOf() {
		class X extends Catalog {
			RE test = g(lit("a"), cc("xyz"));
		}
		X x = new X();
		assertThat(x.test.toString()).isEqualTo("(?:a[xyz])");
		assertThat(x.test.find("ay")).isPresent();
		assertThat(x.test.find("abc")).isNotPresent();
	}

	@Test
	public void testOr() {
		class X extends Catalog {
			RE test = g(startOfLine, or("abc", "def"));
		}
		X x = new X();
		assertThat(x.test.toString()).isEqualTo("(?:^(?:abc|def))");
		assertThat(x.test.find("abcxxx")).isPresent();
		assertThat(x.test.find("def")).isPresent();
		assertThat(x.test.find("defabc")).isPresent();
		assertThat(x.test.find("xdef")).isNotPresent();
	}

	@Test
	public void testLineBreak() {
		class X extends Catalog {
			RE test = g(startOfLine, lit("abc"), nl, lit("def"));
		}
		X x = new X();
		assertThat(x.test.toString()).isEqualTo("(?:^abc\\Rdef)");
		assertThat(x.test.find("abc\ndef")).isPresent();
		assertThat(x.test.find("abc\r\ndef")).isPresent();
		assertThat(x.test.find("abc\rdef")).isPresent();
		assertThat(x.test.find("abc\r\nabc")).isNotPresent();
		assertThat(x.test.find(" abc\ndef")).isNotPresent();
	}

	@Test
	public void testTab() {
		class X extends Catalog {
			RE test = g(startOfLine, tab, lit("abc"));
		}
		X x = new X();
		assertThat(x.test.toString()).isEqualTo("(?:^\tabc)");
		assertThat(x.test.find("\tabc\ndef")).isPresent();
		assertThat(x.test.find("abc\ndef")).isNotPresent();
	}

	@Test
	public void testWord() {
		class X extends Catalog {
			RE test = word;
		}
		X x = new X();
		assertThat(x.test.toString()).isEqualTo("\\w+");
		assertThat(x.test.find("  word word ")).isPresent();
		assertThat(x.test.find(" @$^& ")).isNotPresent();
	}

	@Test
	public void testAtLeast() {
		class X extends Catalog {
			RE	test1	= atLeast(2, lit("abc"));
			RE	test2	= multiple(2, 3, lit("abc"));
		}
		X x = new X();
		assertThat(x.test1.toString()).isEqualTo("(?:abc){2,}");
		assertThat(x.test1.find("  abcabc ")).isPresent();
		assertThat(x.test1.find("  abcabcabc")).isPresent();
		assertThat(x.test1.find(" abc ")).isNotPresent();
		assertThat(x.test2.toString()).isEqualTo("(?:abc){2,3}");
		assertThat(x.test2.matches("abc")).isNotPresent();
		assertThat(x.test2.matches(" abcabc")).isNotPresent();
		assertThat(x.test2.matches("abcabc")).isPresent();
		assertThat(x.test2.matches("abcabcabc")).isPresent();
		assertThat(x.test2.matches("abcabcabcabc")).isNotPresent();
	}

	@Test
	public void testWithAnyCase() {
		class X extends Catalog {
			RE test1 = caseInsenstive(lit("abc"));
		}
		X x = new X();
		assertThat(x.test1.toString()).isEqualTo("(?i:abc)");
		assertThat(x.test1.matches("abc")).isPresent();
		assertThat(x.test1.matches("Abc")).isPresent();
		assertThat(x.test1.matches("abC")).isPresent();
		assertThat(x.test1.matches("def")).isNotPresent();
	}

	@Test
	public void testWithAnyCaseTurnOnThenTurnOff() {
		class X extends Catalog {
			RE test1 = g(lit("abc"), caseInsenstive(lit("def")));
		}
		X x = new X();
		assertThat(x.test1.toString()).isEqualTo("(?:abc(?i:def))");
		assertThat(x.test1.matches("abcdef")).isPresent();
		assertThat(x.test1.matches("abcDeF")).isPresent();
		assertThat(x.test1.matches("abCdef")).isNotPresent();
		assertThat(x.test1.matches("aBcdef")).isNotPresent();
	}

	@Test
	public void testMultipleFlags() {
		RE test1 = g(lit("abc"), caseInsenstive(), unicodeCase(), lit("def"), caseInsenstiveOff());
		assertThat(test1.toString()).isEqualTo("(?:abc(?iu)def(?-i))");
		assertThat(test1.matches("abcdef")).isPresent();
		assertThat(test1.matches("abcDeF")).isPresent();
		assertThat(test1.matches("abCdef")).isNotPresent();
		assertThat(test1.matches("aBcdef")).isNotPresent();
	}

	@Test
	public void testGetText() {
		class X extends Catalog {
			RE test1 = g(lit("http"), opt("s"), lit("://www."), set(ws.not()), dot, lit("com"));
		}
		X x = new X();
		assertThat(x.test1.toString()).isEqualTo("(?:https?://www\\.\\S*\\.com)");
		assertThat(x.test1.find("123 https://www.google.com 456")).isPresent();

	}

	@Test
	public void testStartNamedCapture() {
		class X extends Catalog {
			RE	token	= word;
			RE	domain	= g( set(token, dot), token);
			RE	atSign	= lit("@");
			RE	test1	= g(setAll, atSign, named(domain));
		}
		X x = new X();
		assertThat(x.test1.toString()).isEqualTo("(?:.*@(?<domain>(?:(?:\\w+\\.)*\\w+)))");
		Matcher matcher = x.test1.getMatcher("foobar@example.com");
		assertThat(matcher.matches()).isTrue();
		assertThat(matcher.group("domain")).isEqualTo("example.com");
	}

}
