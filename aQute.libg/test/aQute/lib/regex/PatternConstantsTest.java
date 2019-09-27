package aQute.lib.regex;

import java.util.regex.Pattern;

import org.junit.Test;

public class PatternConstantsTest {
	@Test
	public void testCompileTOKEN() {
		Pattern.compile(PatternConstants.TOKEN);
	}

	@Test
	public void testCompileSYMBOLICNAME() {
		Pattern.compile(PatternConstants.SYMBOLICNAME);
	}

	@Test
	public void testCompileSHA1() {
		Pattern.compile(PatternConstants.SHA1);
	}

}
