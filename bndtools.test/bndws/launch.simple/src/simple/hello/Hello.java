package simple.hello;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Hello implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		System.out.println("Hello World");
	}

	public void stop(BundleContext context) throws Exception {
		System.out.println("Goodbye World!");
	}

}
