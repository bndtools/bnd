package aQute.bnd.runtime.snapshot;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import aQute.bnd.remote.junit.JUnitFramework;
import aQute.bnd.remote.junit.JUnitFrameworkBuilder;
import aQute.bnd.remote.junit.Service;

public class SnapshotTest {

	JUnitFrameworkBuilder builder = new JUnitFrameworkBuilder().bundles(
		"biz.aQute.bnd.runtime.snapshot, org.apache.felix.log, org.apache.felix.configadmin, org.apache.felix.scr, biz.aQute.bnd.runtime.gogo");

	@Test
	public void testMinimum() throws Exception {
		try (JUnitFramework fw = builder.gogo()
			.create()) {

		}
		System.out.println();

	}

	@Service
	ConfigurationAdmin configAdmin;

	@Test
	public void testMoreExtensive() throws Exception {
		try (JUnitFramework fw = builder.gogo()
			.closeTimeout(0)
			.create()) {
			Bundle start1 = fw.bundle()
				.addResource(Comp.class)
				.start();
			Bundle start2 = fw.bundle()
				.addResource(Comp.class)
				.start();
			Thread.sleep(1000);
		}
		System.out.println();

	}

	/**
	 * Test a built in commponent
	 */

	@Component(immediate = true, service = Comp.class)
	public static class Comp {

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
