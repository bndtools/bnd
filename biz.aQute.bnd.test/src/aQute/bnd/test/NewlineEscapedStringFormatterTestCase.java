package aQute.bnd.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.model.conversions.NewlineEscapedStringFormatter;

@SuppressWarnings("restriction")
public class NewlineEscapedStringFormatterTestCase {
	@Test
	public void testNewlines() {
		NewlineEscapedStringFormatter formatter = new NewlineEscapedStringFormatter();

		String i = "1\n2\n3\n";
		String expected = "1\\n\\\n\t2\\n\\\n\t3\\n\\\n\t";
		String o = formatter.convert(i);

		assertEquals(expected, o);
	}
}
