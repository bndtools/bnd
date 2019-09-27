package aQute.libg.qtokens;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import junit.framework.TestCase;

public class TestQuotedTokenizer extends TestCase {

	public void testNativeSeps() {
		String s[] = new QuotedTokenizer("x;c;d=4", ";,=", true).getTokens();
		assertEquals("Length", 7, s.length);
		assertEquals("x", s[0]);
		assertEquals(";", s[1]);
		assertEquals("c", s[2]);
		assertEquals(";", s[3]);
		assertEquals("d", s[4]);
		assertEquals("=", s[5]);
		assertEquals("4", s[6]);
	}

	public void testSimple() {
		String s[] = new QuotedTokenizer("1.jar, 2.jar,    \t   3.jar", ",").getTokens();
		assertEquals("Length", 3, s.length);
		assertEquals("1.jar", s[0]);
		assertEquals("2.jar", s[1]);
		assertEquals("3.jar", s[2]);
	}

	public void testQuoted() {
		String s[] = new QuotedTokenizer("'1 ,\t.jar'", ",").getTokens();
		assertEquals("Length", 1, s.length);
		assertEquals("1 ,\t.jar", s[0]);
	}

	public void testWhiteSpace() {
		String s[] = new QuotedTokenizer("               1.jar,               2.jar         ", ",").getTokens();
		assertEquals("Length", 2, s.length);
		assertEquals("1.jar", s[0]);
		assertEquals("2.jar", s[1]);
	}

	public void testMultipleSeps() {
		String s[] = new QuotedTokenizer("1.jar,,,,,,,,,,,    , ,2.jar", ",").getTokens();
		assertEquals("Length", 14, s.length);
		assertEquals("1.jar", s[0]);
		assertEquals("2.jar", s[13]);
	}

	public void testIteration() {
		Iterable<String> iterable = new QuotedTokenizer("1.jar,,,,,,,,,,,    , ,2.jar", ",");
		assertThat(iterable).hasSize(14)
			.containsSequence("1.jar", "", "", "", "", "", "", "", "", "", "", "", "", "2.jar");
		iterable = new QuotedTokenizer(
			"org.apache.servicemix.bundles.junit;version=\"[4.11,5)\",org.assertj.core;version=\"[3,4)\"", ",");
		assertThat(iterable).hasSize(2)
			.containsSequence("org.apache.servicemix.bundles.junit;version=\"[4.11,5)\"",
				"org.assertj.core;version=\"[3,4)\"");
	}

	public void testStream() {
		Stream<String> stream = new QuotedTokenizer("1.jar,,,,,,,,,,,    , ,2.jar", ",").stream();
		assertThat(stream).hasSize(14)
			.containsSequence("1.jar", "", "", "", "", "", "", "", "", "", "", "", "", "2.jar");
		stream = new QuotedTokenizer(
			"org.apache.servicemix.bundles.junit;version=\"[4.11,5)\",org.assertj.core;version=\"[3,4)\"", ",")
				.stream();
		assertThat(stream).hasSize(2)
			.containsSequence("org.apache.servicemix.bundles.junit;version=\"[4.11,5)\"",
				"org.assertj.core;version=\"[3,4)\"");
	}

	public void testNative() {
		String s[] = new QuotedTokenizer("x.dll;y.dll;abc=3;def=5;version=\"1.2.34,123\"", ";,=").getTokens();
		assertEquals("Length", 8, s.length);
		assertEquals("x.dll", s[0]);
		assertEquals("y.dll", s[1]);
		assertEquals("abc", s[2]);
		assertEquals("3", s[3]);
		assertEquals("def", s[4]);
		assertEquals("5", s[5]);
		assertEquals("version", s[6]);
		assertEquals("1.2.34,123", s[7]);
	}

	public void testEscapedQuote() {
		QuotedTokenizer qt = new QuotedTokenizer("'\\'y'", ",");
		String s = qt.nextToken();
		assertEquals("'y", s);
	}

	public void testRetainQuotes() {
		QuotedTokenizer qt = new QuotedTokenizer(" \" foo \", 'bar' ", ",", false, true);
		assertThat(qt.nextToken()).isEqualTo("\" foo \"");
		assertThat(qt.nextToken()).isEqualTo("'bar'");
		qt = new QuotedTokenizer(" \" foo \", 'bar' ", ",", false, false);
		assertThat(qt.nextToken()).isEqualTo(" foo ");
		assertThat(qt.nextToken()).isEqualTo("bar");
		qt = new QuotedTokenizer("someone;quote=\"He said, \\\"What!?\\\"\"", ";=,", false, true);
		assertThat(qt.stream()).hasSize(3)
			.containsSequence("someone", "quote", "\"He said, \\\"What!?\\\"\"");
		qt = new QuotedTokenizer("someone;quote=\"He said, \\\"What!?\\\"\"", ";=,", false, false);
		assertThat(qt.stream()).hasSize(3)
			.containsSequence("someone", "quote", "He said, \"What!?\"");
	}

	public void testLongEscapedQuote() {
		QuotedTokenizer qt = new QuotedTokenizer(
			"\"{\\\":configurator:resource-version\\\": 1, \\\":configurator:version\\\":\\\"1\\\", \\\":configurator:symbolic-name\\\":\\\"dummy\\\", \\\"org.apache.aries.rsa.discovery.zookeeper\\\" : {}, \\\"org.apache.aries.rsa.discovery.zookeeper.server\\\" : {}}\",'{\":configurator:resource-version\": 1, \":configurator:version\":\"1\", \":configurator:symbolic-name\":\"dummy\", \"org.apache.aries.rsa.discovery.zookeeper\" : {}, \"org.apache.aries.rsa.discovery.zookeeper.server\" : {}}'",
			",");
		String s[] = qt.getTokens();
		assertEquals("Length", 2, s.length);
		assertEquals(
			"{\":configurator:resource-version\": 1, \":configurator:version\":\"1\", \":configurator:symbolic-name\":\"dummy\", \"org.apache.aries.rsa.discovery.zookeeper\" : {}, \"org.apache.aries.rsa.discovery.zookeeper.server\" : {}}",
			s[0]);
		assertEquals(s[0], s[1]);
	}

	public void testExplicitEmptyString() {
		QuotedTokenizer qt = new QuotedTokenizer("literal=''", ";=,");
		qt.nextToken();
		assertEquals("", qt.nextToken());
	}

	public void testImplicitEmptyStringTurnedToNull() {
		QuotedTokenizer qt = new QuotedTokenizer("literal=", ";=,");
		qt.nextToken();
		assertNull(qt.nextToken());
	}
}
