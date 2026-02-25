package aQute.tester.junit.platform;

import static aQute.junit.constants.TesterConstants.TESTER_CONTINUOUS;
import static aQute.junit.constants.TesterConstants.TESTER_CONTROLPORT;
import static aQute.junit.constants.TesterConstants.TESTER_CUSTOMSERVICE;
import static aQute.junit.constants.TesterConstants.TESTER_DIR;
import static aQute.junit.constants.TesterConstants.TESTER_NAMES;
import static aQute.junit.constants.TesterConstants.TESTER_PORT;
import static aQute.junit.constants.TesterConstants.TESTER_SEPARATETHREAD;
import static aQute.junit.constants.TesterConstants.TESTER_TRACE;
import static aQute.junit.constants.TesterConstants.TESTER_UNRESOLVED;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.service.command.Descriptor;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherConstants;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.LoggingListener;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;

import aQute.junit.system.BndSystem;
import aQute.tester.bundle.engine.BundleEngine;
import aQute.tester.bundle.engine.discovery.BundleSelector;
import aQute.tester.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener;
import aQute.tester.junit.platform.utils.BundleUtils;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator, Runnable {
	String									unresolved;
	Launcher								launcher;
	BundleContext							context;
	volatile boolean						active;
	boolean									continuous	= false;
	boolean									trace		= false;
	PrintStream								out			= System.out;
	JUnitEclipseListener					jUnitEclipseListener;
	volatile Thread							thread;
	private File							reportDir;
	private SummaryGeneratingListener		summary;
	private List<TestExecutionListener>		listeners	= new ArrayList<>();
	final BlockingDeque<DiscoverySelector>	queue		= new LinkedBlockingDeque<>();

	public Activator() {}

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		active = true;

		boolean isCustomService = Boolean.valueOf(context.getProperty(TESTER_CUSTOMSERVICE));
		if (!Boolean.valueOf(context.getProperty(TESTER_SEPARATETHREAD))
			&& (isCustomService || Boolean.valueOf(context.getProperty("launch.services")))) {
			// can't register services on mini framework
			Hashtable<String, String> ht = new Hashtable<>();
			if (isCustomService) {
				ht.put("junit.tester", "true");
			}
			else {
				ht.put("main.thread", "true");
			}
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
		final ClassLoader contextClassLoader = thread.getContextClassLoader();
		thread.setContextClassLoader(LauncherFactory.class.getClassLoader());
		try {

			launcher = LauncherFactory.create(LauncherConfig.builder()
				.enableTestEngineAutoRegistration(false)
				.addTestEngines(new BundleEngine())
				.build());

		} finally {
			thread.setContextClassLoader(contextClassLoader);
		}
		List<TestExecutionListener> listenerList = new ArrayList<>();

		setTesterNames(context.getProperty(TESTER_NAMES));

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
				listeners.add(jUnitEclipseListener);
			} catch (Exception e) {
				System.err.println(
					"Cannot create link Eclipse JUnit control on port " + port + " (rerunIDE: " + rerunIDE + ')');
				BndSystem.exit(254);
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
			error("Could not create directory %s", reportDir);
		} else {
			trace("using %s, path: %s", reportDir, reportDir.toPath());
			try {
				listeners.add(new LegacyXmlReportGeneratingListener(reportDir.toPath(), new PrintWriter(System.err)));
			} catch (Exception e) {
				error("Error trying to create xml reporter: %s", e);
			}
		}

		listeners.add(LoggingListener.forBiConsumer(this::trace));
		summary = new SummaryGeneratingListener();
		listeners.add(summary);
		listeners.add(new TestExecutionListener() {
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
						trace("", "TEST %s <<< ABORTED: %s", testName(testIdentifier),
							testExecutionResult.getThrowable()
								.orElse(null));
				}
			}

			@Override
			public void executionSkipped(TestIdentifier testIdentifier, String reason) {
				trace("", "TEST %s <<< SKIPPED", testName(testIdentifier));
			}
		});
		trace("automatic testing of all bundles with " + aQute.bnd.osgi.Constants.TESTCASES + " header");
		try {
			automatic();
		} catch (IOException e) {
			// ignore
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

	Set<String> testCases;

	void setTesterNames(String testerNames) {
		trace("test cases filter set to %s", testerNames);
		if (testerNames == null || testerNames.trim()
			.isEmpty()) {
			testCases = null;
		} else {
			testCases = BundleUtils.testCases(testerNames)
				.collect(toSet());
		}
	}

	Stream<DiscoverySelector> toSelectors(Bundle bundle) {
		if (testCases == null) {
			return Stream.of(BundleSelector.selectBundle(bundle));
		}
		Set<String> classNames = BundleUtils.testCases(bundle)
			.collect(toSet());

		return testCases.stream()
			.map(testcase -> {
				int index = testcase.indexOf('#');
				if (index < 0) {
					return classNames.contains(testcase) ? selectClass(testcase) : null;
				}
				String className = testcase.substring(0, index);
				return classNames.contains(className) ? selectMethod(testcase) : null;
			})
			.filter(Objects::nonNull);
	}

	void automatic() throws IOException {
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
					.flatMap(Activator.this::toSelectors)
					.forEach(queue::offerLast);
			}

			@Override
			public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
				processed.remove(object);
			}
		};
		tracker.open();

		if (Boolean.valueOf(context.getProperty("launch.services")) && continuous) {
			GogoCommandlet cmd = new GogoCommandlet();
			Hashtable<String, Object> properties = new Hashtable<>();
			properties.put("osgi.command.function", new String[] {
				"runTests", "getTesterNames", "setTesterNames"
			});
			properties.put("osgi.command.scope", "tester");

			trace("starting gogo commandlet");
			context.registerService(Object.class, cmd, properties);
		}

		trace("starting queue");
		long result = 0;
		long timeout = continuous ? Long.MAX_VALUE : 5000;
		while (active()) {
			try {
				List<DiscoverySelector> selectors = new ArrayList<>();

				//
				// it would be more logical to check for an empty queue here
				// and !continuous but many tests cases assume that we
				// will wait for at least 1 test case.
				//

				for (DiscoverySelector selector = queue.pollFirst(timeout, TimeUnit.MILLISECONDS); //
					selector != null; //
					selector = queue.pollFirst(100, TimeUnit.MILLISECONDS)) {
					selectors.add(selector);
					queue.drainTo(selectors);
				}
				if (!selectors.isEmpty()) {
					LauncherDiscoveryRequest testRequest = buildRequest(selectors);
					trace("test will run");
					result += test(testRequest);
					trace("test ran");
				}
				if (queue.isEmpty() && !continuous) {
					trace("queue %s", queue);
					BndSystem.exit((int) result);
				}
			} catch (InterruptedException e) {
				trace("tests bundle queue interrupted");
				thread.interrupt();
				break;
			} catch (Exception e) {
				error("Not sure what happened anymore %s", e);
				BndSystem.exit(254);
			}
		}
	}

	LauncherDiscoveryRequest buildRequest(List<? extends DiscoverySelector> selectors) {
		Optional<String> captureStdout = Optional
			.ofNullable(context.getProperty(LauncherConstants.CAPTURE_STDOUT_PROPERTY_NAME));
		Optional<String> captureStderr = Optional
			.ofNullable(context.getProperty(LauncherConstants.CAPTURE_STDERR_PROPERTY_NAME));
		return LauncherDiscoveryRequestBuilder.request()
			.configurationParameter(BundleEngine.CHECK_UNRESOLVED, unresolved)
			.configurationParameter(LauncherConstants.CAPTURE_STDOUT_PROPERTY_NAME, captureStdout.orElse("true"))
			.configurationParameter(LauncherConstants.CAPTURE_STDERR_PROPERTY_NAME, captureStderr.orElse("true"))
			.selectors(selectors)
			.build();
	}

	static DiscoverySelector toSelector(String testcase) {
		if (testcase.startsWith(":")) {
			return BundleSelector.selectBundle(testcase.substring(1));
		}
		if (testcase.indexOf('#') < 0) {
			return selectClass(testcase);
		}
		return selectMethod(testcase);
	}

	class GogoCommandlet {
		@Descriptor("runs the specified tests")
		public void runTests(
			@Descriptor("The tests to run. If no tests are specified, it will automatically run all tests (using the tester.names filter if specified).\n"
				+ "Three syntaxes are available:\n"
				+ " 1. \":bundle.symbolic.name\" - run all tests in the specified bundle\n"
				+ " 2. \"fully.qualified.ClassName\" - run all tests in the specified class\n"
				+ " 3. \"fully.qualified.ClassName#methodName\" - run the specified test method")
			String... tests) {
			if (tests != null && tests.length > 0) {
				Stream.of(tests)
					.map(Activator::toSelector)
					.forEach(queue::offerLast);
			} else {
				Stream.of(context.getBundles())
					.filter(BundleUtils::hasTests)
					.flatMap(Activator.this::toSelectors)
					.forEach(queue::offerLast);
			}
		}

		@Descriptor("retrieves the tester.names filter")
		public String getTesterNames() {
			return Activator.this.testCases == null ? null
				: Activator.this.testCases.stream()
					.collect(Collectors.joining(", "));
		}

		@Descriptor("sets the tester.names filter")
		public void setTesterNames(@Descriptor("The new tester.names filter to apply to automatic testing")
		String... tests) {
			Activator.this.setTesterNames(tests == null ? null
				: Stream.of(tests)
					.collect(Collectors.joining(",")));
			System.err.println("testCases: " + testCases);
		}
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
			ServiceTracker<TestExecutionListener, TestExecutionListener> track = new ServiceTracker<>(context,
				TestExecutionListener.class, null);
			track.open();
			final TestExecutionSummary execSummary;
			try {
				TestExecutionListener[] listenerArray = Stream
					.concat(listeners.stream(), Arrays.stream(track.getServices(new TestExecutionListener[0])))
					.toArray(TestExecutionListener[]::new);
				launcher.execute(testRequest, listenerArray);
			} catch (Throwable t) {
				trace("%s", t);
			} finally {
				execSummary = summary.getSummary();
				track.close();
				if (execSummary != null) {
					trace(null, () -> {
						CharArrayWriter sw = new CharArrayWriter();
						execSummary.printTo(new PrintWriter(sw));
						return sw.toString();
					});
				} else {
					trace("No summary available");
				}
			}

			return (execSummary != null) ? execSummary.getTotalFailureCount() : -1;
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

	private void trace(Throwable t, Supplier<String> string) {
		if (isTrace()) {
			message("# ", t, string.get());
		}
	}

	void message(String prefix, String string, Object... objects) {
		Throwable e = null;

		StringBuilder sb = new StringBuilder();
		int n = 0;
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
					case 'n' :
						sb.append(System.lineSeparator());
						break;
					default :
						sb.append(c);
						break;
				}
			} else {
				sb.append(c);
			}
		}
		while (n < objects.length) { // check excess objects for a throwable
			Object o = objects[n++];
			if (o instanceof Throwable) {
				e = (Throwable) o;
			}
		}
		message(prefix, e, sb.toString());
	}

	private void message(String prefix, Throwable t, String string) {
		synchronized (out) { // avoid interleaving output
			out.print(prefix);
			out.print(string);
			out.print(System.lineSeparator());
			if (t != null) {
				t.printStackTrace(out);
			}
			out.flush();
		}
	}

	public void error(String msg, Object... objects) {
		message("! ", msg, objects);
	}
}
