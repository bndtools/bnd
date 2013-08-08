package aQute.lib.settings;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.lib.io.*;

public class SettingsTest extends TestCase {

	public void testSimple() throws Exception {
		File tmp = IO.getFile("tmp");
		IO.delete(tmp);
		Settings s = new Settings("tmp");
		try {
			s.put("a", "b");
			assertEquals("b", s.get("a"));
			assertNotNull(s.getPublicKey());
			byte[] publicKey = s.getPublicKey();
			s.save();
			
			Settings ss = new Settings("tmp");
			assertEquals("b", ss.get("a"));
			assertNotNull(ss.getPublicKey());
			assertTrue( Arrays.equals(publicKey, ss.getPublicKey()));
			
			ss.clear();
			assertNull(ss.get("a"));
			
		}
		finally {
			IO.delete(tmp);
		}
	}
}
