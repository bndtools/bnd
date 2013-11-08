package test;

import java.io.*;

import aQute.bnd.build.*;
import aQute.lib.io.*;
import junit.framework.*;

public class ExtensionsTest extends TestCase {

	private File	tmp;
	private Workspace	ws;

	public void setUp() throws Exception {
		tmp = IO.getFile("tmp");
		ws = new Workspace( IO.getFile("testresources/ws-extensions"));
	}
	
	public void tearDown() throws Exception {
		IO.delete(tmp);
	}
	
	public void testExtensionSimple() throws Exception {
		String plugin = ws.getPlugin(String.class);
		assertNotNull(plugin);
		assertEquals("hello, I am a test extension",plugin);
	}
}
