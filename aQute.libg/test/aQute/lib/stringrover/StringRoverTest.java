package aQute.lib.stringrover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.Test;

public class StringRoverTest {

	@Test
	public void testLength() {
		StringRover r = new StringRover("abcdefghijklmnopqrstuvwxyz");
		assertThat(r).hasSize(26);
		assertThat(r.increment()).hasSize(25);
		assertThat(r.increment(5)).hasSize(20);
		assertThat(r.isEmpty()).isFalse();
		assertThat(r.increment(19)).hasSize(1);
		assertThat(r.isEmpty()).isFalse();
		assertThat(r.increment()).hasSize(0);
		assertThat(r.isEmpty()).isTrue();
		assertThat(r.increment()
			.isEmpty()).isTrue();
	}

	@Test
	public void testDuplicate() {
		StringRover r = new StringRover("abcdefghijklmnopqrstuvwxyz");
		assertThat(r).hasSize(26);
		assertThat(r.increment(6)).hasSize(20);
		StringRover d = r.duplicate();
		assertThat(d).hasSize(20);
		assertThat(r.reset()).hasSize(26);
		assertThat(d).hasSize(20);
	}

	@Test
	public void testReset() {
		StringRover r = new StringRover("abcdefghijklmnopqrstuvwxyz");
		assertThat(r).hasSize(26);
		assertThat(r.increment(20)).hasSize(6);
		assertThat(r.reset()).hasSize(26);
	}

	@Test
	public void testCharAt() {
		StringRover r = new StringRover("abcdefghijklmnopqrstuvwxyz");
		assertThat(r.charAt(0)).isEqualTo('a');
		assertThat(r.charAt(25)).isEqualTo('z');
		assertThat(r.increment()
			.charAt(0)).isEqualTo('b');
		assertThat(r.increment()
			.charAt(0)).isEqualTo('c');
		assertThat(r.charAt(1)).isEqualTo('d');
		assertThat(r.charAt(2)).isEqualTo('e');
	}

	@Test
	public void testIndexOf() {
		StringRover r = new StringRover("abcdefghijklmnopqrstuvwxyz");
		assertThat(r.indexOf('c', 0)).isEqualTo(2);
		assertThat(r.indexOf('c', 10)).isEqualTo(-1);
		assertThat(r.increment(2)
			.indexOf('c', 0)).isEqualTo(0);
		assertThat(r.increment()
			.indexOf('c', 0)).isEqualTo(-1);
	}

	@Test
	public void testLastIndexOf() {
		StringRover r = new StringRover("abcdefghijklmabcdefghijklm");
		assertThat(r.lastIndexOf('c', 25)).isEqualTo(15);
		assertThat(r.lastIndexOf('c', 10)).isEqualTo(2);
		assertThat(r.lastIndexOf('m', 25)).isEqualTo(25);
		assertThat(r.lastIndexOf('m', 10)).isEqualTo(-1);
		assertThat(r.increment(10)
			.lastIndexOf('c', 15)).isEqualTo(5);
		assertThat(r.increment()
			.lastIndexOf('f', 6)).isEqualTo(-1);
	}

	@Test
	public void testSubString() {
		StringRover r = new StringRover("abcdefghijklmnopqrstuvwxyz");
		assertThat(r.substring(0, 0)).isEmpty();
		assertThat(r.substring(0, 3)).isEqualTo("abc");
		assertThat(r.substring(3, 6)).isEqualTo("def");
		assertThat(r.increment(3)
			.substring(0, 3)).isEqualTo("def");
	}

	@Test
	public void testToString() {
		StringRover r = new StringRover("abcdefghijklmnopqrstuvwxyz");
		assertThat(r).hasToString("abcdefghijklmnopqrstuvwxyz");
		assertThat(r.increment(20)).hasToString("uvwxyz");
		assertThat(r.increment(6)).hasToString("");
	}

	@Test
	public void testConstructor() {
		assertThatNullPointerException().isThrownBy(() -> new StringRover(null));
	}

}
