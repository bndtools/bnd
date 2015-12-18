package aQute.lib.settings;

import java.io.File;
import java.util.Arrays;

import aQute.lib.io.IO;
import junit.framework.TestCase;

public class SettingsTest extends TestCase {

	public void testSimple() throws Exception {
		File tmp = IO.getFile("tmp.json");
		IO.delete(tmp);
		Settings s = new Settings(tmp.getName());
		try {
			s.put("a", "b");
			assertEquals("b", s.get("a"));
			assertNotNull(s.getPublicKey());
			byte[] publicKey = s.getPublicKey();
			s.save();

			Settings ss = new Settings(tmp.getName());
			assertEquals("b", ss.get("a"));
			assertNotNull(ss.getPublicKey());
			assertTrue(Arrays.equals(publicKey, ss.getPublicKey()));

			ss.clear();
			assertNull(ss.get("a"));

		} finally {
			IO.delete(tmp);
		}
	}
}
