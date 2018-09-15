package test;

import java.io.File;

import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class ExtensionsTest extends TestCase {

	private File		tmp;
	private Workspace	ws;

	@Override
	public void setUp() throws Exception {
		tmp = IO.getFile("tmp");
		ws = new Workspace(IO.getFile("testresources/ws-extensions"));
	}

	@Override
	public void tearDown() throws Exception {
		ws.close();
		IO.delete(tmp);
	}

	public void testExtensionSimple() throws Exception {
		String plugin = ws.getPlugin(String.class);
		assertNotNull(plugin);
		assertEquals("hello, I am a test extension", plugin);
	}
}
