package biz.aQute.launcher;

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
import java.util.Properties;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

@ExtendWith(SoftAssertionsExtension.class)
public class LauncherTest {
	@InjectSoftAssertions
	SoftAssertions softly;

	@InjectTemporaryDirectory
	File				testDir;

	private Properties			prior;

	@BeforeEach
	public void before() throws Exception {
		prior = new Properties();
		prior.putAll(System.getProperties());
	}

	/**
	 * Testing the embedded launcher is quite tricky. This test uses a
	 * prefabricated packaged jar. Notice that you need to reexport that jar for
	 * every change in the launcher since it embeds the launcher. This jar is
	 * run twice to see if the second run will not reinstall the bundles.
	 */

	@AfterEach
	public void after() {
		System.setProperties(prior);
	}

	@Test
	public void testRunOrder_0_no_start_levels() throws Exception {
		File file = buildPackage("order-00.bndrun");

		System.setProperty("test.cmd", "quit.no.exit");

		String result = runFramework(file);

		softly.assertThat(result)
			.containsPattern("startlevel: not handled")
			.containsPattern("Startlevel\\s+1")
			.containsPattern("0\\s+ACTIV\\s+<>\\s+System Bundle")
			.containsPattern("1\\s+ACTIV\\s+<>\\s+jar/.?org.apache.felix.log")
			.containsPattern("1\\s+ACTIV\\s+<>\\s+jar/.?demo.jar")
			.containsPattern("1\\s+ACTIV\\s+<>\\s+jar/.?org.apache.servicemix.bundles.junit")
			.containsPattern("1\\s+ACTIV\\s+<>\\s+jar/.?org.apache.felix.configadmin");
	}

	@Test
	public void testRunOrder_1_basic() throws Exception {
		File file = buildPackage("order-01.bndrun");

		System.setProperty("test.cmd", "quit.no.exit");

		String result = runFramework(file);

		softly.assertThat(result)
			.containsPattern("Startlevel\\s+22")
			.containsPattern("0\\s+ACTIV\\s+<>\\s+System Bundle")
			.containsPattern("21\\s+ACTIV\\s+<>\\s+jar/.?org.apache.felix.log")
			.containsPattern("10\\s+ACTIV\\s+<>\\s+jar/.?demo.jar")
			.containsPattern("20\\s+ACTIV\\s+<>\\s+jar/.?org.apache.servicemix.bundles.junit")
			.containsPattern("5\\s+ACTIV\\s+<>\\s+jar/.?org.apache.felix.configadmin")
			.containsPattern("startlevel: default=21, beginning=22")
			.containsPattern("startlevel: notified reached final level 22");
	}

	@Test
	public void testRunOrder_2_decorations() throws Exception {
		File file = buildPackage("order-02.bndrun");

		System.setProperty("test.cmd", "quit.no.exit");

		String result = runFramework(file);

		softly.assertThat(result)
			.containsPattern("Startlevel\\s+23")
			.containsPattern("0\\s+ACTIV\\s+<>\\s+System Bundle")
			.containsPattern("22\\s+ACTIV\\s+<>\\s+jar/.?org.apache.felix.log")
			.containsPattern("11\\s+ACTIV\\s+<>\\s+jar/.?demo.jar")
			.containsPattern("21\\s+ACTIV\\s+<>\\s+jar/.?org.apache.servicemix.bundles.junit")
			.containsPattern("startlevel: default=22, beginning=23")
			.containsPattern("startlevel: notified reached final level 23");
	}

	@Test
	public void testRunOrder_3_manual_beginning_level() throws Exception {
		System.getProperties()
			.remove("launch.properties");
		File file = buildPackage("order-03.bndrun");

		System.setProperty("test.cmd", "quit.no.exit");

		String result = runFramework(file);

		softly.assertThat(result)
			.containsPattern("Startlevel\\s+12")
			.containsPattern("0\\s+ACTIV\\s+<>\\s+System Bundle")
			.containsPattern("22\\s+RSLVD\\s+<>\\s+jar/.?org.apache.felix.log")
			.containsPattern("11\\s+ACTIV\\s+<>\\s+jar/.?demo.jar")
			.containsPattern("21\\s+RSLVD\\s+<>\\s+jar/.?org.apache.servicemix.bundles.junit")
			.containsPattern("6\\s+ACTIV\\s+<>\\s+jar/.?org.apache.felix.configadmin")
			.containsPattern("startlevel: default=22, beginning=12")
			.containsPattern("startlevel: notified reached final level 12");
	}

	@Test
	public void testPackaged() throws Exception {
		File file = buildPackage("keep.bndrun");

		System.setProperty("test.cmd", "quit.no.exit");

		String result = runFramework(file);
		softly.assertThat(result)
			.contains("installing jar/demo.jar");

		result = runFramework(file);
		softly.assertThat(result)
			.contains("not updating jar/demo.jar because identical digest");
	}

	@Test
	public void testFrameworkRestart() throws Exception {
		File file = buildPackage("frameworkrestart.bndrun");

		System.setProperty("test.cmd", "framework.restart");

		String result = runFramework(file);
		System.out.println(result);

		softly.assertThat(result)
			.contains("framework restart, first time")
			.contains("framework restart, second time");
	}

	/**
	 * Tests the EmbeddedLauncher by creating an instance and calling the run
	 * method. We Check if the expected exit value is printed in the result.
	 *
	 * @throws Exception
	 */
	@Test
	public void testEmbeddedLauncherWithRunMethod() throws Exception {
		File file = buildPackage("keep.bndrun");

		System.setProperty("test.cmd", "quit.no.exit");

		String result = runFrameworkWithRunMethod(file);
		softly.assertThat(result)
			.contains("installing jar/demo.jar", "Exited with 197");

	}

	/**
	 * Tests the EmbeddedLauncher without any trace logging
	 *
	 * @throws Exception
	 */
	@Test
	public void testEmbeddedLauncherNoTrace() throws Exception {
		File file = buildPackage("keep_notrace.bndrun");

		System.setProperty("test.cmd", "quit.no.exit");
		System.setProperty("launch.trace", "false");

		String result = runFrameworkWithRunMethod(file);
		softly.assertThat(result)
			.contains("quit.no.exit")
			.doesNotContain("[EmbeddedLauncher] looking for Embedded-Runpath in META-INF/MANIFEST.MF");
	}

	/**
	 * Tests the EmbeddedLauncher without trace logging
	 *
	 * @throws Exception
	 */
	@Test
	public void testEmbeddedLauncherTrace() throws Exception {
		File file = buildPackage("keep_notrace.bndrun");

		System.setProperty("test.cmd", "quit.no.exit");
		System.setProperty("launch.trace", "true");

		String result = runFrameworkWithRunMethod(file);
		softly.assertThat(result)
			.contains("installing jar/demo.jar",
			"[EmbeddedLauncher] looking for Embedded-Runpath in META-INF/MANIFEST.MF");
	}

	private void assertNoErrorsOrWarnings(Processor p, String context) throws IOException {
		softly.assertThat(p.check())
			.as(context + ": check()")
			.isTrue();
		softly.assertThat(p.isPerfect())
			.as(context + ": isPerfect()")
			.isTrue();
		softly.assertThat(p.getWarnings())
			.as(context + ": getWarnings()")
			.isEmpty();
		softly.assertThat(p.getErrors())
			.as(context + ": getErrors()")
			.isEmpty();
	}

	private File buildPackage(String bndrun) throws Exception {
		File tgt = IO.copy(new File(bndrun), new File(testDir, bndrun));
		try (Workspace ws = new Workspace(new File("..")); Run run = Run.createRun(ws, tgt)) {
			File file = new File(testDir, "packaged.jar");
			try (Jar pack = run.pack(null)) {
				assertNoErrorsOrWarnings(run, "run pre pack");
				assertNoErrorsOrWarnings(ws, "ws pre pack");
				pack.write(file);
			}
			assertNoErrorsOrWarnings(run, "run post pack");
			softly.assertThat(file)
				.as("file")
				.isFile();
			// Short-circuit the test execution if we have discovered errors
			// already as the rest of it won't produce sensible results.
			if (softly.assertionErrorsCollected()
				.size() > 0) {
				softly.assertAll();
			}
			return file;
		}
	}

	private String runFramework(File file) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
		InvocationTargetException, IOException, MalformedURLException {
		PrintStream err = System.err;
		PrintStream out = System.out;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintStream out2 = new PrintStream(bout);
		System.setErr(out2);
		System.setOut(out2);
		try {
			try (URLClassLoader l = new URLClassLoader(new URL[] {
				file.toURI()
					.toURL()
			}, null)) {
				Class<?> launcher = l.loadClass("aQute.launcher.pre.EmbeddedLauncher");
				Method main = launcher.getDeclaredMethod("main", String[].class);
				main.invoke(null, (Object) new String[] {});
			}

			out2.flush();
		} finally {
			System.setErr(err);
			System.setOut(out);
		}

		return new String(bout.toByteArray(), StandardCharsets.UTF_8);
	}

	private String runFrameworkWithRunMethod(File file)
		throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
		IOException, MalformedURLException, InstantiationException, IllegalArgumentException, SecurityException {
		PrintStream err = System.err;
		PrintStream out = System.out;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintStream out2 = new PrintStream(bout);
		System.setErr(out2);
		System.setOut(out2);
		try {
			try (URLClassLoader l = new URLClassLoader(new URL[] {
				file.toURI()
					.toURL()
			}, null)) {
				Class<?> launcher = l.loadClass("aQute.launcher.pre.EmbeddedLauncher");
				Object o = launcher.getConstructor()
					.newInstance();
				Method run = launcher.getDeclaredMethod("run", String[].class);
				int result = (int) run.invoke(o, (Object) new String[] {});
				System.out.println("Exited with " + result);
			}
			out2.flush();
		} finally {
			System.setErr(err);
			System.setOut(out);
		}

		return new String(bout.toByteArray(), StandardCharsets.UTF_8);
	}
}
