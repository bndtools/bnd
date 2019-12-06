package aQute.tester.testbase;

import static aQute.junit.constants.TesterConstants.TESTER_CONTINUOUS;
import static aQute.junit.constants.TesterConstants.TESTER_DIR;
import static aQute.junit.constants.TesterConstants.TESTER_NAMES;
import static aQute.junit.constants.TesterConstants.TESTER_PORT;
import static aQute.junit.constants.TesterConstants.TESTER_SEPARATETHREAD;
import static aQute.junit.constants.TesterConstants.TESTER_TRACE;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jdt.internal.junit.model.ITestRunListener2;
import org.eclipse.jdt.internal.junit.model.RemoteTestRunnerClient;
import org.junit.After;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.launchpad.BundleSpecBuilder;
import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.tester.test.utils.TestBundler;
import aQute.tester.test.utils.TestRunData;
import aQute.tester.test.utils.TestRunDataAssert;
import aQute.tester.test.utils.TestRunListener;
import aQute.tester.testclasses.JUnit3Test;
import aQute.tester.testclasses.JUnit4Test;
import aQute.tester.testclasses.With1Error1Failure;
import aQute.tester.testclasses.With2Failures;

// Because we're not in the same project as aQute.junit.TesterConstants and its bundle-private.
public abstract class AbstractActivatorTest extends SoftAssertions {

	static final String		BND_TEST_THREAD	= "bnd Runtime Test Bundle";

	private final String	activatorClass;
	private final String	tester;

	protected TestBundler	testBundler;

	private boolean			DEBUG			= true;

	@AfterEach
	public void after() {
		assertAll();
	}

	protected AbstractActivatorTest(String activatorClass, String tester) {
		this.activatorClass = activatorClass;
		this.tester = tester;
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
	class ExitCheck extends SecurityManager {
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

	protected LaunchpadBuilder	builder;
	protected Launchpad			lp;
	SecurityManager				oldManager;
	Path						tmpDir;
	protected int				eclipseJUnitPort;

	protected TestRunDataAssert assertThat(TestRunData a) {
		return proxy(TestRunDataAssert.class, TestRunData.class, a);
	}

	protected TestInfo	info;
	protected String	name;

	protected File getTmpDir() {
		return tmpDir.toFile();
	}

	protected LaunchpadBuilder setTmpDir() throws IOException {
		// Create tmp dir for test reports to go into as the default is in the
		// project root, which can mess up build dependencies.
		return builder.set(TESTER_DIR, getTmpDir().getAbsolutePath());
	}

	@BeforeEach
	public void setUp(TestInfo info) throws Exception {
		this.info = info;
		Method testMethod = info.getTestMethod()
			.get();
		name = getClass().getName() + "/" + testMethod.getName();
		tmpDir = Paths.get("generated/tmp/test", name)
			.toAbsolutePath();
		IO.delete(tmpDir);
		IO.mkdirs(tmpDir);

		builder = new LaunchpadBuilder();
		builder.bndrun(tester + ".bndrun")
			.excludeExport("aQute.tester.bundle.*")
			.excludeExport("org.junit*")
			.excludeExport("junit.*");
		setTmpDir();
		if (DEBUG) {
			builder.debug()
				.set(TESTER_TRACE, "true");
		}
		lp = null;
		oldManager = System.getSecurityManager();
		System.setSecurityManager(new ExitCheck());
	}

	protected BundleSpecBuilder bundle() {
		return testBundler.bundleWithEE();
	}

	@After
	public void tearDown() {
		System.setSecurityManager(oldManager);
		IO.close(lp);
		IO.close(builder);
	}

	@Test
	public void start_withNoSeparateThreadProp_runsInMainThread() {
		runTests(0, JUnit3Test.class);
		assertThat(testBundler.getCurrentThread(JUnit3Test.class)).as("thread")
			.isSameAs(Thread.currentThread());
	}

	@Test
	public void start_withSeparateThreadPropFalse_startsInMainThread() {
		builder.set(TESTER_SEPARATETHREAD, "false");
		runTests(0, JUnit3Test.class);
		assertThat(testBundler.getCurrentThread(JUnit3Test.class)).as("thread")
			.isSameAs(Thread.currentThread());
	}

	// Gets a handle on the Bnd test thread, if it exists.
	// Try to build some resilience into this test to avoid
	// race conditions when the new test starts.
	private Thread getBndTestThread() throws InterruptedException {
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

	@Test
	public void start_withSeparateThreadProp_startsInNewThread() throws Exception {
		builder.set(TESTER_SEPARATETHREAD, "true");
		createLP();
		addTesterBundle();

		final Thread bndThread = getBndTestThread();

		// Don't assert softly, since if we can't find this thread we can't do
		// the other tests.
		Assertions.assertThat(bndThread)
			.as("thread started")
			.isNotNull()
			.isNotSameAs(Thread.currentThread());

		assertThat(lp.getService(Runnable.class)).as("runnable")
			.isEmpty();

		final AtomicReference<Throwable> exception = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		bndThread.setUncaughtExceptionHandler((thread, e) -> {
			exception.set(e);
			latch.countDown();
		});

		// Can't start the test bundle until after the exception handler
		// is in place to ensure we're ready to catch System.exit().
		addTestBundle(JUnit3Test.class);

		// Wait for the exception handler to catch the exit.
		assertThat(latch.await(20000, TimeUnit.MILLISECONDS)).as("wait for exit")
			.isTrue();

		assertThat(exception.get()).as("exited")
			.isInstanceOf(ExitCode.class);

		if (!(exception.get() instanceof ExitCode)) {
			return;
		}

		final ExitCode ee = (ExitCode) exception.get();

		assertThat(ee.exitCode).as("exitCode")
			.isZero();

		final Thread currentThread = testBundler.getCurrentThread(JUnit3Test.class);
		assertThat(currentThread).as("exec thread")
			.isNotNull()
			.isNotSameAs(Thread.currentThread())
			.isSameAs(bndThread);
	}

	protected ExitCode runTests(int expectedExit, Class<?>... classes) {
		final ExitCode exitCode = runTests(classes);
		assertThat(exitCode.exitCode).as("exitCode")
			.isEqualTo(expectedExit);
		return exitCode;
	}

	protected Bundle		testBundle;
	protected List<Bundle>	testBundles	= new ArrayList<>(10);
	protected Bundle		testerBundle;

	protected interface Callback {
		void run() throws Exception;
	}

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

	@Test
	public void multipleMixedTests_areAllRun() {
		final ExitCode exitCode = runTests(JUnit3Test.class, JUnit4Test.class);

		assertThat(exitCode.exitCode).as("exit code")
			.isZero();
		assertThat(testBundler.getCurrentThread(JUnit3Test.class)).as("junit3")
			.isSameAs(Thread.currentThread());
		assertThat(testBundler.getCurrentThread(JUnit4Test.class)).as("junit4")
			.isSameAs(Thread.currentThread());
	}

	@Test
	public void multipleTests_acrossMultipleBundles_areAllRun() {
		final ExitCode exitCode = runTests(new Class<?>[] {
			JUnit3Test.class
		}, new Class<?>[] {
			JUnit4Test.class
		});

		assertThat(exitCode.exitCode).as("exit code")
			.isZero();
		assertThat(testBundler.getCurrentThread(JUnit3Test.class)).as("junit3")
			.isSameAs(Thread.currentThread());
		assertThat(testBundler.getCurrentThread(JUnit4Test.class)).as("junit4")
			.isSameAs(Thread.currentThread());
	}

	@Test
	public void testerNames_isHonouredByTester() {
		builder.set(TESTER_NAMES, With2Failures.class.getName() + "," + JUnit4Test.class.getName() + ":theOther");
		runTests(2, JUnit3Test.class, JUnit4Test.class, With2Failures.class);
		assertThat(testBundler.getCurrentThread(JUnit3Test.class)).as("JUnit3 thread")
			.isNull();
		assertThat(testBundler.getFlag(JUnit4Test.class, "theOtherFlag")).as("theOther")
			.isTrue();
		assertThat(testBundler.getCurrentThread(JUnit4Test.class)).as("JUnit4 thread")
			.isNull();
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

	@Test
	public void whenNoTestBundles_waitForTestBundle_thenRunAndExit() throws Exception {
		runTesterAndWait();
		addTestBundle(JUnit4Test.class, With2Failures.class);
		runThread.join(10000);
		assertThat(testBundler.getCurrentThread(JUnit4Test.class)).as("thread:after")
			.isSameAs(runThread);
		assertThat(testBundler.getStatic(JUnit4Test.class, AtomicBoolean.class, "theOtherFlag")).as("otherFlag:after")
			.isTrue();
		assertExitCode(2);
	}

	public void assertExitCode(int exitCode) {
		if (uncaught.get() instanceof ExitCode) {
			assertThat(((ExitCode) uncaught.get()).exitCode).as("exitCode")
				.isEqualTo(exitCode);
		} else {
			failBecauseExceptionWasNotThrown(ExitCode.class);
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

	Thread						runThread;
	AtomicReference<Throwable>	uncaught	= new AtomicReference<>();

	@Test
	public void testerContinuous_runsTestsContinuously() {
		builder.set(TESTER_CONTINUOUS, "true");
		runTesterAndWait();
		Bundle old4Bundle = addTestBundle(JUnit4Test.class);
		waitForTesterToWait();
		assertThat(testBundler.getCurrentThread(JUnit4Test.class)).as("junit4")
			.isSameAs(runThread);
		addTestBundle(JUnit3Test.class);
		waitForTesterToWait();
		assertThat(testBundler.getCurrentThread(JUnit3Test.class)).as("junit3")
			.isSameAs(runThread);
		addTestBundle(JUnit4Test.class);
		waitForTesterToWait();
		assertThat(testBundler.getCurrentThread(JUnit4Test.class)).as("junit4 take 2")
			.isSameAs(runThread);
		assertThat(testBundler.getBundleOf(JUnit4Test.class)).as("different bundle")
			.isNotSameAs(old4Bundle);
	}

	public static List<Node> asList(NodeList n) {
		return n.getLength() == 0 ? Collections.<Node> emptyList() : new NodeListWrapper(n);
	}

	static final class NodeListWrapper extends AbstractList<Node> implements RandomAccess {
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

	@Test
	public void exitCode_countsErrorsAndFailures() {
		final ExitCode exitCode = runTests(JUnit4Test.class, With2Failures.class, With1Error1Failure.class);
		assertThat(exitCode.exitCode).isEqualTo(4);
	}

	protected TestRunData runTestsEclipse(Callback postCreateCallback, Class<?>... tests) {
		return runTestsEclipse(postCreateCallback, new Class<?>[][] {
			tests
		});
	}

	RemoteTestRunnerClient client;

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

	// Copied from org.eclipse.jdt.launching.SocketUtil
	public static int findFreePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't get port for test", e);
		}
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
}
