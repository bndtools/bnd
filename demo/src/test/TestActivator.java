package test;

import java.lang.instrument.*;
import java.util.*;
import java.util.concurrent.*;

import org.osgi.framework.*;

public class TestActivator implements BundleActivator {

	public void start(final BundleContext context) throws Exception {
		System.err.println("Hello world");
		context.registerService(TestActivator.class.getName(), this, null);

		String p = context.getProperty("test.cmd");
		System.err.println("test.cmd='" + p + "'");
		if ("exit".equals(p))
			System.exit(42);
		else if ("timeout".equals(p)) {
			Thread.sleep(10000);
		}else if ("noreference".equals(p)) {
			String location = context.getBundle().getLocation();
			
			if ( location.startsWith("reference:"))
				System.exit(-1);
			else
				System.exit(15);
				
		} else if ("agent".equals(p)) {
			Hashtable<String,Object> ht = new Hashtable<String,Object>();
			ht.put("main.thread", true);
			context.registerService(Callable.class.getName(), new Callable<Integer>() {

				public Integer call() throws Exception {
					ServiceReference ref = context.getServiceReference(Instrumentation.class.getName());
					if ( ref == null) 
						return -1;
					
					Instrumentation i = (Instrumentation) context.getService(ref);
					if ( i == null)
						return -2;

					return 55;
				}}, ht);
		} else if ("main.thread".equals(p)) {
			Runnable r = new Runnable() {

				public void run() {
					System.err.println("Running in main");
				}

			};
			Properties props = new Properties();
			props.setProperty("main.thread", "true");
			context.registerService(Runnable.class.getName(), r, props);
		} else if ("main.thread.callable".equals(p)) {
			Callable<Integer> r = new Callable<Integer>() {

				public Integer call() throws Exception {
					System.err.println("Running in main");
					return 42;
				}

			};
			Properties props = new Properties();
			props.setProperty("main.thread", "true");
			context.registerService(Callable.class.getName(), r, props);
		} else if ("main.thread.both".equals(p)) {
			class Both implements Callable<Integer>, Runnable {

				public void run() {
					throw new RuntimeException("Wrong, really wrong. The callable has preference");
				}

				public Integer call() throws Exception {
					return 43;
				}

			}
			Both r = new Both();
			Properties props = new Properties();
			props.setProperty("main.thread", "true");
			context.registerService(new String[] {
					Runnable.class.getName(), Callable.class.getName()
			}, r, props);
		} else if ("main.thread.callableinvalidtype".equals(p)) {
			Callable<Double> r = new Callable<Double>() {

				public Double call() throws Exception {
					System.exit(44); // really wrong
					return 44D;
				}

			};
			Properties props = new Properties();
			props.setProperty("main.thread", "true");
			context.registerService(Callable.class.getName(), r, props);
			
			// Give the launcher some time to finish
			// printing the report. etc.
			Thread.sleep(1000);
			System.exit(0);
		} else if ("main.thread.callablenull".equals(p)) {
			Callable<Integer> r = new Callable<Integer>() {

				public Integer call() throws Exception {
					System.err.println("In main, return null");
					return null;
				}

			};
			Properties props = new Properties();
			props.setProperty("main.thread", "true");
			context.registerService(Callable.class.getName(), r, props);
			// throws exception ...
		}

		System.err.println("Done " + p);

	}

	public void stop(BundleContext arg0) throws Exception {
		System.err.println("Goodbye world");
	}

}
