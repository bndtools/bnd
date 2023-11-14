package bndtools.core.test.utils;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.test.common.exceptions.Exceptions;

import aQute.tester.junit.platform.api.BeforeTestLoopCallback;

@Component(immediate = true)
public class WorkbenchStartupCallback implements BeforeTestLoopCallback {

	static CountDownLatch	flag	= new CountDownLatch(1);

	final BundleContext		bc;

	CountDownLatch			latch	= new CountDownLatch(1);

	@Activate
	public WorkbenchStartupCallback(BundleContext bc) {
		this.bc = bc;
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	void setWB(IWorkbench wb, Map<String, ?> props) {
		latch.countDown();
	}

	void unsetWB(IWorkbench wb) {
		latch = new CountDownLatch(1);
	}

	@Override
	public void beforeTestLoop() {
		System.err.println("===> waiting for Workbench");
		try {
			if (!latch.await(15000, TimeUnit.MILLISECONDS)) {
				throw new RuntimeException("Eclipse E4 workbench didn't arrive after 15s");
			}
			System.err.println("Workbench has arrived!");
			JavaCore.initializeAfterLoad(null);
			System.err.println("JavaCore initialized");
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}
}
