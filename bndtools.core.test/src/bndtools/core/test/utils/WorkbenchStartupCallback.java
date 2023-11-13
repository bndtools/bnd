package bndtools.core.test.utils;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;

import org.eclipse.e4.ui.workbench.IWorkbench;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import aQute.tester.junit.platform.api.BeforeTestLoopCallback;

@Component(immediate = true)
public class WorkbenchStartupCallback implements BeforeTestLoopCallback {

	static CountDownLatch	flag	= new CountDownLatch(1);

	final BundleContext		bc;

	@Activate
	public WorkbenchStartupCallback(BundleContext bc) {
		this.bc = bc;
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	volatile IWorkbench	wb;

	@Override
	public void beforeTestLoop() {
		try {
			// Wait for the Bnd Workspace service to be available before
			// proceeding.
			Instant end = Instant.now()
				.plusMillis(15000);
			while (wb == null && Instant.now()
				.isBefore(end)) {
				Thread.sleep(100);
			}

			if (wb == null) {
				throw new RuntimeException("Eclipse E4 workbench didn't arrive after 15s");
			}
		} catch (InterruptedException ie) {
			throw new RuntimeException("Interrupted!", ie);
		}
	}
}
