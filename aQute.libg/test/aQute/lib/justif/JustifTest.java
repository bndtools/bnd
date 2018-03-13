package aQute.lib.justif;

import java.util.Formatter;

import junit.framework.TestCase;

public class JustifTest extends TestCase {

	public void testTab() {
		// tabs stay in column
		assertJ("a\t0b\t1c\fd\ne", "a    b    c\n          d\ne", 50);
		// tabs
		assertJ("a\t0b\t1c\t2d\t3e", "a    b    c    d    e", 50);
		// line break without spaces
		assertJ("abcdefghijklmnopqrstuvwxyz", "abcdefghij\nklmnopqrst\nuvwxyz", 10);
		// line break with spaces
		assertJ("a b c d e f g h i j k l m n o p q r s t u v w x y z",
			"a b c d e\nf g h i j\nk l m n o\np q r s t\nu v w x y\nz", 10);
	}

	void assertJ(String input, String expected, int w) {
		Justif j = new Justif(w, 5, 10, 15, 20, 25, 30, 40, 45, 50);
		j.formatter()
			.format(input);
		String s = j.wrap();
		assertEquals(expected, s);
		System.out.println(s);
	}

	public void testSimple() throws Exception {
		StringBuilder sb = new StringBuilder();
		Formatter f = new Formatter(sb);
		try {
			Justif j = new Justif(40, 4, 8);

			f.format("0123456789012345\nxx\t0-\t1can\n"
				+ "           use instead of including individual modules in your project. Note:\n"
				+ "           It does not include the Jiffle scripting language or Jiffle image\n"
				+ "           operator.");
			f.flush();

			j.wrap(sb);

			System.out.println(sb);
		} finally {
			f.close();
		}
	}
}
