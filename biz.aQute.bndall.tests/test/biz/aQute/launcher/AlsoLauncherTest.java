package biz.aQute.launcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.util.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.ProjectLauncher.NotificationType;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.Strategy;
import aQute.lib.io.IO;
import aQute.libg.command.Command;

public class AlsoLauncherTest {
	public static final String	TMPDIR		= "generated/tmp/test";
	@Rule
	public final TestName		testName	= new TestName();
	private File				testDir;

	private Workspace			workspace;
	private Project				project;

	@Before
	public void setUp() throws Exception {
		testDir = new File(TMPDIR, testName.getMethodName());
		IO.delete(testDir);
		IO.mkdirs(testDir);
		File wsRoot = new File(testDir, "test ws");
		for (String folder : Arrays.asList("cnf", "demo", "biz.aQute.launcher", "biz.aQute.junit",
			"biz.aQute.tester")) {
			File tgt = new File(wsRoot, folder);
			IO.copy(new File("..", folder), tgt);
			IO.delete(new File(tgt, "generated/buildfiles"));
		}
		workspace = new Workspace(wsRoot);
		project = workspace.getProject("demo");
		project.setTrace(true);
		assertTrue(project.check());
	}

	@SuppressWarnings("restriction")
	@After
	public void tearDown() throws Exception {
		IO.close(project);
		IO.close(workspace);
		System.getProperties()
			.remove("test.cmd");
		for (String key : aQute.launcher.constants.LauncherConstants.LAUNCHER_PROPERTY_KEYS) {
			System.getProperties()
				.remove(key);
		}
	}

	@Test
	public void testExecutableJarWithStripping() throws Exception {
		long full = make(project, null);
		long optStripped = make(project, "strip='OSGI-OPT/*'");
		long optStrippedAndNoBndrun = make(project, "strip='OSGI-OPT/*,*.bndrun'");
		long optNoBndrun = make(project, "strip='*.bndrun'");

		assertThat(full).isGreaterThan(optStripped);
		assertThat(optStripped).isGreaterThan(optStrippedAndNoBndrun);
		assertThat(optStrippedAndNoBndrun).isLessThan(optNoBndrun);

	}

	private long make(Project p, String option) throws Exception {
		if (option != null)
			p.setProperty(Constants.EXECUTABLE, option);
		try (ProjectLauncher l = project.getProjectLauncher(); Jar executable = l.executable()) {
			File tmp = Files.newTemporaryFile();
			try {
				executable.write(tmp);
				return tmp.length();
			} finally {
				IO.delete(tmp);
			}
		}
	}

	/**
	 * Test the rejar and strip properties of the -executable instruction
	 */

	@Test
	public void testExecutableWithRejarringAndStripping() throws Exception {

		long storedStored = makeExec(false, false, false);
		long storedDeflate = makeExec(false, false, true);
		long deflateDeflate = makeExec(false, true, true);
		long deflateStored = makeExec(false, true, false);
		long stripStoredStored = makeExec(true, false, false);
		long stripStoredDeflate = makeExec(true, false, true);
		long stripDeflateDeflate = makeExec(true, true, true);
		long stripDeflateStored = makeExec(true, true, false);

		assertThat(deflateStored).isLessThan(deflateDeflate);
		assertThat(deflateDeflate).isLessThan(storedDeflate);
		assertThat(storedDeflate).isLessThan(storedStored);

		assertThat(stripStoredStored).isLessThan(storedStored);
		assertThat(stripStoredDeflate).isLessThan(storedDeflate);
		assertThat(stripDeflateDeflate).isLessThan(deflateDeflate);
		assertThat(stripDeflateStored).isLessThan(deflateStored);
	}

	private long makeExec(boolean strip, boolean outer, boolean inner) throws Exception {
		project.setProperty(Constants.RUNPROPERTIES, "test.cmd=exit");
		project.setProperty(Constants.RUNTRACE, "false");
		if (outer) {
			project.setProperty(Constants.COMPRESSION, "DEFLATE");
			System.out.println("outer deflate");
		} else {
			project.setProperty(Constants.COMPRESSION, "STORE");
			System.out.println("outer store");
		}

		if (inner) {
			if (strip) {
				project.setProperty(Constants.EXECUTABLE, "rejar=DEFLATE,strip='OSGI-OPT/*,META-INF/maven/*'");
				System.out.println("inner deflate & strip");
			} else {
				project.setProperty(Constants.EXECUTABLE, "rejar=DEFLATE");
				System.out.println("inner deflate & no strip");
			}
		} else {
			if (strip) {
				project.setProperty(Constants.EXECUTABLE, "rejar=STORE,strip='OSGI-OPT/*,META-INF/maven/*'");
				System.out.println("inner store & strip");
			} else {
				project.setProperty(Constants.EXECUTABLE, "rejar=STORE");
				System.out.println("inner store & no strip");
			}
		}

		try (ProjectLauncher l = project.getProjectLauncher(); Jar executable = l.executable()) {
			File tmp = Files.newTemporaryFile();
			try {
				executable.write(tmp);
				System.out.println("size " + tmp.length());
				System.out.println();

				Command cmd = new Command();
				String java = System.getProperty("java", "java");
				cmd.add(java);
				cmd.add("-jar");
				cmd.add(tmp.getAbsolutePath());

				int execute = cmd.execute(System.out, System.err);

				assertThat(execute).isEqualTo(42);

				return tmp.length();
			} finally {
				IO.delete(tmp);
			}
		}
	}

	/**
	 * Create an executable JAR, expand it in a directory and run the demo test
	 * command that quits but does not call System.exit(). That is, if this
	 * returns normally all went ok.
	 */
	@Test
	public void testExpandedJarLauncher() throws Exception {
		project.setProperty(Constants.RUNPROPERTIES, "test.cmd=quit.no.exit");
		ProjectLauncher l = project.getProjectLauncher();
		File temporaryFolder = Files.newTemporaryFolder();
		try {
			try (Jar executable = l.executable()) {
				executable.writeFolder(temporaryFolder);
			}
			try (URLClassLoader loader = new URLClassLoader(new URL[] {
				temporaryFolder.toURI()
					.toURL()
			}, null)) {
				Class<?> launcher = loader.loadClass("aQute.launcher.pre.EmbeddedLauncher");
				Method method = launcher.getMethod("main", String[].class);
				method.invoke(null, (Object) new String[0]);
			}
		} finally {
			IO.delete(temporaryFolder);
		}
	}

	/**
	 * Test the packager for remote
	 *
	 * @throws Exception
	 */
	@Test
	public void testRemotePackager() throws Exception {
		project.setProperty("-runpath", "biz.aQute.remote.launcher;version=latest");
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTrace(true);
			try (Jar executable = l.executable()) {
				assertTrue(project.check());
				assertNotNull(executable);
			}
		}
	}

	/**
	 * Try out the new tester that does not contain JUnit
	 */
	@Test
	public void testJUnitLessTester() throws Exception {
		List<Container> bundles = project.getBundles(Strategy.HIGHEST, "biz.aQute.tester", "TESTER");
		assertThat(bundles).hasSize(1);

		project.setProperty(Constants.TESTPATH, "");
		project.setProperty(Constants.TESTER, "biz.aQute.tester");
		assertTrue(project.check());

		ProjectTester pt = project.getProjectTester();
		pt.addTest("test.TestCase1");
		pt.addTest("test.TestCase2:m1");

		assertThat(pt.test()).isEqualTo(2);
	}

	/**
	 * Gradle Problems exporting an executable jar #980 Test the packager's
	 * difference between plan export in gradle & from bndtools
	 *
	 * @throws Exception
	 */
	@Test
	public void testPackagerDifference1() throws Exception {
		//
		// First as we basically do it in bndtools for a project
		//
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTrace(true);
			try (Jar executable = l.executable()) {
				assertNotNull(executable);

				Properties p = new Properties();
				Resource resource = executable.getResource("launcher.properties");

				try (InputStream in = resource.openInputStream()) {
					p.load(in);
				}

				assertThat(p).containsEntry("in.workspace", "workspace")
					.containsEntry("in.project", "project")
					.containsEntry("in.bndrun", "project");
			}
		}
	}

	@Test
	public void testPackagerDifference2() throws Exception {
		//
		// Next as we basically do it in bndtools for a file
		//
		File f = project.getFile("x.bndrun");
		try (Run run = new Run(project.getWorkspace(), project.getBase(), f);) {
			try (ProjectLauncher l = run.getProjectLauncher()) {
				l.setTrace(true);
				try (Jar executable = l.executable()) {
					assertNotNull(executable);

					Properties p = new Properties();
					Resource resource = executable.getResource("launcher.properties");

					try (InputStream in = resource.openInputStream()) {
						p.load(in);
					}

					assertThat(p).containsEntry("in.workspace", "workspace")
						.containsEntry("in.project", "workspace")
						.containsEntry("in.bndrun", "bndrun");
				}
			}
		}
	}

	@Test
	public void testPackagerDifference3() throws Exception {
		// Test project with export
		File f = new File(testDir, "test.jar");
		project.export(null, false, f);
		try (Jar executable = new Jar(f)) {
			Properties p = new Properties();
			Resource resource = executable.getResource("launcher.properties");

			try (InputStream in = resource.openInputStream()) {
				p.load(in);
			}

			assertThat(p).containsEntry("in.workspace", "workspace")
				.containsEntry("in.project", "project")
				.containsEntry("in.bndrun", "project");
		}
	}

	@Test
	public void testPackagerDifference4() throws Exception {
		// Test file with export
		File f = new File(testDir, "test.jar");
		project.export("x.bndrun", false, f);
		try (Jar executable = new Jar(f)) {
			Properties p = new Properties();
			Resource resource = executable.getResource("launcher.properties");

			try (InputStream in = resource.openInputStream()) {
				p.load(in);
			}

			assertThat(p).containsEntry("in.workspace", "workspace")
				.containsEntry("in.project", "workspace")
				.containsEntry("in.bndrun", "bndrun");
		}
	}

	/**
	 * junit 4 "unrooted" tests with parametrized tests #661
	 *
	 * @throws Exception
	 */
	@Test
	public void testJunit4Tester() throws Exception {
		ProjectTester pt = project.getProjectTester();
		pt.addTest("test.Junit4TestCase");

		assertThat(pt.test()).isEqualTo(0);
		assertTrue(project.check());
	}

	// public void testLocalLaunch() throws Exception {
	// Project project = getProject();
	// ProjectLauncher l = project.getProjectLauncher();
	// l.setTrace(true);
	// l.getRunProperties().put("test.cmd", "exit");
	// //assertTrue(project.check());
	// assertEquals(42, l.start(null));
	// }

	/**
	 * Test if we can keep the framework state.
	 */
	@Test
	public void testRunKeep() throws Exception {

		//
		// First set persistence after clearing the storage
		//
		project.setProperty("-runkeep", "false");
		project.setProperty("-runstorage", "generated/keepfw");
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTrace(true);
			l.getRunProperties()
				.put("test.cmd", "setpersistence");
			assertThat(l.launch()).isEqualTo(55);
		}

		//
		// Check that we really clear by clearing and checking state
		// this must fail with -2
		//
		project.setProperty("-runkeep", "false");
		project.setProperty("-runstorage", "generated/keepfw");
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTrace(true);
			l.getRunProperties()
				.put("test.cmd", "getpersistence");
			assertThat(l.launch()).isEqualTo(254);
		}

		//
		// We now try to set the state again with a cleared framework
		//
		project.setProperty("-runkeep", "false");
		project.setProperty("-runstorage", "generated/keepfw");
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTrace(true);
			l.getRunProperties()
				.put("test.cmd", "setpersistence");
			assertThat(l.launch()).isEqualTo(55);
		}

		//
		// And now it should have been saved if we do not clear
		// the framework
		//
		project.setProperty("-runkeep", "true");
		project.setProperty("-runstorage", "generated/keepfw");
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTrace(true);
			l.getRunProperties()
				.put("test.cmd", "getpersistence");
			assertThat(l.launch()).isEqualTo(65);
		}
	}

	@Test
	public void testNoReferences() throws Exception {
		project.setProperty("-runnoreferences", true + "");

		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTrace(true);
			l.getRunProperties()
				.put("test.cmd", "noreference");
			assertThat(l.launch()).isEqualTo(15);
		}
	}

	/**
	 * Try launching a workspace with spaces
	 */
	@Test
	public void testSpaces() throws Exception {
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTrace(true);
			l.getRunProperties()
				.put("test.cmd", "exit");
			assertThat(l.launch()).isEqualTo(42);
		}
	}

	/**
	 * Test the java agent
	 *
	 * @throws Exception
	 */
	@Test
	public void testAgent() throws Exception {
		project.setProperty("-javaagent", "true");
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTrace(true);
			l.getRunProperties()
				.put("test.cmd", "agent");
			assertThat(l.launch()).isEqualTo(55);
		}
	}

	/**
	 * Test env variables
	 *
	 * @throws Exception
	 */
	@Test
	public void testEnv() throws Exception {
		project.setProperty("-runenv", "ANSWER=84");
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTrace(true);
			l.getRunProperties()
				.put("test.cmd", "env");
			assertThat(l.launch()).isEqualTo(84);
		}
	}

	/**
	 * Tests if the properties are cleaned up. This requires some knowledge of
	 * the launcher unfortunately. It is also not sure if the file is not just
	 * deleted by the onExit ...
	 *
	 * @throws Exception
	 */
	@Test
	public void testCleanup() throws Exception {
		File target = project.getTarget();
		assertNoProperties(target);
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTrace(true);

			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(1000);
						l.cancel();
					} catch (Exception e) {
						// Ignore
					}
				}
			};

			t.start();
			l.getRunProperties()
				.put("test.cmd", "timeout");
			l.launch();
			assertNoProperties(target);
		}
	}

	/**
	 * The properties file is an implementation detail ... so this is white box
	 * testing.
	 *
	 * @param project
	 * @throws Exception
	 */
	private void assertNoProperties(File target) throws Exception {
		if (!target.exists())
			return;

		for (File file : target.listFiles()) {
			assertThat(file).as("There is a launch file in the target directory: %s", file)
				.matches(f -> !f.getName()
					.startsWith("launch"));
		}
	}

	@Test
	public void testSimple() throws Exception {
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTrace(true);
			l.getRunProperties()
				.put("test.cmd", "exit");
			assertThat(l.launch()).isEqualTo(42);
		}
	}

	/**
	 * Test the packager
	 *
	 * @throws Exception
	 */
	@Test
	public void testPackager() throws Exception {
		project.setProperty("[debug]testprop", "debug");
		project.setProperty("[exec]testprop", "exec");
		project.setProperty("Header", "${testprop}");
		project.setProperty(Constants.PROFILE, "exec");
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTrace(true);
			Jar executable = l.executable();
			assertNotNull(executable);
			assertEquals("exec", project.getProperty("testprop"));
			assertEquals("exec", project.getProperty("Header"));
		}
	}

	@Test
	public void testTester() throws Exception {
		ProjectTester pt = project.getProjectTester();
		pt.addTest("test.TestCase1");
		pt.addTest("test.TestCase2:m1");

		assertThat(pt.test()).isEqualTo(2);
	}

	@Test
	public void testTimeoutActivator() throws Exception {
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTimeout(100, TimeUnit.MILLISECONDS);
			l.setTrace(false);
			assertThat(l.launch()).isEqualTo(ProjectLauncher.TIMEDOUT);
		}

	}

	@Test
	public void testTimeout() throws Exception {
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTimeout(100, TimeUnit.MILLISECONDS);
			l.setTrace(false);
			l.getRunProperties()
				.put("test.cmd", "timeout");
			assertThat(l.launch()).isEqualTo(ProjectLauncher.TIMEDOUT);
		}
	}

	/**
	 * Allowing Runnable and Callable, with Callable as priority
	 *
	 * @throws Exception
	 */
	@Test
	public void testMainThread() throws Exception {
		assertExitCode("main.thread", ProjectLauncher.OK);
	}

	@Test
	public void testMainThreadBoth() throws Exception {
		assertExitCode("main.thread.both", 43);
	}

	@Test
	public void testMainThreadCallableNull() throws Exception {
		assertExitCode("main.thread.callablenull", 0);
	}

	@Test
	public void testMainThreadInvalidType() throws Exception {
		assertExitCode("main.thread.callableinvalidtype", 0);
	}

	@Test
	public void testMainThreadCallable() throws Exception {
		assertExitCode("main.thread.callable", 42);
	}

	@Test
	public void testFrameworkStop() throws Exception {
		assertExitCode("framework.stop", ProjectLauncher.STOPPED);
	}

	private void assertExitCode(String cmd, int rv) throws Exception {
		try (ProjectLauncher l = project.getProjectLauncher()) {
			l.setTimeout(25000, TimeUnit.MILLISECONDS);
			l.setTrace(true);
			l.getRunProperties()
				.put("test.cmd", cmd);
			assertThat(l.launch()).isEqualTo(rv);
		}
	}

	@Test
	public void testUnresolvedReporting() throws Exception {
		project.setProperty(Constants.RUNTRACE, "true");

		String runbundles = project.getProperty(Constants.RUNBUNDLES);
		project.setProperty(Constants.RUNBUNDLES,
			runbundles + "," + new File("jar/mandatorynoversion.jar;version=file").getAbsolutePath());
		ProjectTester tester = project.getProjectTester();
		try (ProjectLauncher l = tester.getProjectLauncher()) {
			l.setTimeout(25000, TimeUnit.MILLISECONDS);
			l.setTrace(true);
			AtomicBoolean reported = new AtomicBoolean(false);
			tester.registerForNotifications((a, b) -> {
				reported.set(true);
			});
			assertTrue(project.check());
			assertThat(tester.test()).isEqualTo(1);
			assertThat(reported).isTrue();
		}

	}

	/**
	 * I do not understand this test? It seems to wait 25 seconds and if it did
	 * not get an error through the notifier it is fine. Do we need this?
	 */
	// @Ignore("Just seems to wait for no obvious reason")
	@Test
	public void testFrameworkExtension() throws Exception {
		try (Run run = new Run(project.getWorkspace(), project.getFile("frameworkextension.bndrun"))) {
			run.setProperty(Constants.RUNTRACE, "true");
			ProjectTester tester = run.getProjectTester();
			try (ProjectLauncher l = tester.getProjectLauncher()) {
				AtomicReference<String> error = new AtomicReference<>();
				l.registerForNotifications((NotificationType type, final String notification) -> {
					if (type == NotificationType.ERROR) {
						error.set(notification);
					}
				});
				l.setTimeout(5000, TimeUnit.MILLISECONDS);
				l.setTrace(true);
				l.launch();

				assertThat(error).hasValue(null);
			}
		}
	}

	@Test
	public void testOlderLauncherOnRunpath() throws Exception {
		try (Run run = new Run(project.getWorkspace(), project.getFile("old-launcher.bndrun"))) {
			run.setProperty(Constants.RUNTRACE, "true");

			File file = new File(testDir, "packaged.jar");
			try (Jar pack = run.pack(null)) {
				assertTrue(run.check());
				pack.write(file);
			}

			System.setProperty("test.cmd", "quit.no.exit");
			String result = runFramework(file);
			assertThat(result).contains("installing jar/demo.jar");
		}
	}

	private String runFramework(File file) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
		InvocationTargetException, IOException, MalformedURLException {
		PrintStream out = System.err;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintStream out2 = new PrintStream(bout);
		System.setErr(out2);
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
			System.setErr(out);
		}

		return new String(bout.toByteArray(), StandardCharsets.UTF_8);
	}

}
