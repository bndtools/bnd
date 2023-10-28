package aQute.bnd.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PropertiesLineReaderTest {

	@Test
	public void testReadKV() throws Exception {
		PropertiesLineReader lineReader = new PropertiesLineReader("""
			-runee: JavaSE-1.8
			""");
		LineType type;
		while ((type = lineReader.next()) != LineType.eof) {
			if (type == LineType.entry) {
				assertEquals("-runee", lineReader.key());
				assertEquals("JavaSE-1.8", lineReader.value());
			}
		}
	}

	@Test
	public void testReadComment() throws Exception {
		PropertiesLineReader lineReader = new PropertiesLineReader("""
			-runee: JavaSE-1.8

			# this is a comment
			""");
		LineType type;
		while ((type = lineReader.next()) != LineType.eof) {
			if (type == LineType.comment) {
				assertEquals("#", lineReader.key());
				assertEquals("this is a comment", lineReader.value());
			}
		}
	}

	@Test
	public void testReadEmpty() throws Exception {
		PropertiesLineReader lineReader = new PropertiesLineReader("""
			-runee: JavaSE-1.8

			# this is a comment
			""");
		LineType type;
		while ((type = lineReader.next()) != LineType.eof) {
			if (type == LineType.blank) {
				assertThrows(IllegalStateException.class, () -> lineReader.key());
				assertThrows(IllegalStateException.class, () -> lineReader.value());
				lineReader.region(); // should not throw...
			}
		}
	}
}
