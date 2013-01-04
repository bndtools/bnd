package aQute.junit;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import junit.framework.*;

import org.junit.runner.*;
import org.junit.runner.manipulation.*;
import org.osgi.framework.*;

import aQute.junit.constants.*;

public class Activator implements BundleActivator, TesterConstants, Runnable {
	BundleContext		context;
	volatile boolean	active;
	int					port		= -1;
	boolean				continuous	= false;
	boolean				trace		= false;
	PrintStream			out			= System.err;
	JUnitEclipseReport	jUnitEclipseReport;
	volatile Thread		thread;

	public Activator() {
	}

	public void start(BundleContext context) throws Exception {
		this.context = context;
		active = true;
		if (context.getProperty(TESTER_SEPARATETHREAD) == null) {			
			Hashtable<String,String> ht = new Hashtable<String,String>();
			ht.put("main.thread", "true");
			ht.put(Constants.SERVICE_DESCRIPTION, "JUnit tester");
			context.registerService(Runnable.class.getName(), this, ht);
		} else {
			thread = new Thread("bnd Runtime Test Bundle");
			thread.start();
		}
	}

	public void stop(BundleContext context) throws Exception {
		active = false;
		if (jUnitEclipseReport != null)
			jUnitEclipseReport.close();
		
		if (thread != null) {
			thread.interrupt();
			thread.join(10000);
		}
	}

	public void run() {
		
		continuous = Boolean.valueOf(context.getProperty(TESTER_CONTINUOUS));
		trace = context.getProperty(TESTER_TRACE) != null;
		
		if (thread == null)
			trace("running in main thread");
		
		// We can be started on our own thread or from the main code
		thread = Thread.currentThread();
		

		String testcases = context.getProperty(TESTER_NAMES);
		trace("test cases %s", testcases);
		if (context.getProperty(TESTER_PORT) != null) {
			port = Integer.parseInt(context.getProperty(TESTER_PORT));
			try {
				trace("using port %s", port);
				jUnitEclipseReport = new JUnitEclipseReport(port);
			}
			catch (Exception e) {
				System.err.println("Cannot create link Eclipse JUnit on port " + port);
				System.exit(-2);
			}
		}

		if (testcases == null) {
			trace("automatic testing of all bundles with Test-Cases header");
			try {
				automatic();
			}
			catch (IOException e) {
				// ignore
			}
		} else {
			trace("receivednames of classes to test %s", testcases);
			try {
				int errors = test(null, testcases, null);
				System.exit(errors);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-2);
			}
		}
	}

	void automatic() throws IOException {
		String testerDir = context.getProperty(TESTER_DIR);
		if (testerDir == null)
			testerDir = "testdir";

		final File reportDir = new File(testerDir);
		final List<Bundle> queue = new Vector<Bundle>();
		if (!reportDir.exists() && !reportDir.mkdirs()) {
			throw new IOException("Could not create directory " + reportDir);
		}
		trace("using %s, needed creation %s", reportDir, reportDir.mkdirs());

		trace("adding Bundle Listener for getting test bundle events");
		context.addBundleListener(new SynchronousBundleListener() {
			public void bundleChanged(BundleEvent event) {
				if (event.getType() == BundleEvent.STARTED) {
					checkBundle(queue, event.getBundle());
				}

			}
		});

		for (Bundle b : context.getBundles()) {
			checkBundle(queue, b);
		}

		trace("starting queue");
		int result = 0;
		outer: while (active) {
			Bundle bundle;
			synchronized (queue) {
				while (queue.isEmpty() && active) {
					try {
						queue.wait();
					}
					catch (InterruptedException e) {
						trace("tests bundle queue interrupted");
						thread.interrupt();
						break outer;
					}
				}
			}
			try {
				bundle = queue.remove(0);
				trace("received bundle to test: %s", bundle.getLocation());
				Writer report = getReportWriter(reportDir, bundle);
				try {
					trace("test will run");
					result += test(bundle, (String) bundle.getHeaders().get("Test-Cases"), report);
					trace("test ran");
					if (queue.isEmpty() && !continuous) {
						trace("queue " + queue);
						System.exit(result);
					}
				}
				finally {
					if (report != null)
						report.close();
				}
			}
			catch (Exception e) {
				error("Not sure what happened anymore %s", e);
				System.exit(-2);
			}
		}
	}

	void checkBundle(List<Bundle> queue, Bundle bundle) {
		if (bundle.getState() == Bundle.ACTIVE) {
			String testcases = (String) bundle.getHeaders().get("Test-Cases");
			if (testcases != null) {
				trace("found active bundle with test cases %s : %s", bundle, testcases);
				synchronized (queue) {
					queue.add(bundle);
					queue.notifyAll();
				}
			}
		}
	}

	private Writer getReportWriter(File reportDir, Bundle bundle) throws IOException {
		if (reportDir.isDirectory()) {
			Version v = bundle.getVersion();
			File f = new File(reportDir, "TEST-" + bundle.getSymbolicName() + "-" + v.getMajor() + "." + v.getMinor()
					+ "." + v.getMicro() + ".xml");
			return new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
		}
		return null;
	}

	/**
	 * The main test routine.
	 * 
	 * @param bundle
	 *            The bundle under test or null
	 * @param testnames
	 *            The names to test
	 * @param report
	 *            The report writer or null
	 * @return # of errors
	 */
	int test(Bundle bundle, String testnames, Writer report) {
		trace("testing bundle %s with %s", bundle, testnames);
		Bundle fw = context.getBundle(0);
		try {
			List<String> names = new ArrayList<String>();
			StringTokenizer st = new StringTokenizer(testnames, " ,");
			while (st.hasMoreTokens())
				names.add(st.nextToken());

			List<TestReporter> reporters = new ArrayList<TestReporter>();
			final TestResult result = new TestResult();

			Tee systemErr;
			Tee systemOut;

			systemOut = new Tee(System.err);
			systemErr = new Tee(System.err);
			systemOut.capture(trace).echo(true);
			systemErr.capture(trace).echo(true);
			System.setOut(systemOut.getStream());
			System.setErr(systemErr.getStream());
			trace("changed streams");
			try {

				BasicTestReport basic = new BasicTestReport(this, systemOut, systemErr) {
					@Override
					public void check() {
						if (!active)
							result.stop();
					}
				};

				add(reporters, result, basic);

				if (port > 0) {
					add(reporters, result, jUnitEclipseReport);
				}

				if (report != null) {
					add(reporters, result, new JunitXmlReport(report, bundle, basic));
				}

				for (TestReporter tr : reporters) {
					tr.setup(fw, bundle);
				}

				try {
					TestSuite suite = createSuite(bundle, names, result);
					trace("created suite " + suite);
					List<Test> flattened = new ArrayList<Test>();
					int realcount = flatten(flattened, suite);

					for (TestReporter tr : reporters) {
						tr.begin(flattened, realcount);
					}
					trace("running suite " + suite);
					suite.run(result);

				}
				catch (Throwable t) {
					trace(t.getMessage());
					result.addError(null, t);
				}
				finally {
					for (TestReporter tr : reporters) {
						tr.end();
					}
				}
			}
			catch (Throwable t) {
				System.err.println("exiting " + t);
				t.printStackTrace();
			}
			finally {
				System.setOut(systemOut.oldStream);
				System.setErr(systemErr.oldStream);
				trace("unset streams");
			}
			System.err.println("Errors: " + result.errorCount());
			System.err.println("Failures: " + result.failureCount());
			return result.errorCount() + result.failureCount();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	private TestSuite createSuite(Bundle tfw, List<String> testNames, TestResult result) throws Exception {
		TestSuite suite = new TestSuite();
		for (String fqn : testNames) {
			addTest(tfw, suite, fqn, result);
		}
		return suite;
	}

	private void addTest(Bundle tfw, TestSuite suite, String fqn, TestResult testResult) {
		try {
			int n = fqn.indexOf(':');
			if (n > -1) {
				String method = fqn.substring(n + 1);
				fqn = fqn.substring(0, n);
				Class< ? > clazz = loadClass(tfw, fqn);
				if (clazz != null)
					addTest(tfw, suite, clazz, testResult, method);
				else {
					diagnoseNoClass(tfw, fqn);
					testResult.addError(suite, new Exception("Cannot load class " + fqn
							+ ", was it included in the test bundle?"));
				}

			} else {
				Class< ? > clazz = loadClass(tfw, fqn);
				if (clazz != null)
					addTest(tfw, suite, clazz, testResult, null);
				else {
					diagnoseNoClass(tfw, fqn);
					testResult.addError(suite, new Exception("Cannot load class " + fqn
							+ ", was it included in the test bundle?"));
				}
			}
		}
		catch (Throwable e) {
			System.err.println("Can not create test case for: " + fqn + " : " + e);
			testResult.addError(suite, e);
		}
	}

	private void diagnoseNoClass(Bundle tfw, String fqn) {
		if ( tfw == null) {
			error("No class found: %s, target bundle: %s", fqn, tfw);
			trace("Installed bundles:");
			for ( Bundle bundle : context.getBundles()) {
				Class<?> c = loadClass(bundle,fqn);
				String state;
				switch(bundle.getState()) {
					case Bundle.UNINSTALLED: state = "UNINSTALLED"; break;
					case Bundle.INSTALLED: state = "INSTALLED"; break;
					case Bundle.RESOLVED: state = "RESOLVED"; break;
					case Bundle.STARTING: state = "STARTING"; break;
					case Bundle.STOPPING: state = "STOPPING"; break;
					case Bundle.ACTIVE: state = "ACTIVE"; break;
					default:
						state = "UNKNOWN";
				}
				trace("%s %s %s", bundle.getLocation(), state, c != null);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void addTest(@SuppressWarnings("unused")
	Bundle tfw, TestSuite suite, Class< ? > clazz, @SuppressWarnings("unused")
	TestResult testResult, final String method) {
		if (TestCase.class.isAssignableFrom(clazz)) {
			if (method != null) {
				suite.addTest(TestSuite.createTest(clazz, method));
				return;
			}
			suite.addTestSuite((Class< ? extends TestCase>) clazz);
			return;
		}

		JUnit4TestAdapter adapter = new JUnit4TestAdapter(clazz);
		if (method != null) {
			try {
				adapter.filter(new org.junit.runner.manipulation.Filter() {

					@Override
					public String describe() {
						return "Method filter";
					}

					@Override
					public boolean shouldRun(Description description) {
						if (method.equals(description.getMethodName())) {
							return true;
						}
						return false;
					}
				});
			}
			catch (NoTestsRemainException e) {
				return;
			}
		}
		suite.addTest(new JUnit4TestAdapter(clazz));
	}

	private Class< ? > loadClass(Bundle tfw, String fqn) {
		try {
			if (tfw != null) {
				checkResolved(tfw);
				try {
					return tfw.loadClass(fqn);
				}
				catch (ClassNotFoundException e1) {
					return null;
				}
			}

			Bundle bundles[] = context.getBundles();
			for (int i = bundles.length - 1; i >= 0; i--) {
				try {
					checkResolved(bundles[i]);
					return bundles[i].loadClass(fqn);
				}
				catch (ClassNotFoundException e1) {
					// try next
				}
			}
		}
		catch (Exception e) {
			error("Exception during loading of class: %s. Exception %s and cause %s. This sometimes "
					+ "happens when there is an error in the static initialization, the class has "
					+ "no public constructor, it is an inner class, or it has no public access", fqn, e, e.getCause());
		}
		return null;
	}

	private void checkResolved(Bundle bundle) {
		int state = bundle.getState();
		if (state == Bundle.INSTALLED || state == Bundle.UNINSTALLED) {
			trace("unresolved bundle %s", bundle.getLocation());
		}
	}

	public int flatten(List<Test> list, TestSuite suite) {
		int realCount = 0;
		for (Enumeration< ? > e = suite.tests(); e.hasMoreElements();) {
			Test test = (Test) e.nextElement();
			list.add(test);
			if (test instanceof TestSuite)
				realCount += flatten(list, (TestSuite) test);
			else
				realCount++;
		}
		return realCount;
	}

	private void add(List<TestReporter> reporters, TestResult result, TestReporter rp) {
		reporters.add(rp);
		result.addListener(rp);
	}

	static public String replace(String source, String symbol, String replace) {
		StringBuffer sb = new StringBuffer(source);
		int n = sb.indexOf(symbol, 0);
		while (n > 0) {
			sb.replace(n, n + symbol.length(), replace);
			n = n - symbol.length() + replace.length();
			n = sb.indexOf(replace, n);
		}
		return sb.toString();
	}

	public void trace(String msg, Object... objects) {
		if (trace) {
			message("# ", msg, objects);
		}
	}

	private void message(String prefix, String string, Object[] objects) {
		Throwable e = null;

		StringBuffer sb = new StringBuffer();
		int n = 0;
		sb.append(prefix);
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == '%') {
				c = string.charAt(++i);
				switch (c) {
					case 's' :
						if (n < objects.length) {
							Object o = objects[n++];
							if (o instanceof Throwable) {
								e = (Throwable) o;
								if (o instanceof InvocationTargetException) {
									Throwable t = (InvocationTargetException) o;
									sb.append(t.getMessage());
									e = t;
								} else
									sb.append(e.getMessage());
							} else {
								sb.append(o);
							}
						} else
							sb.append("<no more arguments>");
						break;

					default :
						sb.append(c);
				}
			} else {
				sb.append(c);
			}
		}
		out.println(sb);
		if (e != null && trace)
			e.printStackTrace(out);
	}

	public void error(String msg, Object... objects) {
		message("! ", msg, objects);
	}

}
