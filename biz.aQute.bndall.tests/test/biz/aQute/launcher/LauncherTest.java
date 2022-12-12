package biz.aQute.launcher;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

@ExtendWith(SoftAssertionsExtension.class)
public class LauncherTest {
	@InjectSoftAssertions
	SoftAssertions		softly;

	@InjectTemporaryDirectory
	File				testDir;

	private Properties	prior;

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
			.containsPattern("managed=all")
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
	public void testRunOrder_1_basic_manage_none() throws Exception {
		File file = buildPackage("order-01.bndrun", run -> {
			run.setProperty("-launcher", "manage=none");
		});

		System.setProperty("test.cmd", "quit.no.exit");

		String result = runFramework(file);

		softly.assertThat(result)
			.containsPattern("Startlevel\\s+22")
			.containsPattern("0\\s+ACTIV\\s+<>\\s+System Bundle")
			.containsPattern("1\\s+ACTIV\\s+<>\\s+jar/.?org.apache.felix.log")
			.containsPattern("1\\s+ACTIV\\s+<>\\s+jar/.?demo.jar")
			.containsPattern("1\\s+ACTIV\\s+<>\\s+jar/.?org.apache.servicemix.bundles.junit")
			.containsPattern("1\\s+ACTIV\\s+<>\\s+jar/.?org.apache.felix.configadmin")
			.containsPattern("startlevel: default=0, beginning=22")
			.containsPattern("startlevel: notified reached final level 22");
	}

	@Test
	public void testRunOrder_1_basic_manage_narrow() throws Exception {
		File file = buildPackage("order-01.bndrun", run -> {
			run.setProperty("-launcher", "manage=narrow");
		});

		System.setProperty("test.cmd", "quit.no.exit");

		String result = runFramework(file);

		softly.assertThat(result)
			.containsPattern("managed=narrow")
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
		return buildPackage(bndrun, x -> {});
	}

	private File buildPackage(String bndrun, Consumer<Run> decorate) throws Exception {
		File tgt = IO.copy(new File(bndrun), new File(testDir, bndrun));
		try (Workspace ws = new Workspace(new File("..")); Run run = Run.createRun(ws, tgt)) {
			decorate.accept(run);
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

	static class ArgumentRendering {
		Processor			p;
		Collection<String>	arguments;
		String[]			argumentsArray;
		TestInfo			info;

		@BeforeEach
		void beforeEach(TestInfo info) {
			this.info = info;
			p = new Processor();
			p.setProperty("-runvm.test1", "-Dfoo=\"xyz abc\"");
			p.setProperty("-runvm.test2",
				"-Dbar=xyz abc, -Dlauncher.properties=C:\\Users\\bj hargrave\\git\\bnd\\bndtools.core\\generated\\launch7354374140259840283.properties");
			Parameters hdr = p.getMergedParameters(Constants.RUNVM);
			arguments = hdr.keyList();
			argumentsArray = arguments.stream()
				// .flatMap(argument -> new QuotedTokenizer(argument, " \t",
				// false, true).stream()
				// .filter(
				// Strings::notEmpty))
				.toArray(String[]::new);
		}

		@AfterEach
		void afterEach() {
			IO.close(p);
		}

		void dump(String rendered, String[] roundTrip) {
			System.out.printf("%s\ninput:\n%s\nrendered:\n«%s»\nroundTrip:\n%s\n\n", info.getDisplayName(),
				arguments.stream()
					.collect(joining("»,\n «", " «", "»")),
				rendered, Arrays.stream(roundTrip)
					.collect(joining("»,\n «", " «", "»")));
		}

		@Test
		void testArgumentRendering() throws Exception {
			String rendered = ProjectLauncher.renderArguments(argumentsArray);
			String[] roundTrip = parseArguments(rendered);
			// dump(rendered, roundTrip);
			assertThat(roundTrip).containsExactly(argumentsArray);
		}

		@Test
		void testArgumentRendering_forUnix() throws Exception {
			String rendered = ProjectLauncher.renderArguments(argumentsArray, false);
			String[] roundTrip = parseArgumentsImpl(rendered, false);
			// dump(rendered, roundTrip);
			assertThat(roundTrip).containsExactly(argumentsArray);
			assertThat(rendered).as("shouldn't un-escape backslash")
				.contains("\\\\");
		}

		@Test
		void testArgumentRendering_forWindows() throws Exception {
			String rendered = ProjectLauncher.renderArguments(argumentsArray, true);
			String[] roundTrip = parseArgumentsWindows(rendered, false);
			// dump(rendered, roundTrip);
			assertThat(roundTrip).containsExactly(argumentsArray);
			assertThat(rendered).as("should un-escape backslash")
				.doesNotContain("\\\\");
		}

		// The following were copied from org.eclipse.debug.core.DebugPlugin for
		// use in this test

		public static String[] parseArguments(String args) {
			if (args == null) {
				return new String[0];
			}

			if (IO.isWindows()) {
				return parseArgumentsWindows(args, false);
			}

			return parseArgumentsImpl(args, false);
		}

		static String[] parseArgumentsImpl(String args, boolean split) {
			// man sh, see topic QUOTING
			List<String> result = new ArrayList<>();

			final int DEFAULT = 0;
			final int ARG = 1;
			final int IN_DOUBLE_QUOTE = 2;
			final int IN_SINGLE_QUOTE = 3;

			int state = DEFAULT;
			StringBuilder buf = new StringBuilder();
			int len = args.length();
			for (int i = 0; i < len; i++) {
				char ch = args.charAt(i);
				if (Character.isWhitespace(ch)) {
					if (state == DEFAULT) {
						// skip
						continue;
					} else if (state == ARG) {
						state = DEFAULT;
						result.add(buf.toString());
						buf.setLength(0);
						continue;
					}
				}
				switch (state) {
					case DEFAULT :
					case ARG :
						if (ch == '"') {
							if (split) {
								buf.append(ch);
							}
							state = IN_DOUBLE_QUOTE;
						} else if (ch == '\'') {
							if (split) {
								buf.append(ch);
							}
							state = IN_SINGLE_QUOTE;
						} else if (ch == '\\' && i + 1 < len) {
							if (split) {
								buf.append(ch);
							}
							state = ARG;
							ch = args.charAt(++i);
							buf.append(ch);
						} else {
							state = ARG;
							buf.append(ch);
						}
						break;

					case IN_DOUBLE_QUOTE :
						if (ch == '"') {
							if (split) {
								buf.append(ch);
							}
							state = ARG;
						} else if (ch == '\\' && i + 1 < len
							&& (args.charAt(i + 1) == '\\' || args.charAt(i + 1) == '"')) {
							if (split) {
								buf.append(ch);
							}
							ch = args.charAt(++i);
							buf.append(ch);
						} else {
							buf.append(ch);
						}
						break;

					case IN_SINGLE_QUOTE :
						if (ch == '\'') {
							if (split) {
								buf.append(ch);
							}
							state = ARG;
						} else {
							buf.append(ch);
						}
						break;

					default :
						throw new IllegalStateException();
				}
			}
			if (buf.length() > 0 || state != DEFAULT) {
				result.add(buf.toString());
			}

			return result.toArray(new String[result.size()]);
		}

		static String[] parseArgumentsWindows(String args, boolean split) {
			// see http://msdn.microsoft.com/en-us/library/a1y7w461.aspx
			List<String> result = new ArrayList<>();

			final int DEFAULT = 0;
			final int ARG = 1;
			final int IN_DOUBLE_QUOTE = 2;

			int state = DEFAULT;
			int backslashes = 0;
			StringBuilder buf = new StringBuilder();
			int len = args.length();
			for (int i = 0; i < len; i++) {
				char ch = args.charAt(i);
				if (ch == '\\') {
					backslashes++;
					continue;
				} else if (backslashes != 0) {
					if (ch == '"') {
						for (; backslashes >= 2; backslashes -= 2) {
							buf.append('\\');
							if (split) {
								buf.append('\\');
							}
						}
						if (backslashes == 1) {
							if (state == DEFAULT) {
								state = ARG;
							}
							if (split) {
								buf.append('\\');
							}
							buf.append('"');
							backslashes = 0;
							continue;
						} // else fall through to switch
					} else {
						// false alarm, treat passed backslashes literally...
						if (state == DEFAULT) {
							state = ARG;
						}
						for (; backslashes > 0; backslashes--) {
							buf.append('\\');
						}
						// fall through to switch
					}
				}
				if (Character.isWhitespace(ch)) {
					if (state == DEFAULT) {
						// skip
						continue;
					} else if (state == ARG) {
						state = DEFAULT;
						result.add(buf.toString());
						buf.setLength(0);
						continue;
					}
				}
				switch (state) {
					case DEFAULT :
					case ARG :
						if (ch == '"') {
							state = IN_DOUBLE_QUOTE;
							if (split) {
								buf.append(ch);
							}
						} else {
							state = ARG;
							buf.append(ch);
						}
						break;

					case IN_DOUBLE_QUOTE :
						if (ch == '"') {
							if (i + 1 < len && args.charAt(i + 1) == '"') {
								/*
								 * Undocumented feature in Windows: Two
								 * consecutive double quotes inside a
								 * double-quoted argument are interpreted as a
								 * single double quote.
								 */
								buf.append('"');
								i++;
								if (split) {
									buf.append(ch);
								}
							} else if (buf.length() == 0) {
								// empty string on Windows platform. Account for
								// bug
								// in constructor of JDK's
								// java.lang.ProcessImpl.
								result.add("\"\""); //$NON-NLS-1$
								state = DEFAULT;
							} else {
								state = ARG;
								if (split) {
									buf.append(ch);
								}
							}
						} else {
							buf.append(ch);
						}
						break;

					default :
						throw new IllegalStateException();
				}
			}
			if (buf.length() > 0 || state != DEFAULT) {
				result.add(buf.toString());
			}

			return result.toArray(new String[result.size()]);
		}
	}
}
