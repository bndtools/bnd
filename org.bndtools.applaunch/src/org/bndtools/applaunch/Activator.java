package org.bndtools.applaunch;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	@Override
	public void start(BundleContext context) throws Exception {
		new LauncherTracker(context).open(true);
	}

	@Override
	public void stop(BundleContext context) throws Exception {}
}
