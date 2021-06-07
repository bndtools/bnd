package aQute.tester.testbase;

import static aQute.junit.constants.TesterConstants.TESTER_DIR;
import static aQute.junit.constants.TesterConstants.TESTER_PORT;
import static aQute.junit.constants.TesterConstants.TESTER_TRACE;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.assertj.core.api.Assert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.StandardSoftAssertionsProvider;
import org.eclipse.jdt.internal.junit.model.ITestRunListener2;
import org.eclipse.jdt.internal.junit.model.RemoteTestRunnerClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.bnd.exceptions.Exceptions;
import aQute.launchpad.BundleSpecBuilder;
import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.tester.test.utils.ServiceLoaderMask;
import aQute.tester.test.utils.TestBundler;
import aQute.tester.test.utils.TestRunData;
import aQute.tester.test.utils.TestRunDataAssert;
import aQute.tester.test.utils.TestRunListener;

public class AbstractActivatorTest implements StandardSoftAssertionsProvider {

	static final String					BND_TEST_THREAD		= "bnd Runtime Test Bundle";
	protected final String				activatorClass;
	protected final String				tester;
	protected TestBundler				testBundler;
	protected boolean					DEBUG				= true;
	protected LaunchpadBuilder			builder;

	// We have the Jupiter engine on the classpath so that the tests will run.
	// This classloader will hide it from the framework-under-test if necessary.
	static protected final ClassLoader	SERVICELOADER_MASK	= new ServiceLoaderMask();

	@AfterEach
	public void after() {
		assertAll();
	}

	protected Launchpad			lp;
	protected SecurityManager	oldManager;
	private final Path			resultsDir	= Paths.get("generated", "test-reports", "test")
		.toAbsolutePath();
	protected int				eclipseJUnitPort;
	protected TestInfo			info;

	protected TestRunDataAssert assertThat(TestRunData a) {
		return proxy(TestRunDataAssert.class, TestRunData.class, a);
	}

	protected SoftAssertions softly;

	@BeforeEach
	void beforeEach() {
		softly = new SoftAssertions();
	}

	protected String	name;
	protected Bundle	testBundle;

	protected Path getResultsDir() {
		return resultsDir;
	}

	protected LaunchpadBuilder setResultsDir() throws IOException {
		// Sets report dir for test reports to go into as the default is in the
		// project root, which can mess up build dependencies.
		return builder.set(TESTER_DIR, getResultsDir().toString());
	}

	protected BundleSpecBuilder bundle() {
		return testBundler.bundleWithEE();
	}

	protected Thread getBndTestThread() throws InterruptedException {
		final long endTime = System.currentTimeMillis() + 30000;

		while (System.currentTimeMillis() < endTime) {
			for (Thread thread : Thread.getAllStackTraces()
				.keySet()) {
				if (thread.getName()
					.equals(BND_TEST_THREAD)) {
					return thread;
				}
			}
			Thread.sleep(1000);
		}
		return null;
	}

	protected ExitCode runTests(int expectedExit, Class<?>... classes) {
		final ExitCode exitCode = runTests(classes);
		assertThat(exitCode.exitCode).as("exitCode")
			.isEqualTo(expectedExit);
		return exitCode;
	}

	protected List<Bundle>	testBundles	= new ArrayList<>(10);
	protected Bundle		testerBundle;

	protected interface Callback {
		void run() throws Exception;
	}

	protected Thread runThread;

	protected ExitCode runTests(Class<?>... classes) {
		return runTests((Callback) null, classes);
	}

	protected ExitCode runTests(Class<?>[]... bundles) {
		return runTests((Callback) null, bundles);
	}

	protected ExitCode runTests(Callback postCreateCallback, Class<?>... classes) {
		return runTests(postCreateCallback, new Class<?>[][] {
			classes
		});
	}

	protected ExitCode runTests(Callback postCreateCallback, Class<?>[]... bundles) {
		builder.set("launch.services", "true")
			.set(TESTER_TRACE, "true");
		createLP();
		Stream.of(bundles)
			.forEach(this::addTestBundle);

		addTesterBundle();

		final Optional<Runnable> oR = lp.getService(Runnable.class);

		// Use a hard assertion here to short-circuit the test if no runnable
		// found.
		Assertions.assertThat(oR)
			.as("runnable")
			.isPresent();

		final Runnable r = oR.get();

		assertThat(r.getClass()
			.getName()).as("runnable")
				.isEqualTo(activatorClass);

		if (postCreateCallback != null) {
			try {
				postCreateCallback.run();
			} catch (Exception e) {
				Exceptions.duck(e);
			}
		}

		try {
			r.run();
			throw new AssertionError("Expecting run() to call System.exit(), but it didn't");
		} catch (ExitCode e) {
			return e;
		}
	}

	public void runTesterAndWait() {
		runTester();
		// This is to avoid race conditions and make sure that the
		// tester thread has actually gotten to the point where it is
		// waiting for new bundles.
		waitForTesterToWait();
	}

	public void runTester() {
		if (lp == null) {
			builder.set("launch.services", "true");
			createLP();
		}

		addTesterBundle();

		final Optional<Runnable> oR = lp.getService(Runnable.class);
		Assertions.assertThat(oR)
			.as("runnable")
			.isPresent();
		final Runnable r = oR.get();

		runThread = new Thread(r, name);
		runThread.setUncaughtExceptionHandler((t, x) -> uncaught.set(x));
		runThread.start();
	}

	public void assertExitCode(int exitCode) {
		if (uncaught.get() instanceof ExitCode) {
			assertThat(((ExitCode) uncaught.get()).exitCode).as("exitCode")
				.isEqualTo(exitCode);
		} else {
			check(() -> Assertions.failBecauseExceptionWasNotThrown(ExitCode.class));
		}
	}

	public void waitForTesterToWait() {
		final long waitTime = 10000;
		final long endTime = System.currentTimeMillis() + waitTime;
		int waitCount = 0;
		try {
			OUTER: while (true) {
				Thread.sleep(100);
				final Thread.State state = runThread.getState();
				switch (state) {
					case TERMINATED :
					case TIMED_WAITING :
					case WAITING :
						if (waitCount++ > 5) {
							break OUTER;
						}
						break;
					default :
						waitCount = 0;
						break;
				}
				if (System.currentTimeMillis() > endTime) {
					throw new InterruptedException("Thread still hasn't entered wait state after " + waitTime + "ms");
				}
			}
		} catch (InterruptedException e) {
			throw Exceptions.duck(e);
		}
		// Check that it hasn't terminated.
		assertThat(runThread.getState()).as("runThread")
			.isIn(Thread.State.WAITING, Thread.State.TIMED_WAITING);
	}

	AtomicReference<Throwable> uncaught = new AtomicReference<>();

	public static List<Node> asList(NodeList n) {
		return n.getLength() == 0 ? Collections.<Node> emptyList() : new NodeListWrapper(n);
	}

	protected static final class NodeListWrapper extends AbstractList<Node> implements RandomAccess {
		private final NodeList list;

		NodeListWrapper(NodeList l) {
			list = l;
		}

		@Override
		public Node get(int index) {
			return list.item(index);
		}

		@Override
		public int size() {
			return list.getLength();
		}
	}

	RemoteTestRunnerClient client;

	protected String testSuite() {
		return String.format("/testsuite[contains(@name, '%s')]", testBundle.getSymbolicName());
	}

	protected String testCase(Class<?> testClass, String method) {
		return String.format("%s/testcase[@classname='%s' and @name='%s' and count(error) = 0]", testSuite(),
			testClass.getName(), method);
	}

	protected String testCaseError(Class<?> testClass, String method, Class<? extends Throwable> error) {
		return String.format("%s/testcase[@classname='%s' and @name='%s' and count(error) = 1]/error[@type='%s']",
			testSuite(), testClass.getName(), method, error.getName());
	}

	protected String testCaseFailure(Class<?> testClass, String method, Class<? extends AssertionError> failure) {
		return String.format("%s/testcase[@classname='%s' and @name='%s' and count(failure) = 1]/failure[@type='%s']",
			testSuite(), testClass.getName(), method, failure.getName());
	}

	protected TestRunData runTestsEclipse(Callback postCreateCallback, Class<?>... tests) {
		return runTestsEclipse(postCreateCallback, new Class<?>[][] {
			tests
		});
	}

	public static int findFreePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't get port for test", e);
		}
	}

	protected TestRunListener startEclipseJUnitListener() {
		int port = findFreePort();
		client = new RemoteTestRunnerClient();
		TestRunListener listener = new TestRunListener(this, DEBUG);
		client.startListening(new ITestRunListener2[] {
			listener
		}, port);

		eclipseJUnitPort = port;
		return listener;
	}

	protected TestRunData runTestsEclipse(Callback postCreateCallback, Class<?>[]... testBundles) {
		if (lp != null) {
			throw new IllegalStateException("Framework already started");
		}
		TestRunListener listener = startEclipseJUnitListener();
		builder.set(TESTER_PORT, Integer.toString(eclipseJUnitPort));
		final long startTime = System.currentTimeMillis();
		try {
			runTests(postCreateCallback, testBundles);
			if (listener.getLatestRunData() != null) {
				listener.getLatestRunData()
					.setActualRunTime(System.currentTimeMillis() - startTime);
			}
			listener.waitForClientToFinish(10000);
			listener.checkRunTime();
			return listener.getLatestRunData();
		} catch (InterruptedException e) {
			throw Exceptions.duck(e);
		} finally {
			client.stopWaiting();
		}
	}

	protected TestRunData runTestsEclipse(Class<?>... tests) throws InterruptedException {
		return runTestsEclipse(null, tests);
	}

	protected TestRunData runTestsEclipse(Class<?>[]... testBundles) throws InterruptedException {
		return runTestsEclipse(null, testBundles);
	}

	// This extends Error rather than SecurityException so that it can traverse
	// the catch(Exception) statements in the code-under-test.
	protected class ExitCode extends Error {
		private static final long	serialVersionUID	= -1498037177123939551L;
		public final int			exitCode;
		final StackTraceElement[]	stack;

		public ExitCode(int exitCode, StackTraceElement[] stack) {
			this.exitCode = exitCode;
			this.stack = stack;
		}
	}

	// To catch calls to System.exit() calls within bnd.aQute.junit that
	// otherwise cause the entire test harness to exit.
	public class ExitCheck extends SecurityManager {
		@Override
		public void checkPermission(Permission perm) {}

		@Override
		public void checkPermission(Permission perm, Object context) {}

		@Override
		public void checkExit(int status) {
			// Because the activator might have been loaded in a different
			// classloader, need to check names and not objects.
			if (Stream.of(getClassContext())
				.anyMatch(x -> x.getName()
					.equals(activatorClass))) {
				throw new ExitCode(status, Thread.currentThread()
					.getStackTrace());
			}
			super.checkExit(status);
		}
	}

	public AbstractActivatorTest(String activatorClass, String tester) {
		this.activatorClass = activatorClass;
		this.tester = tester;
	}

	protected void createLP() {
		lp = builder.create(name, info.getTestClass()
			.get()
			.getSimpleName());
		testBundler = new TestBundler(lp);
	}

	protected Bundle addTestBundle(Class<?>... testClasses) {
		testBundle = testBundler.buildTestBundle(testClasses)
			.start();
		testBundles.add(testBundle);
		return testBundle;
	}

	protected void addTesterBundle() {
		lp.bundles(tester)
			.forEach(t -> {
				if (testerBundle != null) {
					throw new IllegalStateException("Attempted to load tester bundle twice");
				}
				testerBundle = t;
				try {
					t.start();
				} catch (BundleException e) {
					Assertions.fail("Couldn't start tester bundle", e);
				}
			});
	}

	protected void readWithTimeout(InputStream inStr) throws Exception {
		long endTime = System.currentTimeMillis() + 10000;
		int available;
		while ((available = inStr.available()) == 0 && System.currentTimeMillis() < endTime) {
			Thread.sleep(10);
		}
		if (available == 0) {
			Assertions.fail("Timeout waiting for data");
		}
		assertThat(available).as("control signal")
			.isEqualTo(1);
		int value = inStr.read();
		assertThat(value).as("control value")
			.isNotEqualTo(-1);
	}

	@Override
	public void assertAll() {
		softly.assertAll();
	}

	@Override
	public boolean wasSuccess() {
		return softly.wasSuccess();
	}

	@Override
	public void collectAssertionError(AssertionError error) {
		softly.collectAssertionError(error);
	}

	@Override
	public List<AssertionError> assertionErrorsCollected() {
		return softly.assertionErrorsCollected();
	}

	@Override
	public <SELF extends Assert<? extends SELF, ? extends ACTUAL>, ACTUAL> SELF proxy(Class<SELF> assertClass,
		Class<ACTUAL> actualClass, ACTUAL actual) {
		return softly.proxy(assertClass, actualClass, actual);
	}

	@Override
	public void succeeded() {
		softly.succeeded();
	}
}
