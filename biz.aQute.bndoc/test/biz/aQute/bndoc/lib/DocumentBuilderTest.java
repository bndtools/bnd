package biz.aQute.bndoc.lib;

import java.io.*;

import junit.framework.*;
import aQute.lib.env.*;

public class DocumentBuilderTest extends TestCase {
	public void testBlock() throws IOException {
		DocumentBuilder db = new DocumentBuilder(new Env());
		
		assertEquals( "A\n  A\n  ...\n", db._block( new String[]{"","1", "2","2", "ABCDEF\nAbcdef\nA\n"}));
		assertEquals( "ABC\n  Abc\n  ...\n", db._block( new String[]{"","3", "2","2", "ABCDEF\nAbcdef\nA\n"}));
		assertEquals( "A\nA\n...\n", db._block( new String[]{"","1", "2","0", "A\nA\nA\n"}));
		assertEquals( "A\n  A\n  ...\n", db._block( new String[]{"","1", "2","2", "A\nA\nA\n"}));
		db.close();
	}
}
