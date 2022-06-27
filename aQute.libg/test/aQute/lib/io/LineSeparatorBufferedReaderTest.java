package aQute.lib.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.nio.CharBuffer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LineSeparatorBufferedReaderTest {
	@Test
	void empty() throws Exception {
		String testString = "";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		assertThat(reader.readLine()).isNull();
	}

	@Test
	void no_eol() throws Exception {
		String testString = "foo=bar";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		assertThat(reader.readLine()).isEqualTo("foo=bar");
		assertThat(reader.lineSeparator()).isEqualTo("");
		assertThat(reader.lineSeparator()).isEqualTo("");
		assertThat(reader.readLine()).isNull();
		assertThat(reader.lineSeparator()).isEqualTo("");
	}

	@Test
	void lf() throws Exception {
		String testString = "foo=bar\n";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		assertThat(reader.readLine()).isEqualTo("foo=bar");
		assertThat(reader.lineSeparator()).isEqualTo("\n");
		assertThat(reader.lineSeparator()).isEqualTo("");
		assertThat(reader.readLine()).isNull();
		assertThat(reader.lineSeparator()).isEqualTo("");
	}

	@Test
	void cr() throws Exception {
		String testString = "foo=bar\r";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		assertThat(reader.readLine()).isEqualTo("foo=bar");
		assertThat(reader.lineSeparator()).isEqualTo("\r");
		assertThat(reader.lineSeparator()).isEqualTo("");
		assertThat(reader.readLine()).isNull();
		assertThat(reader.lineSeparator()).isEqualTo("");
	}

	@Test
	void crlf() throws Exception {
		String testString = "foo=bar\r\n";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		assertThat(reader.readLine()).isEqualTo("foo=bar");
		assertThat(reader.lineSeparator()).isEqualTo("\r\n");
		assertThat(reader.lineSeparator()).isEqualTo("");
		assertThat(reader.readLine()).isNull();
		assertThat(reader.lineSeparator()).isEqualTo("");
	}

	@ValueSource(booleans = {true, false})
	@ParameterizedTest
	void mixed_eol(boolean lineSeparator) throws Exception {
		String testString = "\nfoo1=bar\r\nfoo2=bar\nfoo3=bar\rfoo4=bar";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		assertThat(reader.readLine()).isEqualTo("");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\n");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		assertThat(reader.readLine()).isEqualTo("foo1=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\r\n");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		assertThat(reader.readLine()).isEqualTo("foo2=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\n");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		assertThat(reader.readLine()).isEqualTo("foo3=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\r");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		assertThat(reader.readLine()).isEqualTo("foo4=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		assertThat(reader.readLine()).isNull();
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
	}

	@ValueSource(booleans = {true, false})
	@ParameterizedTest
	void skip(boolean lineSeparator) throws Exception {
		String testString = "\nfoo1=bar\r\nfoo2=bar\nfoo3=bar\rfoo4=bar";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		assertThat(reader.readLine()).isEqualTo("");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\n");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		assertThat(reader.readLine()).isEqualTo("foo1=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\r\n");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		String skipString = "foo2=bar\n";
		assertThat(reader.skip(skipString.length())).isEqualTo(skipString.length());
		assertThat(reader.readLine()).isEqualTo("foo3=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\r");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		assertThat(reader.readLine()).isEqualTo("foo4=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		assertThat(reader.readLine()).isNull();
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
	}

	@ValueSource(booleans = {true, false})
	@ParameterizedTest
	void mark(boolean lineSeparator) throws Exception {
		String testString = "\nfoo1=bar\r\nfoo2=bar\nfoo3=bar\rfoo4=bar";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		assertThat(reader.readLine()).isEqualTo("");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\n");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		assertThat(reader.readLine()).isEqualTo("foo1=bar");
		reader.mark(200);
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\r\n");
		}
		reader.reset();
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\r\n");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		reader.mark(200);
		assertThat(reader.readLine()).isEqualTo("foo2=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\n");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		reader.reset();
		assertThat(reader.readLine()).isEqualTo("foo2=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\n");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		assertThat(reader.readLine()).isEqualTo("foo3=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\r");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		reader.mark(200);
		assertThat(reader.readLine()).isEqualTo("foo4=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		reader.reset();
		assertThat(reader.readLine()).isEqualTo("foo4=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("");
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		assertThat(reader.readLine()).isNull();
		assertThat(reader.lineSeparator()).isEqualTo("");
	}

	@ValueSource(booleans = {true, false})
	@ParameterizedTest
	void newlines(boolean lineSeparator) throws Exception {
		String testString = "\n\r\n\n\rfoo=bar";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		assertThat(reader.readLine()).isEqualTo("");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\n");
		}
		assertThat(reader.readLine()).isEqualTo("");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\r\n");
		}
		assertThat(reader.readLine()).isEqualTo("");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\n");
		}
		assertThat(reader.readLine()).isEqualTo("");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\r");
		}
		assertThat(reader.readLine()).isEqualTo("foo=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("");
		}
		assertThat(reader.readLine()).isNull();
	}

	@Test
	void read_single() throws Exception {
		String testString = "\nfoo1=bar\r\n";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		for (char c : testString.toCharArray()) {
			assertThat(reader.read()).isEqualTo(c);
		}
		assertThat(reader.read()).isEqualTo(-1);
	}

	@ValueSource(booleans = {true, false})
	@ParameterizedTest
	void read_single_pushback(boolean lineSeparator) throws Exception {
		String testString = "\nfoo1=bar\rfoo2=bar";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		assertThat(reader.read()).isEqualTo('\n');
		assertThat(reader.readLine()).isEqualTo("foo1=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\r");
		}
		String remainder = "foo2=bar";
		for (char c : remainder.toCharArray()) {
			assertThat(reader.read()).isEqualTo(c);
		}
		assertThat(reader.read()).isEqualTo(-1);
	}

	@Test
	void read_array() throws Exception {
		String testString = "\nfoo1=bar\r\n";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		char[] cbuf = new char[200];
		assertThat(reader.read(cbuf)).isEqualTo(testString.length());
		assertThat(cbuf).startsWith(testString.toCharArray());
		assertThat(reader.read(cbuf)).isEqualTo(-1);
	}

	@ValueSource(booleans = {true, false})
	@ParameterizedTest
	void read_array_pushback(boolean lineSeparator) throws Exception {
		String testString = "\nfoo1=bar\rfoo2=bar";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		assertThat(reader.read()).isEqualTo('\n');
		assertThat(reader.readLine()).isEqualTo("foo1=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\r");
		}
		char[] cbuf = new char[200];
		String remainder = "foo2=bar";
		for (int remaining; (remaining = remainder.length()) > 0;) {
			int length = reader.read(cbuf);
			assertThat(length).isBetween(1, remaining);
			assertThat(cbuf).startsWith(remainder.substring(0, length)
				.toCharArray());
			remainder = remainder.substring(length);
		}
		assertThat(reader.read(cbuf)).isEqualTo(-1);
	}

	@Test
	void read_charbuffer() throws Exception {
		String testString = "\nfoo1=bar\r\n";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		CharBuffer cbuf = CharBuffer.allocate(200);
		assertThat(reader.read(cbuf)).isEqualTo(testString.length());
		cbuf.flip();
		assertThat(cbuf.toString()).isEqualTo(testString);
		cbuf.clear();
		assertThat(reader.read(cbuf)).isEqualTo(-1);
	}

	@ValueSource(booleans = {true, false})
	@ParameterizedTest
	void read_charbuffer_pushback(boolean lineSeparator) throws Exception {
		String testString = "\nfoo1=bar\rfoo2=bar";
		StringReader stringReader = new StringReader(testString);
		LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(stringReader);
		assertThat(reader.read()).isEqualTo('\n');
		assertThat(reader.readLine()).isEqualTo("foo1=bar");
		if (lineSeparator) {
			assertThat(reader.lineSeparator()).isEqualTo("\r");
		}
		CharBuffer cbuf = CharBuffer.allocate(200);
		String remainder = "foo2=bar";
		for (int remaining; (remaining = remainder.length()) > 0;) {
			int length = reader.read(cbuf);
			cbuf.flip();
			assertThat(length).isBetween(1, remaining);
			assertThat(cbuf.toString()).isEqualTo(remainder.substring(0, length));
			remainder = remainder.substring(length);
			cbuf.clear();
		}
		assertThat(reader.read(cbuf)).isEqualTo(-1);
	}
}
