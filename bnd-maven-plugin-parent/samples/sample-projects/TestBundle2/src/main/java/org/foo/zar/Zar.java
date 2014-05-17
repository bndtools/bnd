package org.foo.zar;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Zar implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("### Starting Zar");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("### Stopping Zar");
	}
}
