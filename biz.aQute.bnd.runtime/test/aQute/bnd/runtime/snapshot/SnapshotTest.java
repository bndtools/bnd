package aQute.bnd.runtime.snapshot;

import org.junit.Test;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.build.Workspace;
import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.lib.io.IO;

public class SnapshotTest {

	static Workspace w;

	static {
		try {
			Workspace.findWorkspace(IO.work);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	LaunchpadBuilder builder = new LaunchpadBuilder().runfw("org.apache.felix.framework")
		.bundles(
			"biz.aQute.bnd.runtime.snapshot, org.apache.felix.log, org.apache.felix.configadmin, org.apache.felix.scr");

	@Test
	public void testMinimum() throws Exception {
		try (Launchpad fw = builder
			.create()) {

		}
		System.out.println();

	}

	@Service
	ConfigurationAdmin configAdmin;


	/**
	 * Test a built in commponent
	 */

	@Component(immediate = true, service = Comp.class)
	public static class Comp {
		@Reference
		ListenerHook hook;

		@Activate
		void activate() {
			System.out.println("Activate");
		}

		@Deactivate
		void deactivate() {
			System.out.println("Deactivate");
		}
	}

}
