package org.example.myosgibundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	public void start(BundleContext context) throws Exception {
		System.out.println("Hello AS7!");
	}

	public void stop(BundleContext context) throws Exception {
		System.out.println("Bye AS7!");
	}
}
