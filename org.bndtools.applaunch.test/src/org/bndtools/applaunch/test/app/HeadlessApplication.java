package org.bndtools.applaunch.test.app;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

/**
 * Headless Eclipse application that publishes what it observed as
 * {@link ApplicationState} service and then blocks, like a real application
 * event loop, until it is stopped.
 */
public class HeadlessApplication implements IApplication {
	private final CountDownLatch					stopped	= new CountDownLatch(1);
	private ServiceRegistration<ApplicationState>	registration;

	@Override
	public Object start(IApplicationContext context) throws Exception {
		String[] args = (String[]) context.getArguments()
			.get(IApplicationContext.APPLICATION_ARGS);
		List<String> applicationArgs = args == null ? List.of() : Arrays.asList(args);
		ApplicationState state = new ApplicationState(Thread.currentThread()
			.getName(), applicationArgs, context);
		BundleContext bundleContext = FrameworkUtil.getBundle(getClass())
			.getBundleContext();
		registration = bundleContext.registerService(ApplicationState.class, state, null);
		context.applicationRunning();
		stopped.await();
		return EXIT_OK;
	}

	@Override
	public void stop() {
		ServiceRegistration<ApplicationState> reg = registration;
		if (reg != null) {
			registration = null;
			try {
				reg.unregister();
			} catch (IllegalStateException e) {
				// already unregistered because the framework is shutting down
			}
		}
		stopped.countDown();
	}
}
