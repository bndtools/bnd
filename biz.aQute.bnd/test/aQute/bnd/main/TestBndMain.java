package aQute.bnd.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import aQute.bnd.osgi.Jar;
import junit.framework.TestCase;

public class TestBndMain extends TestCase {

	private final ByteArrayOutputStream	capturedStdOut	= new ByteArrayOutputStream();
	private PrintStream					originalStdOut;

	private final ByteArrayOutputStream	capturedStdErr	= new ByteArrayOutputStream();
	private PrintStream					originalStdErr;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

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

	public void testBndVersion() throws Exception {
		bnd.mainNoExit(new String[] {
				"version"
		});
		expectNoError();
		expectOutput("${Bundle-Version}");
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

	public void testPackageBndrun() throws Exception {
		bnd.mainNoExit(new String[] {
				"package", "-o", "generated/export.jar", "testdata/standalone/standalone.bndrun"
		});

		// validate exported jar content
		try (Jar result = new Jar(new File("generated/export.jar"))) {
			assertNotNull("missing entry in jar", result.getResource("jar/biz.aQute.launcher-3.1.0.jar"));
			assertNotNull("missing entry in jar", result.getResource("jar/org.apache.felix.framework-5.2.0.jar"));
			assertNotNull("missing entry in jar", result.getResource("jar/printAndExit-1.0.0.jar"));
		}
		expectNoError();
	}

	private void expectOutput(String expected) {
		assertEquals("wrong output", expected, capturedStdOut.toString().trim());
	}

	private void expectNoError() {
		assertEquals("non-empty error output", "", capturedStdErr.toString().trim());
	}

}
