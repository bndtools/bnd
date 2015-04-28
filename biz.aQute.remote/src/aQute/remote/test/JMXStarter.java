package aQute.remote.test;

import java.lang.management.*;

import javax.management.*;

import org.osgi.framework.*;

public class JMXStarter implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		context.registerService(MBeanServer.class.getName(), mbs, null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
