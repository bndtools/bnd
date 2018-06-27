package test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.instrument.Instrumentation;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class TestActivator implements BundleActivator {

	@Override
	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	public void start(final BundleContext context) throws Exception {
		System.err.println("Hello world");
		context.registerService(TestActivator.class.getName(), this, null);

		String p = context.getProperty("test.cmd");
		System.err.println("test.cmd='" + p + "'");
		if ("exit".equals(p))
			System.exit(42);
		else if ("timeout".equals(p)) {
			Thread.sleep(10000);
		}
		if ("setpersistence".equals(p)) {
			File file = context.getDataFile("test.file");
			DataOutputStream dout = new DataOutputStream(new FileOutputStream(file));
			dout.writeUTF("Hello World");
			dout.close();
			System.exit(55);
		} else if ("getpersistence".equals(p)) {
			System.err.println("In get persistence");
			File file = context.getDataFile("test.file");
			if (!file.isFile()) {
				System.err.println("test.file does not exist");
				System.exit(254);
			}

			DataInputStream din = new DataInputStream(new FileInputStream(file));
			String s = din.readUTF();
			din.close();
			if (s.equals("Hello World")) {
				System.err.println("test.file exists & found text");
				System.exit(65);
			} else {
				System.err.println("test.file exists & not found text");
				System.exit(255);
			}

		} else if ("env".equals(p)) {
			String answer = System.getenv("ANSWER");
			try {
				System.err.println("ANSWER=" + answer);
				System.exit(Integer.parseInt(answer));
			} catch (NumberFormatException e) {
				System.exit(255);
			}
		} else if ("noreference".equals(p)) {
			String location = context.getBundle()
				.getLocation();

			if (location.startsWith("reference:"))
				System.exit(255);
			else
				System.exit(15);

		} else if ("agent".equals(p)) {
			Hashtable<String, Object> ht = new Hashtable<>();
			ht.put("main.thread", true);
			context.registerService(Callable.class.getName(), new Callable<Integer>() {

				@Override
				public Integer call() throws Exception {
					ServiceReference ref = context.getServiceReference(Instrumentation.class.getName());
					if (ref == null)
						return -1;

					Instrumentation i = (Instrumentation) context.getService(ref);
					if (i == null)
						return -2;

					return 55;
				}
			}, ht);
		} else if ("quit.no.exit".equals(p)) {
			Callable r = new Callable() {

				@Override
				public Integer call() {
					System.err.println("Quit but not exit()");
					return 197;
				}

			};
			Properties props = new Properties();
			props.setProperty("main.thread", "true");
			context.registerService(Callable.class.getName(), r, (Dictionary) props);
		} else if ("main.thread".equals(p)) {
			Runnable r = new Runnable() {

				@Override
				public void run() {
					System.err.println("Running in main");
				}

			};
			Properties props = new Properties();
			props.setProperty("main.thread", "true");
			context.registerService(Runnable.class.getName(), r, (Dictionary) props);
		} else if ("framework.stop".equals(p)) {
			//
			// Stop the framework
			//

			Runnable r = new Runnable() {

				@Override
				public void run() {
					System.err.println("Running in main");
					Thread t = new Thread() {
						@Override
						public void run() {
							System.err.println("Stopping framework");
							try {
								context.getBundle(0)
									.stop();
								System.err.println("After stopping framework, sleeping");
								Thread.sleep(10000);
								System.err.println("After sleeping");
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					};
					t.start();
					try {
						System.err.println("before joining");
						t.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.err.println("leaving main");
				}
			};
			Properties props = new Properties();
			props.setProperty("main.thread", "true");
			context.registerService(Runnable.class.getName(), r, (Dictionary) props);
		} else if ("main.thread.callable".equals(p)) {
			Callable<Integer> r = new Callable<Integer>() {

				@Override
				public Integer call() throws Exception {
					System.err.println("Running in main");
					return 42;
				}

			};
			Properties props = new Properties();
			props.setProperty("main.thread", "true");
			context.registerService(Callable.class.getName(), r, (Dictionary) props);
		} else if ("main.thread.both".equals(p)) {
			class Both implements Callable<Integer>, Runnable {

				@Override
				public void run() {
					throw new RuntimeException("Wrong, really wrong. The callable has preference");
				}

				@Override
				public Integer call() throws Exception {
					return 43;
				}

			}
			Both r = new Both();
			Properties props = new Properties();
			props.setProperty("main.thread", "true");
			context.registerService(new String[] {
				Runnable.class.getName(), Callable.class.getName()
			}, r, (Dictionary) props);
		} else if ("main.thread.callableinvalidtype".equals(p)) {
			Callable<Double> r = new Callable<Double>() {

				@Override
				public Double call() throws Exception {
					System.exit(44); // really wrong
					return 44D;
				}

			};
			Properties props = new Properties();
			props.setProperty("main.thread", "true");
			context.registerService(Callable.class.getName(), r, (Dictionary) props);

			// Give the launcher some time to finish
			// printing the report. etc.
			Thread.sleep(1000);
			System.exit(0);
		} else if ("main.thread.callablenull".equals(p)) {
			Callable<Integer> r = new Callable<Integer>() {

				@Override
				public Integer call() throws Exception {
					System.err.println("In main, return null");
					return null;
				}

			};
			Properties props = new Properties();
			props.setProperty("main.thread", "true");
			context.registerService(Callable.class.getName(), r, (Dictionary) props);
			// throws exception ...
		}

		System.err.println("Done " + p);

	}

	@Override
	public void stop(BundleContext arg0) throws Exception {
		System.err.println("Goodbye world");
	}

}
