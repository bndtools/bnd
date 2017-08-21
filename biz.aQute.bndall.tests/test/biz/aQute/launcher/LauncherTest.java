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

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;

public class LauncherTest {

	private static final String GENERATED_PACKAGED_JAR = "generated/packaged.jar";

	/**
	 * Testing the embedded launcher is quite tricky. This test uses a
	 * prefabricated packaged jar. Notice that you need to reexport that jar for
	 * every change in the launcher since it embeds the launcher. This jar is
	 * run twice to see if the second run will not reinstall the bundles.
	 */

	@Test
	public void testPackaged() throws Exception {
		File file = buildPackage();

		System.setProperty("test.cmd", "quit.no.exit");
		File fwdir = IO.getFile("generated/keepfw");
		IO.delete(fwdir);

		System.setProperty("org.osgi.framework.storage", fwdir.getAbsolutePath());

		assertTrue(file.isFile());

		String result = runFramework(file);
		assertTrue(result.contains("installing jar/demo.jar"));

		result = runFramework(file);
		assertTrue(result.contains("not updating jar/demo.jar because identical digest"));

	}

	private File buildPackage() throws Exception, IOException {
		Workspace ws = Workspace.findWorkspace(IO.work);
		Run run = Run.createRun(ws, IO.getFile("keep.bndrun"));

		File file = IO.getFile(GENERATED_PACKAGED_JAR);
		try (Jar pack = run.pack(null)) {
			assertTrue(ws.check());
			assertTrue(run.check());
			pack.write(file);
		}
		return file;
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
			Class< ? > launcher = l.loadClass("aQute.launcher.pre.EmbeddedLauncher");
			Method main = launcher.getDeclaredMethod("main", String[].class);
			main.invoke(null, (Object) new String[] {});
		}

		out2.flush();
		System.setErr(out);

		return new String(bout.toByteArray(), StandardCharsets.UTF_8);
	}
}
