package aQute.bnd.test;

import junit.framework.*;
import aQute.bnd.build.model.conversions.*;

public class NewlineEscapedStringFormatterTestCase extends TestCase {
	static public void testNewlines() {
		NewlineEscapedStringFormatter formatter = new NewlineEscapedStringFormatter();

		String i = "1\n2\n3\n";
		String expected = "1\\n\\\n\t2\\n\\\n\t3\\n\\\n\t";
		String o = formatter.convert(i);

		assertEquals(expected, o);
	}
}
