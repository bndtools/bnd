package aQute.bnd.main;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

public class TestBndMain extends TestCase {

	private final ByteArrayOutputStream	capturedStdout	= new ByteArrayOutputStream();

	private PrintStream					originalStdout;

	@Override
	protected void setUp() throws Exception {
		capturedStdout.reset();
		originalStdout = System.out;
		System.setOut(new PrintStream(capturedStdout));
	}

	@Override
	protected void tearDown() throws Exception {
		System.setOut(originalStdout);
	}

	public void testBndVersion() throws Exception {
		bnd.mainNoExit(new String[] {
				"version"
		});
		// bnd.main(new String[] {"-etb",
		// "/Ws/osgi/master/osgi.ct/generated/osgi.ct.cmpn", "runtests",
		// "org.osgi.test.cases.log.bnd", "org.osgi.test.cases.metatype.bnd"});

		assertEquals("${Bundle-Version}", capturedStdout.toString().trim());
	}
}
