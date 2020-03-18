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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.assertj.core.api.AutoCloseableSoftAssertions;
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
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.Strategy;
import aQute.lib.io.IO;
import aQute.libg.command.Command;
import biz.aQute.resolve.Bndrun;

public class AlsoLauncherTest {
	public static final String	TMPDIR		= "generated/tmp/test";
	@Rule
	public final TestName		testName	= new TestName();
	private File				testDir;

	private Workspace			workspace;
	private Project				project;
	private Properties			prior;

	@Before
	public void setUp() throws Exception {
		testDir = new File(TMPDIR, getClass().getName() + "/" + testName.getMethodName());
		IO.delete(testDir);
		IO.mkdirs(testDir);
		prior = new Properties();
		prior.putAll(System.getProperties());

		File wsRoot = new File(testDir, "test ws");
		for (String folder : Arrays.asList("cnf", "demo", "biz.aQute.launcher", "biz.aQute.junit", "biz.aQute.tester",
			"biz.aQute.tester.junit-platform")) {
			File tgt = new File(wsRoot, folder);
			IO.copy(new File("..", folder), tgt);
			IO.delete(new File(tgt, "generated/buildfiles"));
		}
		IO.delete(new File(wsRoot, "cnf/cache"));
		workspace = new Workspace(wsRoot);
		workspace.setProperty(Constants.PROFILE, "prod");
		project = workspace.getProject("demo");
		project.setTrace(true);
		assertTrue(project.check());
	}

	@SuppressWarnings("restriction")
	@After
	public void tearDown() throws Exception {
		IO.close(project);
		IO.close(workspace);
		System.setProperties(prior);
	}

	@Test
	public void testLocationFormat() throws Exception {
		project.setProperty(Constants.RUNPROPERTIES, "test.cmd=exit");
		project.setProperty(Constants.RUNTRACE, "true");
		project.setProperty("-executable", "location='${@bsn}'");
		Entry<String, Resource> export = project.export("bnd.executablejar", null);
		assertThat(export).isNotNull();

		try (Jar jar = new Jar(".", export.getValue()
			.openInputStream())) {

			assertThat(jar.getResources()
				.keySet()).contains(//
					"jar/biz.aQute.launcher.jar", // -runpath
					"jar/org.apache.felix.framework-5.6.10.jar", // -runpath
					"jar/apiguardian-api-1.1.0.jar", // not a bundle
					"jar/demo", //
					// the following were not a bundle yet
					// "jar/junit-jupiter-api", //
					// "jar/junit-jupiter-engine", //
					// "jar/junit-jupiter-params", //
					// "jar/junit-platform-commons", //
					// "jar/junit-platform-engine", //
					// "jar/junit-vintage-engine", //
					"jar/org.apache.felix.configadmin", //
					"jar/org.apache.felix.scr", //
					"jar/org.apache.servicemix.bundles.junit", //
					"jar/org.opentest4j" //
			);

			File tmp = File.createTempFile("foo", ".jar");
			try {

				jar.write(tmp);
				Command cmd = new Command();
				cmd.add(project.getJavaExecutable("java"));
				cmd.add("-jar");
				cmd.add(tmp.getAbsolutePath());

				StringBuilder stdout = new StringBuilder();
				StringBuilder stderr = new StringBuilder();
				int execute = cmd.execute(stdout, stderr);
				String output = stdout.append(stderr)
					.toString();
				System.out.println(output);

				// These must be bsns ow
				assertThat(output).contains("installing jar/org.apache.felix.scr",
					"installing jar/org.apache.felix.configadmin");

				assertThat(execute).isEqualTo(42);

			} finally {
				tmp.delete();
			}
		}
	}

	/**
	 * Test that the Bndrun file is loaded when we create a run
	 *
	 */
	@Test
	public void testCreateBndrun() throws Exception {
		Run run = Run.createRun(workspace, project.getFile("x.bndrun"));

		assertThat(run).isInstanceOf(Bndrun.class);
	}

	@Test
	public void testExportWithIncludedBundlesInBndrun() throws Exception {
		project.setProperty("-resourceonly", "true");
		project.setProperty("-includeresource", "hello;literal=true");

		project.setProperty("-export", "a.bndrun;output=a.bndrun");
		IO.store("-includeresource: generated/demo.jar", project.getFile("a.bndrun"));
		File file = project.getFile("generated/demo.jar");
		file.delete();
		assertThat(file).doesNotExist();

		File[] build = project.build();
		assertThat(project.check()).isTrue();
		assertThat(build).hasSize(2);
		assertThat(build[0]).isEqualTo(file);

		File run = project.getFile("generated/a.bndrun.jar");
		assertThat(build[1]).isEqualTo(run);

	}

	@Test
	public void testExportOptions() throws Exception {
		project.setProperty("-resourceonly", "true");
		project.setProperty("-includeresource", "hello;literal=true");

		project.setProperty("-export", //
			"x.bndrun, " //
				+ "x.bndrun;duplicate=true," //
				+ "x.bndrun;name=aa.jar, " //
				+ "x.bndrun;name=bsn.jar;bsn=foo.bar;version=1.2.3, " //
				+ "x.bndrun;type=bnd.runbundles;name=runbundles.jar," //
				+ "x.bndrun;name=foo.jar;Foo=aaa," //
				+ "export-options.bndrun;-profile=debug;type=bnd.executablejar.pack," //
				+ "export-options.bndrun;type=bnd.executablejar.pack;name=export-options-prod.jar");

		project.setProperty(Constants.FIXUPMESSAGES, "Duplicate file in -export;is:=warning");

		File[] build = project.build();


		assertThat(project.check("Duplicate file in -export")).isTrue();
		assertThat(build).hasSize(8);

		assertThat(build[0].getName()).isEqualTo("demo.jar");
		assertThat(build[1].getName()).isEqualTo("x.bndrun.jar");
		assertThat(build[2].getName()).isEqualTo("aa.jar");
		assertThat(build[3].getName()).isEqualTo("bsn.jar");
		assertThat(build[4].getName()).isEqualTo("runbundles.jar");
		assertThat(build[5].getName()).isEqualTo("foo.jar");
		assertThat(build[6].getName()).isEqualTo("export-options.jar");
		assertThat(build[7].getName()).isEqualTo("export-options-prod.jar");

		File demo = project.getFile("generated/demo.jar");
		Domain domain = Domain.domain(demo);
		assertThat(domain.get(Constants.BUNDLE_SYMBOLICNAME)).isEqualTo("demo");

		File runbundles = project.getFile("generated/runbundles.jar");
		domain = Domain.domain(runbundles);
		assertThat(domain).isNull();

		File bsn = project.getFile("generated/bsn.jar");
		domain = Domain.domain(bsn);
		assertThat(domain.get(Constants.BUNDLE_SYMBOLICNAME)).isEqualTo("foo.bar");
		assertThat(domain.get(Constants.BUNDLE_VERSION)).isEqualTo("1.2.3");

		File withFoo = project.getFile("generated/foo.jar");
		domain = Domain.domain(withFoo);
		assertThat(domain.get("Foo")).isEqualTo("aaa");

		File profile = project.getFile("generated/export-options.jar");
		domain = Domain.domain(profile);
		assertThat(domain.get("Bar")
			.trim()).isEqualTo("DEBUG");

		File profileProd = project.getFile("generated/export-options-prod.jar");
		domain = Domain.domain(profileProd);
		assertThat(domain.get("Bar")
			.trim()).isEqualTo("PROD");

		File run = project.getFile("generated/x.bndrun.jar");
		assertThat(build[1]).isEqualTo(run);

	}

	@Test
	public void testSelfExportError() throws Exception {
		project.setProperty("-resourceonly", "true");
		project.setProperty("-includeresource", "hello;literal=true");

		project.setProperty("-export", //
			"bnd.bnd");

		File[] build = project.build();

		assertThat(project.check("Cannot export the same file that contains the -export instruction")).isTrue();
	}

	@Test
	public void testExportWithRunKeep() throws Exception {
		project.setProperty("-runkeep", "true");
		Entry<String, Resource> export = project.export("bnd.executablejar", null);
		assertThat(project.check()).isTrue();
		assertThat(export).isNotNull();

		try (Jar jar = new Jar(".", export.getValue()
			.openInputStream())) {
			Resource resource = jar.getResource("launcher.properties");
			assertThat(resource).isNotNull();

			Properties p = new Properties();
			p.load(resource.openInputStream());

			assertThat(p.getProperty("launch.keep")).isEqualTo("true");

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
				cmd.add(l.getJavaExecutable("java"));
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

	@SuppressWarnings("deprecation")
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

	@SuppressWarnings("deprecation")
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

		for (File f : target.listFiles()) {
			if (f.getName()
				.startsWith("launch"))
				f.delete();
		}

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

	@Test
	public void testReporting_notUsingReferences() throws Exception {
		try (ProjectLauncher l = project.getProjectLauncher()) {
			project.setProperty("-runnoreferences", "true");
			doTestReporting(l);
		}
	}

	@Test
	public void testReporting_usingReferences() throws Exception {
		try (ProjectLauncher l = project.getProjectLauncher()) {
			project.setProperty("-runnoreferences", "false");
			doTestReporting(l);
		}
	}

	private void doTestReporting(ProjectLauncher l) throws Exception {
		l.setTrace(true);
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();
		l.setStreams(out, err);
		l.getRunProperties()
			.put("test.cmd", "framework.stop");

		try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
			Macro r = project.getReplacer();

			String[] expected = {
				"org.apache.felix.scr", "org.apache.servicemix.bundles.junit", "org.assertj.core", "demo"
			};

			l.launch();

			// High-level test; cuts down our search space for subsequent tests
			// which makes the output more readable in the event of a subsequent
			// assertion failure.
			Matcher match = Pattern.compile("^Id\\s+\\s+Levl\\s+State.*?(?=^#)", Pattern.DOTALL | Pattern.MULTILINE)
				.matcher(err);
			assertThat(match.find()).as("report block")
				.isTrue();
			final String report = match.group();

			softly.assertThat(report)
				.as("System Bundle")
				.containsPattern("\\n0\\s+\\d+\\s*ACTIV\\s+[<][>]\\s+System Bundle");

			final DateTimeFormatter f = DateTimeFormatter.ofPattern("YYYYMMddHHmm")
				.withZone(ZoneOffset.UTC);

			for (String bundle : expected) {
				final Path file = Paths.get(r.process("${repo;" + bundle + ";latest}"));
				final String lastModified = f.format(java.nio.file.Files.getLastModifiedTime(file)
					.toInstant());

				softly.assertThat(report)
					.as(bundle)
					.containsPattern("ACTIV\\s+" + lastModified + "\\s+.*?\\Q" + file.getFileName() + "\\E");
			}
		}
	}

	/**
	 * The properties file is an implementation detail ... so this is white box
	 * testing.
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
	public void testTesterQuoted() throws Exception {
		ProjectTester pt = project.getProjectTester();
		pt.addTest("'test.Junit4TestCase'");
		pt.addTest("\"test.TestCase2:m1\"");
		pt.addTest("\"test.TestCase2:m2\"");
		assertThat(pt.getTests()).containsExactly("test.Junit4TestCase", "test.TestCase2:m1", "test.TestCase2:m2");
		assertThat(pt.test()).isEqualTo(1);
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
