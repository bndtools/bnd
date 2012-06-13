package test;

import java.util.*;

import org.osgi.framework.*;

public class TestActivator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		System.err.println("Hello world");
		context.registerService(TestActivator.class.getName(), this, null);

		String p = context.getProperty("test.cmd");
		System.err.println("test.cmd='" + p + "'");
		if ("exit".equals(p))
			System.exit(42);
		else if ("timeout".equals(p)) {
			Thread.sleep(10000);
		} else if ("main.thread".equals(p)) {
			Runnable r = new Runnable() {

				public void run() {
					System.err.println("Running in main");
				}

			};
			Properties props = new Properties();
			props.setProperty("main.thread", "true");
			context.registerService(Runnable.class.getName(), r, props);
		}

		System.err.println("Done " + p);

	}

	public void stop(BundleContext arg0) throws Exception {
		System.err.println("Goodbye world");
	}

}
