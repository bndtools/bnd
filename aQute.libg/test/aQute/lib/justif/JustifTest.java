package aQute.lib.justif;

import java.util.*;

import junit.framework.*;

public class JustifTest extends TestCase {

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
		}
		finally {
			f.close();
		}
	}
}
