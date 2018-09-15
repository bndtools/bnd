package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.assertj.core.util.Files;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.Strategy;
import aQute.lib.io.IO;
import aQute.libg.command.Command;
import junit.framework.TestCase;

public class LauncherTest extends TestCase {

	private Workspace	workspace;
	private Project		project;

	@Override
	public void tearDown() throws IOException {
		if (project != null) {
			project.close();
			workspace.close();
		}
	}

	public void testExecutableJarWithStripping() throws Exception {
		Project project = getProject();

		long full = make(project, null);
		long optStripped = make(project, "strip='OSGI-OPT/*'");
		long optStrippedAndNoBndrun = make(project, "strip='OSGI-OPT/*,*.bndrun'");
		long optNoBndrun = make(project, "strip='*.bndrun'");

		assertThat(full > optStripped).isTrue();
		assertThat(optStripped > optStrippedAndNoBndrun).isTrue();
		assertThat(optStrippedAndNoBndrun < optNoBndrun).isTrue();

	}

	private long make(Project p, String option) throws Exception {
		ProjectLauncher l = project.getProjectLauncher();
		if (option != null)
			p.setProperty(Constants.EXECUTABLE, option);
		try (Jar executable = l.executable()) {
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

	public void testExecutableWithRejarringAndStripping() throws Exception {

		long storedStored = makeExec(false, false, false);
		long storedDeflate = makeExec(false, false, true);
		long deflateDeflate = makeExec(false, true, true);
		long deflateStored = makeExec(false, true, false);
		long stripStoredStored = makeExec(true, false, false);
		long stripStoredDeflate = makeExec(true, false, true);
		long stripDeflateDeflate = makeExec(true, true, true);
		long stripDeflateStored = makeExec(true, true, false);

		assertTrue(deflateStored < deflateDeflate);
		assertTrue(deflateDeflate < storedDeflate);
		assertTrue(storedDeflate < storedStored);

		assertTrue(stripStoredStored < storedStored);
		assertTrue(stripStoredDeflate < storedDeflate);
		assertTrue(stripDeflateDeflate < deflateDeflate);
		assertTrue(stripDeflateStored < deflateStored);
	}

	private long makeExec(boolean strip, boolean outer, boolean inner) throws Exception {
		Project project = getProject();
		project.setProperty(Constants.RUNPROPERTIES, "test.cmd=exit");
		project.setProperty(Constants.RUNTRACE, "false");
		ProjectLauncher l = project.getProjectLauncher();
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

		try (Jar executable = l.executable()) {
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

	public void testExpandedJarLauncher() throws Exception {
		Project project = getProject();
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
	public void testRemotePackager() throws Exception {
		Project project = getProject();
		project.clear();
		project.setProperty("-runpath", "biz.aQute.remote.launcher;version=latest");
		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		Jar executable = l.executable();
		assertTrue(project.check());
		assertNotNull(executable);
	}

	/**
	 * Try out the new tester that does not contain JUnit
	 */
	public void testJUnitLessTester() throws Exception {
		Project project = getProject();

		List<Container> bundles = project.getBundles(Strategy.HIGHEST, "biz.aQute.tester", "TESTER");
		assertNotNull(bundles);
		assertEquals(1, bundles.size());

		project.setProperty(Constants.TESTPATH, "");
		project.setProperty(Constants.TESTER, "biz.aQute.tester");
		project.clear();
		project.build();
		assertTrue(project.check());

		ProjectTester pt = project.getProjectTester();
		pt.addTest("test.TestCase1");
		pt.addTest("test.TestCase2:m1");

		assertEquals(2, pt.test());
	}

	/**
	 * Gradle Problems exporting an executable jar #980 Test the packager's
	 * difference between plan export in gradle & from bndtools
	 * 
	 * @throws Exception
	 */
	public void testPackagerDifference() throws Exception {

		//
		// First as we basically do it in bndtools for a project
		//

		{
			Project project = getProject();
			project.clear();
			ProjectLauncher l = project.getProjectLauncher();
			l.setTrace(true);
			Jar executable = l.executable();
			assertNotNull(executable);

			Properties p = new Properties();
			Resource resource = executable.getResource("launcher.properties");

			try (InputStream in = resource.openInputStream()) {
				p.load(in);
			}

			assertEquals("workspace", p.getProperty("in.workspace"));
			assertEquals("project", p.getProperty("in.project"));
			assertEquals("project", p.getProperty("in.bndrun"));
		}

		//
		// First as we basically do it in bndtools for a file
		//

		{
			Project project = getProject();
			project.clear();
			File f = project.getFile("x.bndrun");
			try (Run run = new Run(project.getWorkspace(), project.getBase(), f);) {

				ProjectLauncher l = run.getProjectLauncher();
				l.setTrace(true);
				Jar executable = l.executable();
				assertNotNull(executable);

				Properties p = new Properties();
				Resource resource = executable.getResource("launcher.properties");

				try (InputStream in = resource.openInputStream()) {
					p.load(in);
				}

				assertEquals("workspace", p.getProperty("in.workspace"));
				assertEquals("workspace", p.getProperty("in.project"));
				assertEquals("bndrun", p.getProperty("in.bndrun"));
			}
		}

		// Test project with export

		{

			Project project = getProject();
			project.clear();
			File f = new File("generated/test.jar");
			project.export(null, false, f);

			try (Jar executable = new Jar(f);) {

				Properties p = new Properties();
				Resource resource = executable.getResource("launcher.properties");

				try (InputStream in = resource.openInputStream()) {
					p.load(in);
				}

				assertEquals("workspace", p.getProperty("in.workspace"));
				assertEquals("project", p.getProperty("in.project"));
				assertEquals("project", p.getProperty("in.bndrun"));
			}
		}

		// Test file with export

		{

			Project project = getProject();
			project.clear();
			File f = new File("generated/test.jar");
			project.export("x.bndrun", false, f);

			try (Jar executable = new Jar(f);) {

				Properties p = new Properties();
				Resource resource = executable.getResource("launcher.properties");

				try (InputStream in = resource.openInputStream()) {
					p.load(in);
				}

				assertEquals("workspace", p.getProperty("in.workspace"));
				assertEquals("workspace", p.getProperty("in.project"));
				assertEquals("bndrun", p.getProperty("in.bndrun"));
			}
		}

	}

	/**
	 * junit 4 "unrooted" tests with parametrized tests #661
	 * 
	 * @throws Exception
	 */
	public void testJunit4Tester() throws Exception {
		Project project = getProject();
		project.clear();
		project.build();

		ProjectTester pt = project.getProjectTester();
		pt.addTest("test.Junit4TestCase");

		assertEquals(0, pt.test());
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
	public void testRunKeep() throws Exception {

		//
		// First set persistence after clearing the storage
		//
		Project project = getProject();
		project.setProperty("-runkeep", "false");
		project.setProperty("-runstorage", "generated/keepfw");
		ProjectLauncher l = project.getProjectLauncher();

		l.setTrace(true);
		l.getRunProperties()
			.put("test.cmd", "setpersistence");
		assertEquals(55, l.launch());

		//
		// Check that we really clear by clearing and checking state
		// this must fail with -2
		//

		project = getProject();
		project.setProperty("-runkeep", "false");
		project.setProperty("-runstorage", "generated/keepfw");
		l = project.getProjectLauncher();

		l.setTrace(true);
		l.getRunProperties()
			.put("test.cmd", "getpersistence");
		assertEquals(254, l.launch());

		//
		// We now try to set the state again with a cleared framework
		//
		project = getProject();
		project.setProperty("-runkeep", "false");
		project.setProperty("-runstorage", "generated/keepfw");
		l = project.getProjectLauncher();

		l.setTrace(true);
		l.getRunProperties()
			.put("test.cmd", "setpersistence");
		assertEquals(55, l.launch());

		//
		// And now it should have been saved if we do not clear
		// the framework
		//

		project = getProject();
		project.setProperty("-runkeep", "true");
		project.setProperty("-runstorage", "generated/keepfw");
		l = project.getProjectLauncher();

		l.setTrace(true);
		l.getRunProperties()
			.put("test.cmd", "getpersistence");
		assertEquals(65, l.launch());

	}

	public void testNoReferences() throws Exception {
		Project project = getProject();
		project.setProperty("-runnoreferences", true + "");

		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		l.getRunProperties()
			.put("test.cmd", "noreference");
		assertEquals(15, l.launch());
	}

	/**
	 * Try launching a workspace with spaces
	 */
	public void testSpaces() throws Exception {
		File f = new File("t m p");
		try {
			File cnf = new File(f, "cnf");
			File demo = new File(f, "demo");
			IO.copy(IO.getFile("../cnf"), IO.getFile(f, "cnf"));
			IO.copy(IO.getFile("../demo"), IO.getFile(f, "demo"));
			IO.copy(IO.getFile("../biz.aQute.launcher"), IO.getFile(f, "biz.aQute.launcher"));
			IO.copy(IO.getFile("../biz.aQute.junit"), IO.getFile(f, "biz.aQute.junit"));

			Workspace ws = Workspace.getWorkspace(f);
			Project p = ws.getProject("demo");
			p.setTrace(true);
			p.build();
			try {
				ProjectLauncher l = p.getProjectLauncher();
				l.setTrace(true);
				l.getRunProperties()
					.put("test.cmd", "exit");
				assertEquals(42, l.launch());
			} finally {
				p.close();
				ws.close();
			}
		} finally {
			IO.delete(f);
		}
	}

	/**
	 * Test the java agent
	 * 
	 * @throws Exception
	 */
	public void testAgent() throws Exception {
		Project project = getProject();
		project.clear();
		project.setProperty("-javaagent", "true");
		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		l.getRunProperties()
			.put("test.cmd", "agent");
		assertEquals(55, l.launch());
	}

	/**
	 * Test env variables
	 * 
	 * @throws Exception
	 */
	public void testEnv() throws Exception {
		Project project = getProject();
		project.clear();
		project.setProperty("-runenv", "ANSWER=84");
		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		l.getRunProperties()
			.put("test.cmd", "env");
		assertEquals(84, l.launch());
	}

	/**
	 * Tests if the properties are cleaned up. This requires some knowledge of
	 * the launcher unfortunately. It is also not sure if the file is not just
	 * deleted by the onExit ...
	 * 
	 * @throws Exception
	 */
	public void testCleanup() throws Exception {
		Project project = getProject();
		File target = project.getTarget();
		IO.deleteWithException(target);
		project.clear();
		assertNoProperties(target);
		final ProjectLauncher l = project.getProjectLauncher();
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
			if (file.getAbsolutePath()
				.startsWith("launch")) {
				fail("There is a launch file in the target directory: " + file);
			}
		}
	}

	public void testSimple() throws Exception {
		Project project = getProject();
		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		l.getRunProperties()
			.put("test.cmd", "exit");
		// assertTrue(project.check());
		assertEquals(42, l.launch());
	}

	/**
	 * Test the packager
	 * 
	 * @throws Exception
	 */
	public void testPackager() throws Exception {
		Project project = getProject();
		project.clear();
		project.setProperty("[debug]testprop", "debug");
		project.setProperty("[exec]testprop", "exec");
		project.setProperty("Header", "${testprop}");
		project.setProperty(Constants.PROFILE, "exec");
		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		Jar executable = l.executable();
		assertNotNull(executable);
		assertEquals("exec", project.getProperty("testprop"));
		assertEquals("exec", project.getProperty("Header"));
	}

	/**
	 * This needs to be adapted because the previous left lots of files after
	 * testing. This current one does not work since the demo project uses the
	 * snapshots of the launcher and tester, and when copied they are not there
	 * in that workspace. So we need another demo project that does not use OSGi
	 * and has not special deps. Then the following code can be used.
	 * 
	 * @throws Exception
	 */
	// public void testWorkspaceWithSpace() throws Exception {
	// // reuse built .class files from the demo project.
	// String base = new
	// File("").getAbsoluteFile().getParentFile().getAbsolutePath();
	// File ws = IO.getFile("tmp/ space ");
	// try {
	// IO.delete(ws);
	// ws.mkdirs();
	// IO.copy( IO.getFile("../demo"), IO.getFile(ws, "demo"));
	// IO.getFile(ws, "cnf").mkdirs();
	// IO.copy( IO.getFile("../cnf"), IO.getFile(ws, "cnf"));
	// Workspace wp = new Workspace(ws);
	//
	// Project p = wp.getProject("demo");
	// p.clear();
	// ProjectLauncher l = p.getProjectLauncher();
	// l.setTrace(true);
	// l.getRunProperties().put("test.cmd", "exit");
	// assertEquals(42, l.launch());
	// }
	// finally {
	// IO.delete(ws);
	// }
	// }

	/**
	 * @throws Exception
	 */
	Project getProject() throws Exception {
		workspace = Workspace.getWorkspace(new File("").getAbsoluteFile()
			.getParentFile());
		workspace.clear();
		project = workspace.getProject("demo");
		project.setTrace(true);
		project.clear();
		project.forceRefresh();
		assertTrue(project.check());
		// (project.getWorkspace().check());
		return project;
	}

	public void testTester() throws Exception {
		Project project = getProject();
		project.clear();
		project.build();

		ProjectTester pt = project.getProjectTester();
		pt.addTest("test.TestCase1");
		pt.addTest("test.TestCase2:m1");

		assertEquals(2, pt.test());
	}

	public void testTimeoutActivator() throws Exception {
		Project project = getProject();
		project.clear();

		ProjectLauncher l = project.getProjectLauncher();
		l.setTimeout(100, TimeUnit.MILLISECONDS);
		l.setTrace(false);
		assertEquals(ProjectLauncher.TIMEDOUT, l.launch());

	}

	public void testTimeout() throws Exception {
		Project project = getProject();
		project.clear();

		ProjectLauncher l = project.getProjectLauncher();
		l.setTimeout(100, TimeUnit.MILLISECONDS);
		l.setTrace(false);
		l.getRunProperties()
			.put("test.cmd", "timeout");
		assertEquals(ProjectLauncher.TIMEDOUT, l.launch());
	}

	/**
	 * Allowing Runnable and Callable, with Callable as priority
	 * 
	 * @throws Exception
	 */
	public void testMainThread() throws Exception {
		assertExitCode("main.thread", ProjectLauncher.OK);
	}

	public void testMainThreadBoth() throws Exception {
		assertExitCode("main.thread.both", 43);
	}

	public void testMainThreadCallableNull() throws Exception {
		assertExitCode("main.thread.callablenull", 0);
	}

	public void testMainThreadInvalidType() throws Exception {
		assertExitCode("main.thread.callableinvalidtype", 0);
	}

	public void testMainThreadCallable() throws Exception {
		assertExitCode("main.thread.callable", 42);
	}

	public void testFrameworkStop() throws Exception {
		assertExitCode("framework.stop", ProjectLauncher.STOPPED);
	}

	private void assertExitCode(String cmd, int rv) throws Exception {
		Project project = getProject();
		project.clear();

		ProjectLauncher l = project.getProjectLauncher();
		l.setTimeout(15000, TimeUnit.MILLISECONDS);
		l.setTrace(true);
		l.getRunProperties()
			.put("test.cmd", cmd);
		assertEquals(rv, l.launch());
	}

	public void testUnresolved() throws Exception {
		Project project = getProject();
		project.clear();
		project.setProperty(Constants.RUNTRACE, "true");

		String mandatorynoversion = IO.getFile("jar/mandatorynoversion.jar")
			.getAbsolutePath();
		String runbundles = project.getProperty(Constants.RUNBUNDLES);
		project.setProperty(Constants.RUNBUNDLES, runbundles + "," + mandatorynoversion + ";version=file");
		ProjectTester tester = project.getProjectTester();
		tester.prepare();
		ProjectLauncher l = tester.getProjectLauncher();
		l.addRunBundle(mandatorynoversion);
		l.setTimeout(25000, TimeUnit.MILLISECONDS);
		l.setTrace(true);
		assertEquals(1, l.launch());
	}
}
