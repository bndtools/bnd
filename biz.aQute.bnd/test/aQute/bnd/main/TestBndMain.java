package aQute.bnd.main;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

public class TestBndMain extends TestCase {

	private final ByteArrayOutputStream	capturedStdOut	= new ByteArrayOutputStream();
	private PrintStream					originalStdOut;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		capturedStdOut.reset();
		originalStdOut = System.out;
		System.setOut(new PrintStream(capturedStdOut));
	}

	@Override
	protected void tearDown() throws Exception {
		System.setOut(originalStdOut);
		super.tearDown();
	}

	public void testBndVersion() throws Exception {
		bnd.mainNoExit(new String[] {
				"version"
		});
		expectOutput("${Bundle-Version}");
	}

	public void testRunStandalone() throws Exception {
		bnd.mainNoExit(new String[] {
				"run", "testdata/standalone/standalone.bndrun"
		});
		expectOutput("Gesundheit!");
	}

	public void testRunWorkspace() throws Exception {
		bnd.mainNoExit(new String[] {
				"run", "testdata/workspace/p/workspace.bndrun"
		});

		expectOutput("Gesundheit!");
	}

	private void expectOutput(String expected) {
		assertEquals("wrong output", expected, capturedStdOut.toString().trim());
	}

}
