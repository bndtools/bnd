package aQute.libg.command;

import junit.framework.TestCase;

public class ExecuteTest extends TestCase {

	public static void testSimple() throws Exception {
		Command c = new Command();
		c.add("java");
		c.add("-version");
		assertEquals(0, c.execute(System.out, System.err));
	}

	// public void testCoffee() throws Exception {
	// Command c = new Command();
	// c.arg("sh", "-c", "coffee -sc")
	// //
	// .var("PATH",
	// "/opt/local/bin:/opt/local/sbin:/Developer/usr/bin:/opt/local/bin:/opt/local/sbin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/usr/X11/bin:/usr/local/git/bin");
	// assertEquals(0, c.execute("()->1", System.out, System.err));
	// }
}
