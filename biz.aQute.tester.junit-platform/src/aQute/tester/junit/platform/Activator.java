package aQute.tester.junit.platform;

import static aQute.junit.constants.TesterConstants.TESTER_CONTINUOUS;
import static aQute.junit.constants.TesterConstants.TESTER_CONTROLPORT;
import static aQute.junit.constants.TesterConstants.TESTER_DIR;
import static aQute.junit.constants.TesterConstants.TESTER_NAMES;
import static aQute.junit.constants.TesterConstants.TESTER_PORT;
import static aQute.junit.constants.TesterConstants.TESTER_SEPARATETHREAD;
import static aQute.junit.constants.TesterConstants.TESTER_TRACE;
import static aQute.junit.constants.TesterConstants.TESTER_UNRESOLVED;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.LoggingListener;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.BundleTracker;

import aQute.tester.bundle.engine.BundleEngine;
import aQute.tester.bundle.engine.discovery.BundleSelector;
import aQute.tester.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener;
import aQute.tester.junit.platform.utils.BundleUtils;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator, Runnable {
	String								unresolved;
	Launcher							launcher;
	BundleContext						context;
	volatile boolean					active;
	boolean								continuous	= false;
	boolean								trace		= false;
	PrintStream							out			= System.err;
	JUnitEclipseListener				jUnitEclipseListener;
	volatile Thread						thread;
	private File						reportDir;
	private SummaryGeneratingListener	summary;
	private TestExecutionListener[]		listeners;

	public Activator() {}

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		active = true;

		if (!Boolean.valueOf(context.getProperty(TESTER_SEPARATETHREAD))
			&& Boolean.valueOf(context.getProperty("launch.services"))) {
			// can't register services on mini framework
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
		if (jUnitEclipseListener != null)
			jUnitEclipseListener.close();

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

		launcher = LauncherFactory.create(LauncherConfig.builder()
				.enableTestEngineAutoRegistration(false)
				.addTestEngines(new BundleEngine())
				.build());

		List<TestExecutionListener> listenerList = new ArrayList<>();

		String testcases = context.getProperty(TESTER_NAMES);
		trace("test cases %s", testcases);
		int port = -1;
		boolean rerunIDE = false;
		if (context.getProperty(TESTER_CONTROLPORT) != null) {
			port = Integer.parseInt(context.getProperty(TESTER_CONTROLPORT));
			rerunIDE = true;
		} else if (context.getProperty(TESTER_PORT) != null) {
			port = Integer.parseInt(context.getProperty(TESTER_PORT));
		}

		if (port > 0) {
			try {
				trace("using control port %s, rerun IDE?: %s", port, rerunIDE);
				jUnitEclipseListener = new JUnitEclipseListener(port, rerunIDE);
				listenerList.add(jUnitEclipseListener);
			} catch (Exception e) {
				System.err.println(
					"Cannot create link Eclipse JUnit control on port " + port + " (rerunIDE: " + rerunIDE + ')');
				System.exit(254);
			}
		}

		String testerDir = context.getProperty(TESTER_DIR);
		if (testerDir == null)
			testerDir = "testdir";

		reportDir = new File(testerDir);

		//
		// Jenkins does not detect test failures unless reported
		// by JUnit XML output. If we have an unresolved failure
		// we timeout. The following will test if there are any
		// unresolveds and report this as a JUnit failure. It can
		// be disabled with -testunresolved=false
		//
		unresolved = context.getProperty(TESTER_UNRESOLVED);

		trace("run unresolved %s", unresolved);

		if (!reportDir.exists() && !reportDir.mkdirs()) {
			System.err.printf("Could not create directory %s%n", reportDir);
		} else {
			trace("using %s, path: %s", reportDir, reportDir.toPath());
			try {
				listenerList
					.add(new LegacyXmlReportGeneratingListener(reportDir.toPath(), new PrintWriter(System.err)));
			} catch (Exception e) {
				error("Error trying to create xml reporter: %s", e);
			}
		}

		listenerList.add(LoggingListener.forBiConsumer((t, msg) -> {
			if (t == null) {
				trace(msg.get());
			} else {
				trace(msg.get(), t);
			}
		}));
		summary = new SummaryGeneratingListener();
		listenerList.add(summary);
		listenerList.add(new TestExecutionListener() {
			@Override
			public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
				switch (testExecutionResult.getStatus()) {
					case SUCCESSFUL :
						return;
					case FAILED :
						message("", "TEST %s <<< ERROR: %s", testName(testIdentifier),
							testExecutionResult.getThrowable()
								.orElse(null));
						return;
					case ABORTED :
						message("", "TEST %s <<< ABORTED: %s", testName(testIdentifier),
							testExecutionResult.getThrowable()
								.orElse(null));
				}
			}

			@Override
			public void executionSkipped(TestIdentifier testIdentifier, String reason) {
				message("", "TEST %s <<< SKIPPED", testName(testIdentifier));
			}
		});
		listeners = listenerList.toArray(new TestExecutionListener[0]);

		if (testcases == null) {
			trace("automatic testing of all bundles with " + aQute.bnd.osgi.Constants.TESTCASES + " header");
			try {
				automatic();
			} catch (IOException e) {
				// ignore
			}
		} else {
			trace("received names of classes to test %s", testcases);
			try {
				List<DiscoverySelector> testcaseSelectors = BundleUtils.testCases(testcases)
					.map(testcase -> testcase.indexOf('#') < 0 ? selectClass(testcase) : selectMethod(testcase))
					.collect(toList());
				LauncherDiscoveryRequest request = buildRequest(testcaseSelectors);
				long result = test(request);
				System.exit((int) result);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(254);
			}
		}
	}

	String testName(TestIdentifier testIdentifier) {
		return testIdentifier.getSource()
			.map(source -> {
				if (source instanceof ClassSource) {
					ClassSource classSource = (ClassSource) source;

					return classSource.getClassName();
				} else if (source instanceof MethodSource) {
					MethodSource methodSource = (MethodSource) source;

					return Stream
						.of(methodSource.getClassName(), "#", methodSource.getMethodName(),
							Optional.ofNullable(methodSource.getMethodParameterTypes())
								.map(mpt -> "(".concat(mpt)
									.concat(")"))
								.orElse(""))
						.collect(joining());
				}

				return source.toString();
			})
			.orElseGet(testIdentifier::getDisplayName);
	}

	void automatic() throws IOException {
		final BlockingDeque<BundleSelector> queue = new LinkedBlockingDeque<>();

		trace("Opening BundleTracker for finding test bundles");
		BundleTracker<Bundle> tracker = new BundleTracker<Bundle>(context,
			Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE, null) {
			final Set<Bundle> processed = new HashSet<>();

			@Override
			public Bundle addingBundle(Bundle bundle, BundleEvent event) {
				if ((bundle.getState() & Bundle.RESOLVED) != 0) {
					// maybe a fragment
					Bundle host = BundleUtils.getHost(bundle)
						.orElse(bundle);
					if (host != bundle) { // fragment
						if ((host.getState() & (Bundle.STARTING | Bundle.ACTIVE)) != 0) {
							process(Stream.of(bundle));
						}
						return bundle;
					}
					// a RESOLVED host
					return null;
				}
				// must be a host
				process(Stream.of(bundle));
				process(BundleUtils.getFragments(bundle));
				return bundle;
			}

			private void process(Stream<Bundle> stream) {
				stream.filter(processed::add)
					.filter(BundleUtils::hasTests)
					.map(BundleSelector::selectBundle)
					.forEach(queue::offerLast);
			}

			@Override
			public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
				processed.remove(object);
			}
		};
		tracker.open();

		trace("starting queue");
		long result = 0;
		while (active()) {
			try {
				List<BundleSelector> selectors = new ArrayList<>();
				for (BundleSelector selector = queue.takeFirst(); //
					selector != null; //
					selector = queue.pollFirst(100, TimeUnit.MILLISECONDS)) {
					selectors.add(selector);
					queue.drainTo(selectors);
				}
				LauncherDiscoveryRequest testRequest = buildRequest(selectors);
				trace("test will run");
				result += test(testRequest);
				trace("test ran");
				if (queue.isEmpty() && !continuous) {
					trace("queue " + queue);
					System.exit((int) result);
				}
			} catch (InterruptedException e) {
				trace("tests bundle queue interrupted");
				thread.interrupt();
				break;
			} catch (Exception e) {
				error("Not sure what happened anymore %s", e);
				System.exit(254);
			}
		}
	}

	LauncherDiscoveryRequest buildRequest(List<? extends DiscoverySelector> selectors) {
		return LauncherDiscoveryRequestBuilder.request()
			.configurationParameter(BundleEngine.CHECK_UNRESOLVED, unresolved)
			.selectors(selectors)
			.build();
	}

	/**
	 * The main test routine.
	 *
	 * @param bundle The bundle under test or null
	 * @param testnames The names to test
	 * @return # of errors
	 */
	long test(LauncherDiscoveryRequest testRequest) {
		trace("testing request %s", testRequest);
		try {
			try {
				launcher.execute(testRequest, listeners);
			} catch (Throwable t) {
				trace("%s", t);
			} finally {
				summary.getSummary()
					.printTo(new PrintWriter(out));
			}

			return summary.getSummary()
				.getTestsFailedCount();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
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
}
