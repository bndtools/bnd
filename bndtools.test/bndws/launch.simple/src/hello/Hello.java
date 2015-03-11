package hello;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Hello implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		System.out.println("Hello");
		throw new IllegalArgumentException("BoooO!");
	}

	public void stop(BundleContext context) throws Exception {
		System.out.println("Goodbye");
	}

}
