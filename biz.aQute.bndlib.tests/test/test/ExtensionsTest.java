package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.build.Workspace;
import aQute.bnd.util.home.Home;
import aQute.lib.io.IO;

public class ExtensionsTest {

	private File		tmp;
	private Workspace	ws;

	@BeforeEach
	public void setUp() throws Exception {
		tmp = IO.getFile("tmp");
		ws = new Workspace(IO.getFile("testresources/ws-extensions"));
		File cacheDir = IO.getFile(ws.getProperty(Workspace.CACHEDIR, Home.getUserHomeBnd("caches/shas")
			.getAbsolutePath()));
		File dir = IO.getFile(cacheDir, "2D96DA7F7A81443130072C718CB40CE5182DA362");
		IO.delete(dir);
	}

	@AfterEach
	public void tearDown() throws Exception {
		ws.close();
		IO.delete(tmp);
	}

	@Test
	public void testExtensionSimple() throws Exception {
		String plugin = ws.getPlugin(String.class);
		assertNotNull(plugin);
		assertEquals("hello, I am a test extension", plugin);
	}
}
