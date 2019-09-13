package aQute.lib.stringrover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.Test;

public class StringRoverTest {

	@Test
	public void testLength() {
		String test = "abcdefghijklmnopqrstuvwxyz";
		StringRover r = new StringRover(test);
		assertThat(r).hasSize(test.length());
		assertThat(r.increment()).hasSize((test = test.substring(1)).length());
		assertThat(r.increment(5)).hasSize((test = test.substring(5)).length());
		assertThat(r.isEmpty()).isFalse();
		assertThat(r.increment(19)).hasSize((test = test.substring(19)).length());
		assertThat(r.isEmpty()).isFalse();
		assertThat(r.increment()).hasSize((test = test.substring(1)).length());
		assertThat(r.isEmpty()).isTrue();
		assertThat(r.increment()
			.isEmpty()).isTrue();
	}

	@Test
	public void testDuplicate() {
		String test = "abcdefghijklmnopqrstuvwxyz";
		int len = test.length();
		StringRover r = new StringRover(test);
		assertThat(r).hasSize(len);
		r.increment(6);
		test = test.substring(6);
		assertThat(r).hasSize(test.length());
		StringRover d = r.duplicate();
		assertThat(d).hasSize(test.length());
		assertThat(r.reset()).hasSize(len);
		assertThat(d).hasSize(test.length());
	}

	@Test
	public void testReset() {
		String test = "abcdefghijklmnopqrstuvwxyz";
		StringRover r = new StringRover(test);
		assertThat(r).hasSize(test.length());
		assertThat(r.increment(20)).hasSize(test.substring(20)
			.length());
		assertThat(r.reset()).hasSize(test.length());
	}

	@Test
	public void testCharAt() {
		String test = "abcdefghijklmnopqrstuvwxyz";
		StringRover r = new StringRover(test);
		assertThat(r.charAt(0)).isEqualTo(test.charAt(0));
		assertThat(r.charAt(25)).isEqualTo(test.charAt(25));
		r.increment();
		test = test.substring(1);
		assertThat(r.charAt(0)).isEqualTo(test.charAt(0));
		r.increment();
		test = test.substring(1);
		assertThat(r.charAt(0)).isEqualTo(test.charAt(0));
		assertThat(r.charAt(1)).isEqualTo(test.charAt(1));
		assertThat(r.charAt(2)).isEqualTo(test.charAt(2));
	}

	@Test
	public void testIndexOfChar() {
		String test = "abcdefghijklmnopqrstuvwxyz";
		StringRover r = new StringRover(test);
		assertThat(r.indexOf('@')).isEqualTo(test.indexOf('@'));
		assertThat(r.indexOf('c')).isEqualTo(test.indexOf('c'));
		assertThat(r.indexOf('c', 0)).isEqualTo(test.indexOf('c', 0));
		assertThat(r.indexOf('c', 10)).isEqualTo(test.indexOf('c', 10));
		r.increment(2);
		test = test.substring(2);
		assertThat(r.indexOf('c')).isEqualTo(test.indexOf('c'));
		assertThat(r.indexOf('c', 0)).isEqualTo(test.indexOf('c', 0));
		r.increment();
		test = test.substring(1);
		assertThat(r.indexOf('c')).isEqualTo(test.indexOf('c'));
		assertThat(r.indexOf('c', 0)).isEqualTo(test.indexOf('c', 0));
	}

	@Test
	public void testIndexOfString() {
		String test = "abcdefghijklmnopqrstuvwxyz";
		StringRover r = new StringRover(test);
		assertThat(r.indexOf("")).isEqualTo(test.indexOf(""));
		assertThat(r.indexOf("cdf")).isEqualTo(test.indexOf("cdf"));
		assertThat(r.indexOf("a")).isEqualTo(test.indexOf("a"));
		assertThat(r.indexOf("cde")).isEqualTo(test.indexOf("cde"));
		assertThat(r.indexOf("cde", 0)).isEqualTo(test.indexOf("cde", 0));
		assertThat(r.indexOf("cde", 10)).isEqualTo(test.indexOf("cde", 10));
		r.increment(2);
		test = test.substring(2);
		assertThat(r.indexOf("")).isEqualTo(test.indexOf(""));
		assertThat(r.indexOf("c")).isEqualTo(test.indexOf("c"));
		assertThat(r.indexOf("cde")).isEqualTo(test.indexOf("cde"));
		assertThat(r.indexOf("cde", 0)).isEqualTo(test.indexOf("cde", 0));
		r.increment();
		test = test.substring(1);
		assertThat(r.indexOf("cde")).isEqualTo(test.indexOf("cde"));
		assertThat(r.indexOf("cde", 0)).isEqualTo(test.indexOf("cde", 0));
		test = "";
		r = new StringRover(test);
		assertThat(r.indexOf("")).isEqualTo(test.indexOf(""));
		assertThat(r.indexOf("cde")).isEqualTo(test.indexOf("cde"));
		test = "aaaaa";
		r = new StringRover(test);
		assertThat(r.indexOf("aaa")).isEqualTo(test.indexOf("aaa"));
		assertThat(r.indexOf("aaa", 2)).isEqualTo(test.indexOf("aaa", 2));
		assertThat(r.indexOf("aaa", 3)).isEqualTo(test.indexOf("aaa", 3));
	}

	@Test
	public void testLastIndexOfChar() {
		String test = "abcdefghijklmabcdefghijklm";
		StringRover r = new StringRover(test);
		assertThat(r.lastIndexOf('@')).isEqualTo(test.lastIndexOf('@'));
		assertThat(r.lastIndexOf('m')).isEqualTo(test.lastIndexOf('m'));
		assertThat(r.lastIndexOf('c')).isEqualTo(test.lastIndexOf('c'));
		assertThat(r.lastIndexOf('c', 25)).isEqualTo(test.lastIndexOf('c', 25));
		assertThat(r.lastIndexOf('c', 10)).isEqualTo(test.lastIndexOf('c', 10));
		assertThat(r.lastIndexOf('m', 25)).isEqualTo(test.lastIndexOf('m', 25));
		assertThat(r.lastIndexOf('m', 10)).isEqualTo(test.lastIndexOf('m', 10));
		r.increment(10);
		test = test.substring(10);
		assertThat(r.lastIndexOf('m')).isEqualTo(test.lastIndexOf('m'));
		assertThat(r.lastIndexOf('c')).isEqualTo(test.lastIndexOf('c'));
		assertThat(r.lastIndexOf('c', 15)).isEqualTo(test.lastIndexOf('c', 15));
		assertThat(r.lastIndexOf('m', 15)).isEqualTo(test.lastIndexOf('m', 15));
		r.increment();
		test = test.substring(1);
		assertThat(r.lastIndexOf('f', 6)).isEqualTo(test.lastIndexOf('f', 6));
		r.increment(15);
		test = test.substring(15);
		assertThat(r.lastIndexOf('m')).isEqualTo(test.lastIndexOf('m'));
	}

	@Test
	public void testLastIndexOfString() {
		String test = "abcdefghijklmabcdefghijklm";
		StringRover r = new StringRover(test);
		assertThat(r.lastIndexOf("")).isEqualTo(test.lastIndexOf(""));
		assertThat(r.lastIndexOf("bbc")).isEqualTo(test.lastIndexOf("bbc"));
		assertThat(r.lastIndexOf("m")).isEqualTo(test.lastIndexOf("m"));
		assertThat(r.lastIndexOf("klm")).isEqualTo(test.lastIndexOf("klm"));
		assertThat(r.lastIndexOf("cde")).isEqualTo(test.lastIndexOf("cde"));
		assertThat(r.lastIndexOf("cde", 25)).isEqualTo(test.lastIndexOf("cde", 25));
		assertThat(r.lastIndexOf("abc", 10)).isEqualTo(test.lastIndexOf("abc", 10));
		assertThat(r.lastIndexOf("cde", 10)).isEqualTo(test.lastIndexOf("cde", 10));
		assertThat(r.lastIndexOf("klm", 25)).isEqualTo(test.lastIndexOf("klm", 25));
		assertThat(r.lastIndexOf("klm", 10)).isEqualTo(test.lastIndexOf("klm", 10));
		r.increment(10);
		test = test.substring(10);
		assertThat(r.lastIndexOf("bbc")).isEqualTo(test.lastIndexOf("bbc"));
		assertThat(r.lastIndexOf("")).isEqualTo(test.lastIndexOf(""));
		assertThat(r.lastIndexOf("m")).isEqualTo(test.lastIndexOf("m"));
		assertThat(r.lastIndexOf("klm")).isEqualTo(test.lastIndexOf("klm"));
		assertThat(r.lastIndexOf("cde")).isEqualTo(test.lastIndexOf("cde"));
		assertThat(r.lastIndexOf("cde", 15)).isEqualTo(test.lastIndexOf("cde", 15));
		assertThat(r.lastIndexOf("klm", 15)).isEqualTo(test.lastIndexOf("klm", 15));
		r.increment();
		test = test.substring(1);
		assertThat(r.lastIndexOf("fgh", 6)).isEqualTo(test.lastIndexOf("fgh", 6));
		assertThat(r.lastIndexOf("lmabc")).isEqualTo(test.lastIndexOf("lmabc"));
		r.increment(15);
		test = test.substring(15);
		assertThat(r.lastIndexOf("klm")).isEqualTo(test.lastIndexOf("klm"));
		test = "";
		r = new StringRover(test);
		assertThat(r.lastIndexOf("")).isEqualTo(test.lastIndexOf(""));
		assertThat(r.lastIndexOf("bbc")).isEqualTo(test.lastIndexOf("bbc"));
		test = "aaaaa";
		r = new StringRover(test);
		assertThat(r.lastIndexOf("aaa")).isEqualTo(test.lastIndexOf("aaa"));
		assertThat(r.lastIndexOf("aaa", 0)).isEqualTo(test.lastIndexOf("aaa", 0));
		assertThat(r.lastIndexOf("aaa", 2)).isEqualTo(test.lastIndexOf("aaa", 2));
		assertThat(r.lastIndexOf("aaa", 3)).isEqualTo(test.lastIndexOf("aaa", 3));
	}

	@Test
	public void testSubString() {
		String test = "abcdefghijklmnopqrstuvwxyz";
		StringRover r = new StringRover(test);
		assertThat(r.substring(0, 0)).isEqualTo(test.substring(0, 0))
			.isEmpty();
		assertThat(r.substring(0, 3)).isEqualTo(test.substring(0, 3));
		assertThat(r.substring(3, 6)).isEqualTo(test.substring(3, 6));
		r.increment(3);
		test = test.substring(3);
		assertThat(r.substring(0, 3)).isEqualTo(test.substring(0, 3));
		assertThat(r.substring(0)).isEqualTo(test.substring(0));
		assertThat(r.substring(3)).isEqualTo(test.substring(3));
		r.increment(3);
		test = test.substring(3);
		assertThat(r.substring(0)).isEqualTo(test.substring(0));
	}

	@Test
	public void testToString() {
		String test = "abcdefghijklmnopqrstuvwxyz";
		StringRover r = new StringRover(test);
		assertThat(r).hasToString(test);
		r.increment(20);
		test = test.substring(20);
		assertThat(r).hasToString(test);
		r.increment(6);
		test = test.substring(6);
		assertThat(r).hasToString(test);
	}

	@Test
	public void testConstructor() {
		assertThatNullPointerException().isThrownBy(() -> new StringRover(null));
	}

	@Test
	public void testStartsWith() {
		String test = "abcdefghijklmnopqrstuvwxyz";
		StringRover r = new StringRover(test);
		assertThat(r.startsWith("")).isEqualTo(test.startsWith(""));
		assertThat(r.startsWith("def")).isEqualTo(test.startsWith("def"));
		assertThat(r.startsWith("abc")).isEqualTo(test.startsWith("abc"));
		assertThat(r.startsWith("def", 3)).isEqualTo(test.startsWith("def", 3));
		r.increment(3);
		test = test.substring(3);
		assertThat(r.startsWith("def")).isEqualTo(test.startsWith("def"));
		assertThat(r.startsWith("abc", -3)).isEqualTo(test.startsWith("abc", -3));
		r.increment(21);
		test = test.substring(21);
		assertThat(r.startsWith("")).isEqualTo(test.startsWith(""));
		assertThat(r.startsWith("yz")).isEqualTo(test.startsWith("yz"));
		assertThat(r.startsWith("yza")).isEqualTo(test.startsWith("yza"));
	}

	@Test
	public void testEndsWith() {
		String test = "abcdefghijklmnopqrstuvwxyz";
		StringRover r = new StringRover(test);
		assertThat(r.endsWith("")).isEqualTo(test.endsWith(""));
		assertThat(r.endsWith("xyz")).isEqualTo(test.endsWith("xyz"));
		assertThat(r.endsWith("wxyz")).isEqualTo(test.endsWith("wxyz"));
		assertThat(r.endsWith("abc")).isEqualTo(test.endsWith("abc"));
		r.increment(23);
		test = test.substring(23);
		assertThat(r.endsWith("")).isEqualTo(test.endsWith(""));
		assertThat(r.endsWith("xyz")).isEqualTo(test.endsWith("xyz"));
		assertThat(r.endsWith("wxyz")).isEqualTo(test.endsWith("wxyz"));
	}
}
