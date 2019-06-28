package aQute.junit;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import org.junit.runner.Description;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import aQute.junit.constants.TesterConstants;
import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

public class Activator implements BundleActivator, TesterConstants, Runnable {
	BundleContext		context;
	volatile boolean	active;
	int					port		= -1;
	boolean				continuous	= false;
	boolean				trace		= false;
	PrintStream			out			= System.err;
	JUnitEclipseReport	jUnitEclipseReport;
	volatile Thread		thread;

	public Activator() {}

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		active = true;
		if (!Boolean.valueOf(context.getProperty(TESTER_SEPARATETHREAD))
			&& Boolean.valueOf(context.getProperty("launch.services"))) { // can't
																			// register
																			// services
																			// on
																			// mini
																			// framework
			Hashtable<String, String> ht = new Hashtable<>();
			ht.put("main.thread", "true");
			ht.put(Constants.SERVICE_DESCRIPTION, "JUnit tester");
			context.registerService(Runnable.class.getName(), this, ht);
		} else {
			thread = new Thread(this, "bnd Runtime Test Bundle");
			thread.start();
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		active = false;
		if (jUnitEclipseReport != null)
			jUnitEclipseReport.close();

		if (thread != null) {
			thread.interrupt();

			thread.join(10000);
		}
	}

	public boolean active() {
		return active;
	}

	@Override
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
			} catch (Exception e) {
				System.err.println("Cannot create link Eclipse JUnit on port " + port);
				System.exit(254);
			}
		}

		String testerDir = context.getProperty(TESTER_DIR);
		if (testerDir == null)
			testerDir = "testdir";

		final File reportDir = new File(testerDir);
		//
		// Jenkins does not detect test failures unless reported
		// by JUnit XML output. If we have an unresolved failure
		// we timeout. The following will test if there are any
		// unresolveds and report this as a JUnit failure. It can
		// be disabled with -testunresolved=false
		//

		String unresolved = context.getProperty(TESTER_UNRESOLVED);
		trace("run unresolved %s", unresolved);

		if (unresolved == null || unresolved.equalsIgnoreCase("true")) {
			//
			// Check if there are any unresolved bundles.
			// If yes, we run a test case to get a proper JUnit report
			//
			Bundle testBundle = null;
			for (Bundle b : context.getBundles()) {
				if (b.getState() == Bundle.INSTALLED) {
					testBundle = b;
					break;
				}
			}
			if (testBundle != null) {
				for (Bundle b : context.getBundles()) {
					String testcasesheader = b.getHeaders()
						.get(aQute.bnd.osgi.Constants.TESTCASES);
					if (testcasesheader != null) {
						testBundle = b;
						break;
					}
				}
				int err = 0;
				try {
					err = test(context.getBundle(), "aQute.junit.UnresolvedTester",
						getReportWriter(reportDir, bundleReportName(testBundle)));
				} catch (IOException e) {
					// ignore
				}
				if (err != 0) {
					System.exit(err);
				}
			}
		}

		if (!reportDir.exists() && !reportDir.mkdirs()) {
			System.err.printf("Could not create directory %s%n", reportDir);
		}
		trace("using %s", reportDir);

		if (testcases == null) {
			trace("automatic testing of all bundles with " + aQute.bnd.osgi.Constants.TESTCASES + " header");
			try {
				automatic(reportDir);
			} catch (IOException e) {
				// ignore
			}
		} else {
			trace("receivednames of classes to test %s", testcases);
			try {
				int errors;
				try (Writer report = getReportWriter(reportDir, reportDir.getName())) {
					errors = test(null, testcases, report);
				}
				System.exit(errors);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(254);
			}
		}
	}

	void automatic(File reportDir) throws IOException {
		final List<Bundle> queue = new Vector<>();

		trace("adding Bundle Listener for getting test bundle events");
		context.addBundleListener((SynchronousBundleListener) event -> {
			switch (event.getType()) {
				case BundleEvent.STARTED :
				case BundleEvent.RESOLVED :
					checkBundle(queue, event.getBundle());
					break;
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
					} catch (InterruptedException e) {
						trace("tests bundle queue interrupted");
						thread.interrupt();
						break outer;
					}
				}
			}
			try {
				bundle = queue.remove(0);
				trace("received bundle to test: %s", bundle.getLocation());
				try (Writer report = getReportWriter(reportDir, bundleReportName(bundle))) {
					trace("test will run");
					result += test(bundle, bundle.getHeaders()
						.get(aQute.bnd.osgi.Constants.TESTCASES), report);
					trace("test ran");
					if (queue.isEmpty() && !continuous) {
						trace("queue " + queue);
						System.exit(result);
					}
				}
			} catch (Exception e) {
				error("Not sure what happened anymore %s", e);
				System.exit(254);
			}
		}
	}

	void checkBundle(List<Bundle> queue, Bundle bundle) {
		Bundle host = findHost(bundle);

		if (host != null && (host.getState() == Bundle.ACTIVE || host.getState() == Bundle.STARTING)) {
			String testcases = bundle.getHeaders()
				.get(aQute.bnd.osgi.Constants.TESTCASES);
			if (testcases != null) {
				trace("found active bundle with test cases %s : %s", bundle, testcases);
				synchronized (queue) {
					queue.add(bundle);
					queue.notifyAll();
				}
			}
		}
	}

	private Writer getReportWriter(File reportDir, String name) throws IOException {
		if (reportDir.isDirectory()) {
			File f = new File(reportDir, "TEST-" + name + ".xml");
			Writer writer = new OutputStreamWriter(Files.newOutputStream(f.toPath()), UTF_8);
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			return writer;
		}
		return null;
	}

	private String bundleReportName(Bundle bundle) {
		Version v = bundle.getVersion();
		return bundle.getSymbolicName() + "-" + v.getMajor() + "." + v.getMinor() + "." + v.getMicro();
	}

	/**
	 * The main test routine.
	 *
	 * @param bundle The bundle under test or null
	 * @param testnames The names to test
	 * @param report The report writer or null
	 * @return # of errors
	 */
	int test(Bundle bundle, String testnames, Writer report) {
		trace("testing bundle %s with %s", bundle, testnames);
		Bundle fw = context.getBundle(0);
		try {

			bundle = findHost(bundle);

			List<String> names = Arrays.asList(testnames.trim()
				.split(",\\s*"));

			List<TestReporter> reporters = new ArrayList<>();
			final TestResult result = new TestResult();

			Tee systemOut = new Tee(System.err);
			Tee systemErr = new Tee(System.err);
			systemOut.capture(isTrace())
				.echo(true);
			systemErr.capture(isTrace())
				.echo(true);
			final PrintStream originalOut = System.out;
			final PrintStream originalErr = System.err;
			System.setOut(systemOut.getStream());
			System.setErr(systemErr.getStream());
			trace("changed streams");
			try {

				BasicTestReport basic = new BasicTestReport(this, systemOut, systemErr, result);

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

				TestSuite suite = createSuite(bundle, names, result);
				try {
					trace("created suite %s #%s", suite.getName(), suite.countTestCases());
					List<Test> flattened = new ArrayList<>();
					int realcount = flatten(flattened, suite);

					for (TestReporter tr : reporters) {
						tr.begin(flattened, realcount);
					}
					trace("running suite %s", suite);
					suite.run(result);

				} catch (Throwable t) {
					trace("%s", t);
					result.addError(suite, t);
				} finally {
					for (TestReporter tr : reporters) {
						tr.end();
					}
				}
			} catch (Throwable t) {
				System.err.println("exiting " + t);
				t.printStackTrace(System.err);
			} finally {
				System.setOut(originalOut);
				System.setErr(originalErr);
				trace("unset streams");
			}
			System.err.println("Tests run  : " + result.runCount());
			System.err.println("Passed     : " + (result.runCount() - result.errorCount() - result.failureCount()));
			System.err.println("Errors     : " + result.errorCount());
			System.err.println("Failures   : " + result.failureCount());

			return result.errorCount() + result.failureCount();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	/*
	 * Find the host for a fragment. We just iterate over all other bundles and
	 * ask for the fragments. We returns the first one found.
	 */
	private Bundle findHost(Bundle bundle) {
		if (bundle == null)
			return null;

		List<Bundle> hosts = new ArrayList<>();
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		// Bundle isn't resolved.
		if (wiring == null) {
			System.err.println("unresolved bundle: " + bundle);
			return null;
		}
		List<BundleWire> wires = wiring.getRequiredWires(HostNamespace.HOST_NAMESPACE);
		System.err.println("required wires for " + bundle + " " + wires);

		for (BundleWire wire : wires) {
			hosts.add(wire.getProviderWiring()
				.getRevision()
				.getBundle());
		}
		if (hosts.isEmpty()) {
			return bundle;
		}
		if (hosts.size() > 1) {
			trace("Found multiple hosts for fragment %s: %s", bundle, hosts);
		}
		return hosts.get(0);
	}

	private TestSuite createSuite(Bundle tfw, List<String> testNames, TestResult result) {
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
				Class<?> clazz = loadClass(tfw, fqn);
				if (clazz != null)
					addTest(suite, clazz, method);
				else {
					diagnoseNoClass(tfw, fqn);
					testResult.addError(suite,
						new Exception("Cannot load class " + fqn + ", was it included in the test bundle?"));
				}

			} else {
				Class<?> clazz = loadClass(tfw, fqn);
				if (clazz != null)
					addTest(suite, clazz, null);
				else {
					diagnoseNoClass(tfw, fqn);
					testResult.addError(suite,
						new Exception("Cannot load class " + fqn + ", was it included in the test bundle?"));
				}
			}
		} catch (Throwable e) {
			System.err.println("Can not create test case for: " + fqn + " : " + e);
			testResult.addError(suite, e);
		}
	}

	private void diagnoseNoClass(Bundle tfw, String fqn) {
		if (tfw == null) {
			error("No class found: %s, target bundle: %s", fqn, tfw);
			trace("Installed bundles:");
			for (Bundle bundle : context.getBundles()) {
				Class<?> c = loadClass(bundle, fqn);
				String state;
				switch (bundle.getState()) {
					case Bundle.UNINSTALLED :
						state = "UNINSTALLED";
						break;
					case Bundle.INSTALLED :
						state = "INSTALLED";
						break;
					case Bundle.RESOLVED :
						state = "RESOLVED";
						break;
					case Bundle.STARTING :
						state = "STARTING";
						break;
					case Bundle.STOPPING :
						state = "STOPPING";
						break;
					case Bundle.ACTIVE :
						state = "ACTIVE";
						break;
					default :
						state = "UNKNOWN";
				}
				trace("%s %s %s", bundle.getLocation(), state, c != null);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void addTest(TestSuite suite, Class<?> clazz, final String method) {

		if (TestCase.class.isAssignableFrom(clazz)) {
			if (hasJunit4Annotations(clazz)) {
				error(
					"The test class %s extends %s and it uses JUnit 4 annotations. This means that the annotations will be ignored.",
					clazz.getName(), TestCase.class.getName());
			}
			trace("using JUnit 3");
			if (method != null) {
				suite.addTest(TestSuite.createTest(clazz, method));
				return;
			}
			suite.addTestSuite((Class<? extends TestCase>) clazz);
			return;
		}

		trace("using JUnit 4");

		JUnit4TestAdapter adapter = new JUnit4TestAdapter(clazz);
		if (method != null) {
			trace("method specified " + clazz + ":" + method);
			final Pattern glob = Pattern.compile(method.replaceAll("\\*", ".*")
				.replaceAll("\\?", ".?"));

			try {
				adapter.filter(new org.junit.runner.manipulation.Filter() {

					@Override
					public String describe() {
						return "Method filter for " + method;
					}

					@Override
					public boolean shouldRun(Description description) {
						if (glob.matcher(description.getMethodName())
							.lookingAt()) {
							trace("accepted " + description.getMethodName());
							return true;
						}
						trace("rejected " + description.getMethodName());
						return false;
					}
				});
			} catch (NoTestsRemainException e) {
				return;
			}
		}
		suite.addTest(adapter);
	}

	private boolean hasJunit4Annotations(Class<?> clazz) {
		if (hasAnnotations("org.junit.", clazz.getAnnotations()))
			return true;

		for (Method m : clazz.getMethods()) {
			if (hasAnnotations("org.junit.", m.getAnnotations()))
				return true;
		}
		return false;
	}

	private boolean hasAnnotations(String prefix, Annotation[] annotations) {
		if (annotations != null)
			for (Annotation a : annotations)
				if (a.getClass()
					.getName()
					.startsWith(prefix))
					return true;
		return false;
	}

	private Class<?> loadClass(Bundle tfw, String fqn) {
		try {
			if (tfw != null) {
				checkResolved(tfw);
				try {
					return tfw.loadClass(fqn);
				} catch (ClassNotFoundException e1) {
					return null;
				}
			}

			trace("finding %s", fqn);
			Bundle bundles[] = context.getBundles();
			for (int i = bundles.length - 1; i >= 0; i--) {
				try {
					checkResolved(bundles[i]);
					trace("found in %s", bundles[i]);
					return bundles[i].loadClass(fqn);
				} catch (ClassNotFoundException e1) {
					trace("not in %s", bundles[i]);
					// try next
				}
			}
		} catch (Exception e) {
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
		for (Enumeration<?> e = suite.tests(); e.hasMoreElements();) {
			Test test = (Test) e.nextElement();

			if (test instanceof JUnit4TestAdapter) {
				list.add(test);

				for (Test t : ((JUnit4TestAdapter) test).getTests()) {
					if (t instanceof TestSuite) {
						realCount += flatten(list, (TestSuite) t);
					} else {
						list.add(t);
						realCount++;
					}
				}
				continue;
			}

			list.add(test);
			if (test instanceof TestSuite) {
				realCount += flatten(list, (TestSuite) test);
			} else {
				realCount++;
			}
		}
		return realCount;
	}

	private void add(List<TestReporter> reporters, TestResult result, TestReporter rp) {
		reporters.add(rp);
		result.addListener(rp);
	}

	static public String replace(String source, String symbol, String replace) {
		StringBuilder sb = new StringBuilder(source);
		int n = sb.indexOf(symbol, 0);
		while (n > 0) {
			sb.replace(n, n + symbol.length(), replace);
			n = n - symbol.length() + replace.length();
			n = sb.indexOf(replace, n);
		}
		return sb.toString();
	}

	boolean isTrace() {
		return trace;
	}

	public void trace(String msg, Object... objects) {
		if (isTrace()) {
			message("# ", msg, objects);
		}
	}

	void message(String prefix, String string, Object... objects) {
		Throwable e = null;

		StringBuilder sb = new StringBuilder();
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
								Throwable t = e = (Throwable) o;
								for (Throwable cause; (t instanceof InvocationTargetException)
									&& ((cause = t.getCause()) != null);) {
									t = cause;
								}
								sb.append(t.getMessage());
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
		if (e != null)
			e.printStackTrace(out);
	}

	public void error(String msg, Object... objects) {
		message("! ", msg, objects);
	}

	/**
	 * Running a test from the command line
	 *
	 * @param args
	 */

	public static void main(String args[]) {
		System.out.println("args " + Arrays.toString(args));
	}
}
