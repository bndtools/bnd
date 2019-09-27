package run;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		System.err.println("Run Barry, RUN!!!");
		System.exit(0);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
