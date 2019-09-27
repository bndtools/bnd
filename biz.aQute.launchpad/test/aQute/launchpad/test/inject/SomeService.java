package aQute.launchpad.test.inject;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class SomeService implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("start");
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put("foo", FrameworkUtil.getBundle(SomeService.class)
			.getSymbolicName());
		context.registerService(SomeService.class, this, properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("stop");

	}

}
