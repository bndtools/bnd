package aQute.libg.re;

import static aQute.libg.re.Catalog.Alnum;
import static aQute.libg.re.Catalog.ahead;
import static aQute.libg.re.Catalog.atomic;
import static aQute.libg.re.Catalog.back;
import static aQute.libg.re.Catalog.behind;
import static aQute.libg.re.Catalog.capture;
import static aQute.libg.re.Catalog.cc;
import static aQute.libg.re.Catalog.dotall;
import static aQute.libg.re.Catalog.dquote;
import static aQute.libg.re.Catalog.g;
import static aQute.libg.re.Catalog.if_;
import static aQute.libg.re.Catalog.list;
import static aQute.libg.re.Catalog.lit;
import static aQute.libg.re.Catalog.or;
import static aQute.libg.re.Catalog.reluctant;
import static aQute.libg.re.Catalog.seq;
import static aQute.libg.re.Catalog.set;
import static aQute.libg.re.Catalog.setAll;
import static aQute.libg.re.Catalog.some;
import static aQute.libg.re.Catalog.while_;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import aQute.libg.re.RE.C;
import aQute.libg.re.RE.Match;

public class RETest {

	List<String> ids = new ArrayList<>();
	{
		ids.add("数学"); // Japanese for "mathematics"
		ids.add("математика"); // Russian for "mathematics"
		ids.add("رياضيات"); // Arabic for "mathematics"
		ids.add("गणित"); // Hindi for "mathematics"
		ids.add("数学的"); // Simplified Chinese for "mathematical"
		ids.add("matemáticas"); // Spanish for "mathematics"
		ids.add("matematički"); // Croatian for "mathematical"
		ids.add("ਗਣਿਤ"); // Punjabi for "mathematics"
		ids.add("μαθηματικά"); // Greek for "mathematics"
		ids.add("matematika"); // Indonesian for "mathematics"
		ids.add("მათემატიკა"); // Georgian for "mathematics"
		ids.add("մաթեմատիկա"); // Armenian for "mathematics"
		ids.add("คณิตศาสตร์"); // Thai for "mathematics"
		ids.add("πRadian"); // Pi symbol
		ids.add("θAngle"); // Theta symbol
		ids.add("φField"); // Phi symbol
		ids.add("ψWaveFunction"); // Psi symbol
		ids.add("αCoefficient"); // Alpha symbol
		ids.add("βBetaCoefficient"); // Beta symbol
		ids.add("γGammaValue"); // Gamma symbol
		ids.add("δDeltaChange"); // Delta symbol
		ids.add("εEpsilonValue"); // Epsilon symbol
		ids.add("λWavelength"); // Lambda symbol
		ids.add("σStandardDeviation"); // Sigma symbol
		ids.add("ωAngularFrequency"); // Omega symbol
	}

	@Test
	public void test() {
		@SuppressWarnings("unused")
		class X extends Catalog {
			RE	id			= g(javaJavaIdentifierStart, set(javaJavaIdentifierPart));
			RE	fqn			= g(id, set(g(dot, id)));
			RE	eq			= lit("=");
			RE	value		= cc("()=\n\r").not();
			RE	clause		= term(id, eq, value);
			RE	clauses		= list(clause);
			RE	import_		= lit("import");
			RE	comment		= lit("#");
			RE	annStart	= lit("@");

			RE	imprt		= term(comment, import_, ws, fqn, opt(semicolon), eol);
			RE	annotation	= term(comment, annStart, fqn, parOpen, named(clauses), parClose, eol);

			RE	named		= named(eq);
		}
		X x = new X();

		assertThat(x.named.toString()).isEqualTo("(?<eq>=)");
		assertThat(x.value.toString()).isEqualTo("[^()=\n\r]");
		assertThat(x.id.toString()).isEqualTo("(?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)");
		assertThat(x.fqn.toString()).isEqualTo(
			"(?:(?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)(?:\\.(?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*))*)");
		for (String id : ids) {
			assertThat(x.id.matches(id)).as(id)
				.isPresent();
			assertThat(x.fqn.matches(id)).as(id)
				.isPresent();
			String fqn = id + "." + id + "." + id;
			assertThat(x.fqn.matches(fqn)).as(fqn)
				.isPresent();
		}
	}

	@Test
	public void testStrings() {
		class X extends Catalog {
			RE	innerDquoted	= or(or(dquote, backslash).not(), g(backslash, all));
			RE	dqstring		= g(dquote, g("content", set(innerDquoted)), dquote);
		}
		X x = new X();
		assertThat(x.innerDquoted.toString()).isEqualTo("(?:[^\"\\\\]|(?:\\\\.))");
		assertThat(x.dqstring.toString()).isEqualTo("(?:\"(?<content>(?:[^\"\\\\]|(?:\\\\.))*)\")");
		String s = """
			prefix "Hello \\" World \\ " suffix
			""";
		assertThat(x.dqstring.find(s)).isPresent();
		Match match = x.dqstring.find(s)
			.get();
		assertThat(match.group("content")
			.map(g -> g.toString())).isNotNull();
		assertThat(match.group("content")).isPresent();
	}

	@Test
	public void testAppend() {
		class X extends Catalog {
			RE	match		= lit("abc");
			RE	namedMatch	= named(match);
		}
		X x = new X();
		String append = x.namedMatch.append("def abc ghi abc jkl", (Match m) -> m.group("match")
			.map(mg -> mg.toString()
				.toUpperCase())
			.orElse(""));
		assertThat(append).isEqualTo("def ABC ghi ABC jkl");
	}

	@Test
	public void testStream() {
		class X extends Catalog {
			RE	match		= lit("abc");
			RE	namedMatch	= named(match);
		}
		X x = new X();
		assertThat(x.namedMatch.findAll("def abc ghi abc jkl")
			.count()).isEqualTo(2);
	}

	@Test
	public void testIf() {
		RE cond = if_(dquote, seq(setAll, dquote), lit("nostring"));
		assertThat(cond.toString()).isEqualTo("(?:(?=\").*\"|nostring)");
		assertThat(cond.find("\"hello world\"")).isPresent();
		assertThat(cond.find("nostring")).isPresent();
		assertThat(cond.matches("x")).isNotPresent();

		RE cond2 = dotall(if_(seq(setAll, lit("foo")), lit("bar")), setAll);
		assertThat(cond2.toString()).isEqualTo("(?s:(?=.*foo)(?:bar).*)");
		assertThat(cond2.matches("bar some foo")).isPresent();
		assertThat(cond2.find("bar \n foo")).isPresent();
		assertThat(cond2.find("bar \n fxoo")).isNotPresent();
	}

	@Test
	public void testWhile() {
		RE cond = while_(seq(setAll, lit("foo")), lit("f"));
		assertThat(cond.toString()).isEqualTo("(?=.*foo)f*");
		assertThat(cond.find("fffffffoo")).isPresent();
	}

	@Test
	public void testAtomic() {
		RE token = g(lit("a"), atomic(or(lit("bc", "b"))), lit("c"));
		assertThat(token.toString()).isEqualTo("(?:a(?>(?:bc|b))c)");
		assertThat(token.matches("abcc")).isPresent();
		assertThat(token.matches("abc")).isNotPresent();

	}

	@Test
	public void testLiteral() {
		assertThat(lit("abc ^$()[]|+*?{} def").toString())
			.isEqualTo("abc\\ \\^\\$\\(\\)\\[\\]\\|\\+\\*\\?\\{\\}\\ def");

	}

	@Test
	public void testAheadBehind() {
		RE ahead = g(lit("q"), ahead(lit("u")));

		assertThat(ahead.toString()).isEqualTo("(?:q(?=u))");
		assertThat(ahead.find("qu")).isPresent();
		assertThat(ahead.find("u")).isNotPresent();
		assertThat(ahead.find("q")).isNotPresent();

		RE behind = g(behind(lit("q")), lit("u"));
		assertThat(behind.toString()).isEqualTo("(?:(?<=q)u)");
		assertThat(behind.find("qu")).isPresent();
		assertThat(behind.find("q")).isNotPresent();
		assertThat(behind.find("u")).isNotPresent();

		RE notAhead = g(lit("q"), ahead(lit("u")).not());
		assertThat(notAhead.toString()).isEqualTo("(?:q(?!u))");
		assertThat(notAhead.find("qu")).isNotPresent();
		assertThat(notAhead.find("qx")).isPresent();
		assertThat(notAhead.find("u")).isNotPresent();
		assertThat(notAhead.find("q")).isPresent();

		RE notBehind = g(behind(lit("q")).not(), lit("u"));

		assertThat(notBehind.toString()).isEqualTo("(?:(?<!q)u)");
		assertThat(notBehind.find("qu")).isNotPresent();
		assertThat(notBehind.find("u")).isPresent();
		assertThat(notBehind.find("xu")).isPresent();
		assertThat(notBehind.find("u")).isPresent();
	}

	@Test
	public void testGreedy() {
		RE greedy = seq(lit("<"), setAll, lit(">"));
		RE reluctant = seq(lit("<"), reluctant(setAll), lit(">"));
		RE reluctant2 = seq(lit("<"), setAll.reluctant(), lit(">"));
		RE possesive = seq(dquote, set(dquote.not())
			.possesive(), dquote);

		assertThat(greedy.toString()).isEqualTo("<.*>");
		assertThat(greedy.find("This is a <EM>first</EM> test")
			.get()
			.toString()).isEqualTo("<EM>first</EM>");

		assertThat(reluctant.toString()).isEqualTo("<.*?>");
		assertThat(reluctant.find("This is a <EM>first</EM> test")
			.get()
			.toString()).isEqualTo("<EM>");

		assertThat(possesive.toString()).isEqualTo("\"(?:[^\"])*+\"");
		assertThat(possesive.find("abc \"hello world\" def")
			.get()
			.toString()).isEqualTo("\"hello world\"");
	}

	@Test
	public void testList() {
		RE l = list(some(lit("a")), lit(","));
		assertThat(l.toString()).isEqualTo("(?:\\s*a+\\s*(?:\\s*,\\s*a+)*)");
		assertThat(l.matches("a")).isPresent();
		assertThat(l.matches("a,")).isNotPresent();
		assertThat(l.matches("a,a")).isPresent();
		assertThat(l.matches("aaaaa,aaaaa")).isPresent();
		assertThat(l.matches("aaaaa , aaaaa")).isPresent();
		assertThat(l.matches("a ")).isPresent();
	}

	@Test
	public void testParameters() {
		class X extends Catalog {
			RE	innerDquoted	= or(or(dquote, backslash).not(), g(backslash, all));
			RE	dqstring		= g(dquote, set(innerDquoted), dquote);
			RE	id				= g(javaJavaIdentifierStart, set(javaJavaIdentifierPart));
			RE	eq				= lit("=");
			RE	value			= or(set(cc(",;").not()), dqstring);
			RE	property		= term(id, opt(eq, value));
			RE	clause			= term(id, set(term(semicolon, property)));
			RE	parameters		= list(clause);
		}
		X x = new X();

		assertThat(x.dqstring.matches("\"3\"")).isPresent();
		assertThat(x.clause.matches("foo")).describedAs(x.clause.toString())
			.isPresent();
		assertThat(x.clause.matches("foo;a")).describedAs(x.clause.toString())
			.isPresent();
		assertThat(x.value.matches("xxxxx")).describedAs(x.value.toString())
			.isPresent();
		assertThat(x.value.matches("\"1;2,3\"")).describedAs(x.value.toString())
			.isPresent();
		assertThat(x.property.matches("x=\"1;2,3\"")).describedAs(x.value.toString())
			.isPresent();
		assertThat(x.clause.matches("foo; a = 6 9;b=\"1;2,3\"")).describedAs(x.clause.toString())
			.isPresent();

		assertThat(x.parameters.toString()).isEqualTo(
			"(?:\\s*(?:\\s*(?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\s*(?:\\s*;\\s*(?:\\s*(?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\s*(?:=(?:(?:[^,;])*|(?:\"(?:[^\"\\\\]|(?:\\\\.))*\")))?))*)\\s*(?:\\s*,\\s*(?:\\s*(?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\s*(?:\\s*;\\s*(?:\\s*(?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\s*(?:=(?:(?:[^,;])*|(?:\"(?:[^\"\\\\]|(?:\\\\.))*\")))?))*))*)");
		assertThat(x.parameters.matches("foo")).isPresent();
		assertThat(x.parameters.matches("foo, bar, yuck")).isPresent();
		assertThat(x.parameters.matches("foo;a=1, bar, yuck")).isPresent();
		assertThat(x.parameters.matches("foo;a=1, bar;b=\"2,;\\\"\", yuck")).describedAs(x.parameters.toString())
			.isPresent();
	}

	@Test
	public void testCharacterClasses() {
		class X extends Catalog {
			C	abc					= cc("abc");
			C	cde					= cc("cde");
			C	cdf					= cc("cdf");
			C	abcANDcde			= abc.and(cde);
			C	abcANDcdeANDcdf		= abc.and(cde.and(cdf));
			C	abcANDcdeANDcdf2	= abc.and(cde)
				.and(cdf);

			C	abcNOTb				= abc.and(cc("b").not());

			C	alpha				= Alnum.and(digit.not());
		}
		X x = new X();

		assertThat(set(cc("a")).toString()).isEqualTo("a*");

		assertThat(x.abcNOTb.toString()).isEqualTo("[abc&&[^b]]");

		C and = Alnum.and(Catalog.digit.not());

		assertThat(x.alpha.toString()).isEqualTo("[\\p{Alnum}&&\\D]");
		assertThat(Alnum.matches("c")).isPresent();
		assertThat(Alnum.matches("9")).isPresent();
		assertThat(x.alpha.matches("c")).isPresent();
		assertThat(x.alpha.matches("9")).isNotPresent();

		assertThat(x.abcNOTb.matches("c")).isPresent();
		assertThat(x.abcNOTb.matches("b")).isNotPresent();

		assertThat(x.abc.toString()).isEqualTo("[abc]");
		assertThat(x.cde.toString()).isEqualTo("[cde]");
		assertThat(x.abcANDcde.toString()).isEqualTo("[abc&&[cde]]");
		assertThat(x.abcANDcdeANDcdf2.toString()).isEqualTo("[abc&&[cde]&&[cdf]]");
		assertThat(x.abcANDcdeANDcdf2.matches("c")).isPresent();
		assertThat(x.abcANDcdeANDcdf2.matches("a")).isNotPresent();
		assertThat(x.abcANDcdeANDcdf2.matches("x")).isNotPresent();
		assertThat(x.abcANDcdeANDcdf.toString()).isEqualTo("[abc&&[cde&&[cdf]]]");
		assertThat(x.abcANDcdeANDcdf.matches("c")).isPresent();
		assertThat(x.abcANDcdeANDcdf.matches("a")).isNotPresent();
		assertThat(x.abcANDcdeANDcdf.matches("x")).isNotPresent();
		assertThat(x.abc.matches("a")).isPresent();

	}

	@Test
	public void testBack() {
		RE test = seq(lit("<"), capture(Catalog.word), setAll.reluctant(), lit("</"), back(1), lit(">"));
		assertThat(test.toString()).isEqualTo("<(\\w+).*?</\\1>");
		assertThat(test.matches("<abc>blabka</abc>")).isPresent();
		assertThat(test.matches("<abc>blabka</def>")).isNotPresent();
	}

	@Test
	public void testNamed() {
		RE test = g("foo", lit("a"));
		assertThat(test.toString()).isEqualTo("(?<foo>a)");
		assertThat(test.getGroupNames()).containsExactly("foo");

		RE test2 = g(test, g("bar", lit("b")));
		assertThat(test2.toString()).isEqualTo("(?:(?<foo>a)(?<bar>b))");
		assertThat(test2.getGroupNames()).containsExactlyInAnyOrder("foo", "bar");

		RE test3 = g(test, g("bar", g("xyz", lit("b"))));
		assertThat(test3.toString()).isEqualTo("(?:(?<foo>a)(?<bar>(?<xyz>b)))");
		assertThat(test3.getGroupNames()).containsExactlyInAnyOrder("foo", "bar", "xyz");

	}

	@Test
	void testSpecial() {
		C s = Catalog.ws;
		C w = Catalog.letter;
		assertThat(s.toString()).isEqualTo("\\s");
		assertThat(s.not()
			.toString()).isEqualTo("\\S");
		assertThat(s.not()
			.not()
			.toString()).isEqualTo("\\s");
		assertThat(w.toString()).isEqualTo("\\w");
		assertThat(w.not()
			.toString()).isEqualTo("\\W");

	}

	@Test
	void testCC() {
		C a = cc("abc");
		C d = cc("def");
		C g = cc("ghi");

		assertThat(a.toString()).isEqualTo("[abc]");
		assertThat(a.not()
			.toString()).isEqualTo("[^abc]");
		assertThat(a.not()
			.not()
			.toString()).isEqualTo("[abc]");

		assertThat(a.and(d)
			.toString()).isEqualTo("[abc&&[def]]");
		assertThat(a.or(d)
			.toString()).isEqualTo("[abcdef]");
		assertThat(a.and(d)
			.or(g)
			.toString()).isEqualTo("[abcghi&&[def]]");
	}

	@Test
	void testMatching() {
		C a = cc("abc");
		C d = cc("def");
		C g = cc("ghi");
		RE as = some(a);
		assertThat(as.matches("")).isNotPresent();
		assertThat(as.matches("aaaaa")).isPresent();
		assertThat(as.matches("aabacaa")).isPresent();
		assertThat(as.matches("aabacaax")).isNotPresent();
		assertThat(as.find("aabacaax")).isPresent();
		assertThat(as.lookingAt("aabacaax")).isPresent();
	}
}
