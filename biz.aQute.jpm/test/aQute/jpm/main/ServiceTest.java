package aQute.jpm.main;

import junit.framework.*;

public class ServiceTest extends TestCase {

	public void testService() throws Exception {
		// Main.main(new String[] {
		// "-etu", "service", "--create",
		// "generated/biz.aQute.jpm.daemon-3.0.0.jar", "dm"
		// });
		Main.main(new String[] {
				"-etu", "service", "--update", "--coordinates", "generated/biz.aQute.jpm.daemon-3.0.0.jar", "dm"
		});
	}
}
