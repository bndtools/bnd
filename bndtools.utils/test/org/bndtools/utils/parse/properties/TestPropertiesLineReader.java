package org.bndtools.utils.parse.properties;

import static org.bndtools.utils.parse.properties.LineType.blank;
import static org.bndtools.utils.parse.properties.LineType.comment;
import static org.bndtools.utils.parse.properties.LineType.entry;
import static org.bndtools.utils.parse.properties.LineType.eof;

import org.eclipse.jface.text.Region;

import junit.framework.TestCase;

public class TestPropertiesLineReader extends TestCase {

	public static void testEmpty() throws Exception {
		String input = "\n" + "  \n" + "\t  ";
		PropertiesLineReader reader = new PropertiesLineReader(input);

		assertEquals(blank, reader.next());
		assertEquals(new Region(0, 0), reader.region());

		assertEquals(blank, reader.next());
		assertEquals(new Region(1, 2), reader.region());

		assertEquals(blank, reader.next());
		assertEquals(new Region(4, 3), reader.region());

		assertEquals(eof, reader.next());
	}

	public static void testRegionAfterEOF() throws Exception {
		PropertiesLineReader reader = new PropertiesLineReader("");
		assertEquals(blank, reader.next());
		assertEquals(new Region(0, 0), reader.region());
		assertEquals(eof, reader.next());
		try {
			reader.region();
			fail("Should throw IllegalStateException");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public static void testComment() throws Exception {
		String input = "# comment";
		PropertiesLineReader reader = new PropertiesLineReader(input);
		assertEquals(comment, reader.next());
		assertEquals(new Region(0, 9), reader.region());
		assertEquals(eof, reader.next());
	}

	public static void testCommentLines() throws Exception {
		String input = "# comment1\n" + "# comment2\n" + "   # comment3\n" + "! comment4\n" + "   ! comment5";
		PropertiesLineReader reader = new PropertiesLineReader(input);
		assertEquals(comment, reader.next());
		assertEquals(new Region(0, 10), reader.region());

		assertEquals(comment, reader.next());
		assertEquals(new Region(11, 10), reader.region());

		assertEquals(comment, reader.next());
		assertEquals(new Region(22, 13), reader.region());

		assertEquals(comment, reader.next());
		assertEquals(new Region(36, 10), reader.region());

		assertEquals(comment, reader.next());
		assertEquals(new Region(47, 13), reader.region());

		assertEquals(eof, reader.next());
	}

	public static void testCommentsDontContinue() throws Exception {
		// first comment ends with backslash but this shouldn't continue the
		// line
		String input = "# comment\\\n# comment";

		PropertiesLineReader reader = new PropertiesLineReader(input);
		assertEquals(comment, reader.next());
		assertEquals(new Region(0, 10), reader.region());

		assertEquals(comment, reader.next());
		assertEquals(new Region(11, 9), reader.region());

		assertEquals(eof, reader.next());
	}

	public static void testEntryLines() throws Exception {
		String input = "foo=bar\n" + "foo2:bar2\n" + "   foo3:bar3";
		PropertiesLineReader reader = new PropertiesLineReader(input);

		assertEquals(entry, reader.next());
		assertEquals(new Region(0, 7), reader.region());
		assertEquals("foo", reader.key());

		assertEquals(entry, reader.next());
		assertEquals(new Region(8, 9), reader.region());
		assertEquals("foo2", reader.key());

		assertEquals(entry, reader.next());
		assertEquals(new Region(18, 12), reader.region());
		assertEquals("foo3", reader.key());
	}

	public static void testContinuedLine() throws Exception {
		String input = "foo=bar,\\\n" + "  baz";
		PropertiesLineReader reader = new PropertiesLineReader(input);

		assertEquals(entry, reader.next());
		assertEquals(new Region(0, 15), reader.region());
		assertEquals("foo", reader.key());
	}

	public static void testTrailingWhitespaceAfterKey() throws Exception {
		String input = "   hello  :world";
		PropertiesLineReader reader = new PropertiesLineReader(input);

		assertEquals(entry, reader.next());
		assertEquals(new Region(0, 16), reader.region());
		assertEquals("hello", reader.key());
	}

	public static void testEscapedCharsInKey() throws Exception {
		String input = "hel\\ \\ lo: world\n" + "hell\\=o\\:world=foo";
		PropertiesLineReader reader = new PropertiesLineReader(input);

		assertEquals(entry, reader.next());
		assertEquals(new Region(0, 16), reader.region());
		assertEquals("hel  lo", reader.key());

		assertEquals(entry, reader.next());
		assertEquals(new Region(17, 18), reader.region());
		assertEquals("hell=o:world", reader.key());
	}

	public static void testEmptyValue() throws Exception {
		String input = "cheeses";
		PropertiesLineReader reader = new PropertiesLineReader(input);

		assertEquals(entry, reader.next());
		assertEquals(new Region(0, 7), reader.region());
		assertEquals("cheeses", reader.key());
	}

	public static void testCarriageReturnAndLineFeed() throws Exception {
		String input = "line1=foo\r\n" + "line2=foo,\\\r\n" + "  bar";
		PropertiesLineReader reader = new PropertiesLineReader(input);

		assertEquals(entry, reader.next());
		assertEquals(new Region(0, 9), reader.region());
		assertEquals("line1", reader.key());

		assertEquals(entry, reader.next());
		assertEquals(new Region(11, 18), reader.region());
		assertEquals("line2", reader.key());
	}
}
