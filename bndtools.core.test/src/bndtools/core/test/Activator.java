package bndtools.core.test;

import java.time.Instant;

import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		// bndtools.core.Central has a hard dependency on a running Workbench, so we
		// need to wait until the workbench is up and running. I've not found a better
		// way to do this...
		// Need to do it in the activator, because even loading the test class
		// requires the workbench to be running. This also forces us to make sure
		// we are loaded lazily, otherwise we'll block the Workbench from starting
		// even while we're waiting for it to start...
		Instant endTime = Instant.now().plusMillis(20000);
		while(!PlatformUI.isWorkbenchRunning() && endTime.isAfter(Instant.now())) {
			Thread.sleep(500);
		}
		if (!PlatformUI.isWorkbenchRunning()) {
			throw new IllegalStateException("Workbench not running after 20s");
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}
}
