package aQute.junit;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

import aQute.junit.constants.*;

public class Activator extends Thread implements BundleActivator, TesterConstants {
	BundleContext		context;
	volatile boolean	active;
	int					port		= -1;
	String				reportPath;
	boolean				continuous	= false;

	public Activator() {
		super("bnd Runtime Test Bundle");
	}

	public void start(BundleContext context) throws Exception {
		this.context = context;
		active = true;
		start();
	}

	public void stop(BundleContext context) throws Exception {
		active = false;
		interrupt();
		join(10000);
	}

	public void run() {
		continuous = Boolean.valueOf(context.getProperty(TESTER_CONTINUOUS));

		String testcases = context.getProperty(TESTER_NAMES);
		if (context.getProperty(TESTER_PORT) != null)
			port = Integer.parseInt(context.getProperty(TESTER_PORT));

		if (testcases == null)
			automatic();
		else {
			try {
				int errors = test(null, testcases, null);
				System.exit(errors);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-2);
			}
		}
	}

	void automatic() {
		final File reportDir = new File(context.getProperty(TESTER_DIR));
		final List<Bundle> queue = new Vector<Bundle>();

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

		outer: while (active) {
			Bundle bundle;
			synchronized (queue) {
				while (queue.isEmpty() && active) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
						interrupt();
						break outer;
					}
				}
			}
			try {
				bundle = (Bundle) queue.remove(0);
				Writer report = getReportWriter(reportDir, bundle);
				try {
					test(bundle, (String) bundle.getHeaders().get("Test-Cases"), report);
				} finally {
					if (report != null)
						report.close();
				}
			} catch (Exception e) {
				System.err.println("Not sure what happened anymore");
				e.printStackTrace(System.err);
				System.exit(-2);
			}
			if (queue.isEmpty() && !continuous) {
				System.exit(0);
			}

		}
	}

	private void checkBundle(List<Bundle> queue, Bundle bundle) {
		if (bundle.getState() == Bundle.ACTIVE) {
			String testcases = (String) bundle.getHeaders().get("Test-Cases");
			if (testcases != null) {
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
			File f = new File(reportDir, "TEST-" + bundle.getSymbolicName() + "-" + v.getMajor()
					+ "." + v.getMinor() + "." + v.getMicro() + ".xml");
			return new FileWriter(f);
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

			systemOut = new Tee(System.out);
			systemErr = new Tee(System.err);
			systemOut.capture(false);
			systemErr.capture(false);
			System.setOut(systemOut.getStream());
			System.setErr(systemErr.getStream());
			try {

				BasicTestReport basic = new BasicTestReport(systemOut, systemErr) {
					public void check() {
						if (!active)
							result.stop();
					}
				};

				add(reporters, result, basic);

				if (port > 0) {
					add(reporters, result, new JUnitEclipseReport(port));
				}

				if (report != null) {
					add(reporters, result, new JunitXmlReport(report, bundle, basic));
				}

				try {
					TestSuite suite = createSuite(bundle, names);
					List<Test> flattened = new ArrayList<Test>();
					int realcount = flatten(flattened, suite);

					for (TestReporter tr : reporters) {
						tr.begin(fw, bundle, flattened, realcount);
					}
					suite.run(result);

				} catch (Throwable t) {
					result.addError(null, t);
				} finally {
					for (TestReporter tr : reporters) {
						tr.end();
					}
				}
			} finally {
				System.setOut(systemOut.oldStream);
				System.setErr(systemErr.oldStream);
			}
			return result.errorCount() + result.failureCount();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	private TestSuite createSuite(Bundle tfw, List<String> testNames) throws Exception {
		TestSuite suite = new TestSuite();
		for (String fqn : testNames) {
			int n = fqn.indexOf(':');
			if (n > 0) {
				String method = fqn.substring(n + 1);
				fqn = fqn.substring(0, n);
				Class<?> clazz = loadClass(tfw, fqn);
				suite.addTest(TestSuite.createTest(clazz, method));
			} else {
				Class<?> clazz = loadClass(tfw, fqn);
				suite.addTestSuite(clazz);
			}
		}
		return suite;
	}

	private Class<?> loadClass(Bundle tfw, String fqn) {
		if (tfw != null)
			try {
				return tfw.loadClass(fqn);
			} catch (ClassNotFoundException e1) {
				return null;
			}

		Bundle bundles[] = context.getBundles();
		for (int i = bundles.length - 1; i >= 0; i--) {
			try {
				return bundles[i].loadClass(fqn);
			} catch (Exception e) {
				// Ignore, looking further
			}
		}
		return null;
	}

	public int flatten(List<Test> list, TestSuite suite) {
		int realCount = 0;
		for (Enumeration<Test> e = suite.tests(); e.hasMoreElements();) {
			Test test = e.nextElement();
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

}
