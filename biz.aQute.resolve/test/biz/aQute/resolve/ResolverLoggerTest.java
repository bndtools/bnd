package biz.aQute.resolve;

import junit.framework.TestCase;

public class ResolverLoggerTest extends TestCase {

	public void testSimple() throws Exception {
		try (ResolverLogger rl = new ResolverLogger()) {
			rl.log(ResolverLogger.LOG_ERROR, "test", null);
			assertEquals(String.format("ERROR: test%n"), rl.getLog());

			for (int i = 0; i < 100000; i++) {
				rl.log(ResolverLogger.LOG_ERROR, "test " + i, null);
			}

			String s = rl.getLog();
			assertTrue(s.endsWith(String.format("test 99999%n")));
		}
	}
}
