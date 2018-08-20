package aQute.bnd.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Jar;
import junit.framework.TestCase;

public class TestBndMain extends TestCase {

	private final ByteArrayOutputStream	capturedStdOut	= new ByteArrayOutputStream();
	private PrintStream					originalStdOut;

	private final ByteArrayOutputStream	capturedStdErr	= new ByteArrayOutputStream();
	private PrintStream					originalStdErr;
	private String						version;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		version = About.CURRENT.getWithoutQualifier()
			.toString();

		capturedStdOut.reset();
		originalStdOut = System.out;
		System.setOut(new PrintStream(capturedStdOut));

		capturedStdErr.reset();
		originalStdErr = System.err;
		System.setErr(new PrintStream(capturedStdErr));
	}

	@Override
	protected void tearDown() throws Exception {
		System.setErr(originalStdErr);
		System.setOut(originalStdOut);
		super.tearDown();
	}

	public void testRunStandalone() throws Exception {
		bnd.mainNoExit(new String[] {
			"run", "testdata/standalone/standalone.bndrun"
		});
		expectNoError();
		expectOutput("Gesundheit!");
	}

	public void testRunWorkspace() throws Exception {
		bnd.mainNoExit(new String[] {
			"run", "testdata/workspace/p/workspace.bndrun"
		});
		expectNoError();
		expectOutput("Gesundheit!");
	}

	public void testPackageBndrunStandalone() throws Exception {
		String output = "generated/tmp/export-standalone.jar";
		bnd.mainNoExit(new String[] {
			"package", "-o", output, "testdata/standalone/standalone.bndrun"
		});
		expectNoError();

		// validate exported jar content
		try (Jar result = new Jar(new File(output))) {
			expectJarEntry(result, "jar/biz.aQute.launcher-" + version + ".jar");
			expectJarEntry(result, "jar/org.apache.felix.framework-5.6.10.jar");
			expectJarEntry(result, "jar/printAndExit-1.0.0.jar");
		}
	}

	public void testPackageBndrunWorkspace() throws Exception {
		String output = "generated/tmp/export-workspace.jar";
		bnd.mainNoExit(new String[] {
			"package", "-o", output, "testdata/workspace/p/workspace.bndrun"
		});
		expectNoError();

		// validate exported jar content
		try (Jar result = new Jar(new File(output))) {
			expectJarEntry(result, "jar/biz.aQute.launcher-" + version + ".jar");
			expectJarEntry(result, "jar/org.apache.felix.framework-5.6.10.jar");
			expectJarEntry(result, "jar/printAndExit-1.0.0.jar");
		}
	}

	public void testPackageProject() throws Exception {
		String output = "generated/tmp/export-workspace-project.jar";
		bnd.mainNoExit(new String[] {
			"-t", "package", "-o", output, "testdata/workspace/p2"
		});
		expectNoError();

		// validate exported jar content
		try (Jar result = new Jar(new File(output))) {
			System.out.println(result.getResources()
				.keySet());
			expectJarEntry(result, "jar/biz.aQute.launcher-" + version + ".jar");
			expectJarEntry(result, "jar/org.apache.felix.framework-5.6.10.jar");
			expectJarEntry(result, "jar/p2.jar");
		}
	}

	private void expectOutput(String expected) {
		assertEquals("wrong output", expected, capturedStdOut.toString()
			.trim());
	}

	private void expectNoError() {
		assertEquals("non-empty error output", "", capturedStdErr.toString()
			.trim());
	}

	private void expectJarEntry(Jar jar, String path) {
		assertNotNull("missing entry in jar: " + path, jar.getResource(path));
	}

}
