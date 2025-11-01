package bndtools.util.ui;

import static bndtools.editor.completion.BackslashValidator.Result.ERROR;
import static bndtools.editor.completion.BackslashValidator.Result.REFRESH;
import static bndtools.editor.completion.BackslashValidator.Result.UNDEFINED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.junit.jupiter.api.Test;

import bndtools.editor.completion.BackslashValidator;
import bndtools.editor.completion.BackslashValidator.Result;

class BndScannerTest {

	@Test
	void testNoBackslashAtAll() throws Exception {
		assertScanner("abc", UNDEFINED);
	}

	@Test
	void testSingleBackslashFollowedBySpace() throws Exception {
		assertScanner("\\ ", ERROR);
	}

	@Test
	void testSingleBackslashFollowedBy2Spaces() throws Exception {
		assertScanner("\\  ", ERROR);
	}

	@Test
	void testSingleBackslashFollowedBy2Tabs() throws Exception {
		assertScanner("\\\t\t", ERROR);
	}

	@Test
	void testSingleBackslashFollowedBySpaceThenEOL() throws Exception {
		assertScanner("\\ \n", ERROR);
	}

	@Test
	void testSingleBackslashFollowedByTabThenEOL() throws Exception {
		assertScanner("\\\t\n", ERROR);
	}

	@Test
	void testSingleBackslashFollowedBySpaceThenEOL2() throws Exception {
		assertScanner("\\ \r\n", ERROR);
	}

	@Test
	void testSingleBackslashFollowedByTabThenEOL2() throws Exception {
		assertScanner("\\\t\r\n", ERROR);
	}

	@Test
	void testSingleBackslashFollowedByEOL() throws Exception {
		assertScanner("\\\n", UNDEFINED);
	}

	@Test
	void testSingleBackslashFollowedByEOL2() throws Exception {
		assertScanner("\\\r\n", UNDEFINED);
	}


	@Test
	void testSingleBackslashFollowedByTab() {
		assertScanner("\\\t", ERROR);
	}

	@Test
	void testOddBackslashAtEOF() {
		assertScanner("\\", REFRESH);
	}

	@Test
	void testEvenBackslashes() {
		assertScanner("\\\\", UNDEFINED);
	}

	@Test
	void testValidEscapeAfterOddBackslash() {
		assertScanner("\\n", UNDEFINED);
		assertScanner("\\t", UNDEFINED);
		assertScanner("\\r", UNDEFINED);
		assertScanner("\\f", UNDEFINED);
		assertScanner("\\\\", UNDEFINED);
		assertScanner("\\:", UNDEFINED);
		assertScanner("\\=", UNDEFINED);
		assertScanner("\\#", UNDEFINED);
		assertScanner("\\!", UNDEFINED);
	}

	@Test
	void testUnicodeEscape() {
		assertScanner("\\u1234", UNDEFINED);
	}

	@Test
	void testInvalidUnicodeEscape() {
		assertScanner("\\u12G4", ERROR);
	}

	@Test
	void testInvalidUnicodeEscapeEOL() {
		assertScanner("\\u12\n", ERROR);
	}

	@Test
	void testElse() {
		assertScanner("\\a", ERROR);
	}

	private void assertScanner(String s, Result expected) {
		MockScanner scanner = new MockScanner(s);
		int first = scanner.read();
		assertEquals(expected, BackslashValidator.handleBackslashes(scanner, first), "Failed for input: `" + s + "`");
	}

}

class MockScanner implements ICharacterScanner {
	private final char[]	chars;
	private int				pos	= 0;

	MockScanner(String s) {
		chars = s.toCharArray();
	}

	@Override
	public int read() {
		if (pos >= chars.length)
			return EOF;
		return chars[pos++];
	}

	@Override
	public void unread() {
		if (pos > 0)
			pos--;
	}

	@Override
	public int getColumn() {
		return pos;
	}

	@Override
	public char[][] getLegalLineDelimiters() {
		return new char[0][];
	}
}
