package aQute.tester.testbase;

import static aQute.junit.constants.TesterConstants.TESTER_CONTINUOUS;
import static aQute.junit.constants.TesterConstants.TESTER_NAMES;
import static aQute.junit.constants.TesterConstants.TESTER_SEPARATETHREAD;
import static aQute.junit.constants.TesterConstants.TESTER_TRACE;

import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.framework.Bundle;

import aQute.launchpad.LaunchpadBuilder;
import aQute.lib.io.IO;
import aQute.tester.testclasses.JUnit3Test;
import aQute.tester.testclasses.JUnit4Test;
import aQute.tester.testclasses.With1Error1Failure;
import aQute.tester.testclasses.With2Failures;
import aQute.tester.testclasses.junit.platform.JUnit4ContainerError;
import aQute.tester.testclasses.junit.platform.JUnit4ContainerFailure;

// Because we're not in the same project as aQute.junit.TesterConstants and its bundle-private.
public abstract class AbstractActivatorCommonTest extends AbstractActivatorTest {

	protected AbstractActivatorCommonTest(String activatorClass, String tester) {
		super(activatorClass, tester);
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

	@Test
	public void exitCode_countsErrorsAndFailures() {
		final ExitCode exitCode = runTests(JUnit4Test.class, With2Failures.class, With1Error1Failure.class);
		assertThat(exitCode.exitCode).isEqualTo(4);
	}

	@Test
	public void exitCode_countsContainerErrorsAndFailures() {
		runTests(2, JUnit4ContainerFailure.class, JUnit4ContainerError.class);
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

	@AfterEach
	public void tearDown() {
		System.setSecurityManager(oldManager);
		IO.close(lp);
		IO.close(builder);
	}
}
