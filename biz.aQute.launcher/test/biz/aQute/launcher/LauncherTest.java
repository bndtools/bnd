package biz.aQute.launcher;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import aQute.launcher.Launcher;
import aQute.launcher.pre.EmbeddedLauncher;
import aQute.lib.io.IO;

public class LauncherTest {

	/**
	 * Testing the embedded launcher is quite tricky. This test uses a
	 * prefabricated packaged jar. Notice that you need to reexport that jar for
	 * every change in the launcher since it embeds the launcher. This jar is
	 * run twice to see if the second run will not reinstall the bundles.
	 */

	@Test
	public void testPackaged() throws Exception {
		System.setProperty("test.cmd", "quit.no.exit");
		File fwdir = IO.getFile("generated/keepfw");
		IO.delete(fwdir);

		System.setProperty("org.osgi.framework.storage", fwdir.getAbsolutePath());

		Class<Launcher> ll = Launcher.class;
		File file = IO.getFile("testresources/packaged.jar");
		assertTrue(file.isFile());

		String result = runFramework(file);
		assertTrue(result.contains("installing jar/demo.jar"));

		result = runFramework(file);
		assertTrue(result.contains("not updating jar/demo.jar because identical sha"));

	}

	private String runFramework(File file) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
			InvocationTargetException, IOException, MalformedURLException {
		PrintStream out = System.err;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintStream out2 = new PrintStream(bout);
		System.setErr(out2);
		
		try (URLClassLoader l = new URLClassLoader(new URL[] {
				file.toURI().toURL()
		}, null)) {
			Class< ? > launcher = l.loadClass(EmbeddedLauncher.class.getName());
			Method main = launcher.getDeclaredMethod("main", String[].class);
			main.invoke(null, (Object) new String[] {});
		}

		out2.flush();
		System.setErr(out);

		return new String(bout.toByteArray(), StandardCharsets.UTF_8);
	}
}
