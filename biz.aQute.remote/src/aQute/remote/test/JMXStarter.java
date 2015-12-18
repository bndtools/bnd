package aQute.remote.test;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class JMXStarter implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		context.registerService(MBeanServer.class.getName(), mbs, null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {}

}
