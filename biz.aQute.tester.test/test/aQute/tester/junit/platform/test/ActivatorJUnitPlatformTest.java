package aQute.tester.junit.platform.test;

import static aQute.junit.constants.TesterConstants.TESTER_CONTINUOUS;
import static aQute.junit.constants.TesterConstants.TESTER_CONTROLPORT;
import static aQute.junit.constants.TesterConstants.TESTER_NAMES;
import static aQute.junit.constants.TesterConstants.TESTER_PORT;
import static aQute.junit.constants.TesterConstants.TESTER_TRACE;
import static aQute.junit.constants.TesterConstants.TESTER_UNRESOLVED;
import static aQute.tester.test.utils.TestRunData.nameOf;
import static org.eclipse.jdt.internal.junit.model.ITestRunListener2.STATUS_FAILURE;

import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.assertj.core.api.Assertions;
import org.junit.AssumptionViolatedException;
import org.junit.ComparisonFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.launcher.TestExecutionListener;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;
import org.opentest4j.TestAbortedException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.w3c.dom.Document;
import org.xmlunit.assertj.XmlAssert;

import aQute.launchpad.BundleBuilder;
import aQute.launchpad.LaunchpadBuilder;
import aQute.lib.io.IO;
import aQute.lib.xml.XML;
import aQute.tester.test.assertions.CustomAssertionError;
import aQute.tester.test.assertions.internal.CustomAssertionFailedError;
import aQute.tester.test.assertions.internal.CustomJUnit3ComparisonFailure;
import aQute.tester.test.assertions.internal.CustomJUnit4ComparisonFailure;
import aQute.tester.test.assertions.internal.CustomMultipleFailuresError;
import aQute.tester.test.utils.TestEntry;
import aQute.tester.test.utils.TestFailure;
import aQute.tester.test.utils.TestRunData;
import aQute.tester.test.utils.TestRunListener;
import aQute.tester.testbase.AbstractActivatorCommonTest;
import aQute.tester.testbase.AbstractActivatorTest;
import aQute.tester.testclasses.JUnit3Test;
import aQute.tester.testclasses.JUnit4Test;
import aQute.tester.testclasses.With1Error1Failure;
import aQute.tester.testclasses.With2Failures;
import aQute.tester.testclasses.junit.platform.JUnit3ComparisonTest;
import aQute.tester.testclasses.junit.platform.JUnit4AbortTest;
import aQute.tester.testclasses.junit.platform.JUnit4ComparisonTest;
import aQute.tester.testclasses.junit.platform.JUnit4Skipper;
import aQute.tester.testclasses.junit.platform.JUnit5AbortTest;
import aQute.tester.testclasses.junit.platform.JUnit5ContainerError;
import aQute.tester.testclasses.junit.platform.JUnit5ContainerFailure;
import aQute.tester.testclasses.junit.platform.JUnit5ContainerSkipped;
import aQute.tester.testclasses.junit.platform.JUnit5ContainerSkippedWithCustomDisplayName;
import aQute.tester.testclasses.junit.platform.JUnit5DisplayNameTest;
import aQute.tester.testclasses.junit.platform.JUnit5ParameterizedTest;
import aQute.tester.testclasses.junit.platform.JUnit5SimpleComparisonTest;
import aQute.tester.testclasses.junit.platform.JUnit5Skipper;
import aQute.tester.testclasses.junit.platform.JUnit5Test;
import aQute.tester.testclasses.junit.platform.JUnitComparisonSubclassTest;
import aQute.tester.testclasses.junit.platform.JUnitMixedComparisonTest;
import aQute.tester.testclasses.junit.platform.Mixed35Test;
import aQute.tester.testclasses.junit.platform.Mixed45Test;
import aQute.tester.testclasses.junit.platform.ParameterizedTesterNamesTest;

public class ActivatorJUnitPlatformTest extends AbstractActivatorCommonTest {
	public ActivatorJUnitPlatformTest() {
		super("aQute.tester.junit.platform.Activator", "biz.aQute.tester.junit-platform");
	}

	@Override
	protected void createLP() {
		builder.usingClassLoader(SERVICELOADER_MASK);
		super.createLP();
	}

	@Test
	public void multipleMixedTests_areAllRun_withJupiterTest() {
		final ExitCode exitCode = runTests(JUnit3Test.class, JUnit4Test.class, JUnit5Test.class);

		assertThat(exitCode.exitCode).as("exit code")
			.isZero();
		assertThat(testBundler.getCurrentThread(JUnit3Test.class)).as("junit3")
			.isSameAs(Thread.currentThread());
		assertThat(testBundler.getCurrentThread(JUnit4Test.class)).as("junit4")
			.isSameAs(Thread.currentThread());
		assertThat(testBundler.getCurrentThread(JUnit5Test.class)).as("junit5")
			.isSameAs(Thread.currentThread());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void multipleMixedTests_inASingleTestCase_areAllRun() {
		final ExitCode exitCode = runTests(Mixed35Test.class, Mixed45Test.class);

		assertThat(exitCode.exitCode).as("exit code")
			.isZero();
		assertThat(testBundler.getStatic(Mixed35Test.class, Set.class, "methods")).as("Mixed JUnit 3 & 5")
			.containsExactlyInAnyOrder("testJUnit3", "junit5Test");
		assertThat(testBundler.getStatic(Mixed45Test.class, Set.class, "methods")).as("Mixed JUnit 4 & 5")
			.containsExactlyInAnyOrder("junit4Test", "junit5Test");
	}

	@Test
	public void eclipseListener_reportsResults_acrossMultipleBundles() throws InterruptedException {
		Class<?>[][] tests = {
			{
				With2Failures.class, JUnit4Test.class
			}, {
				With1Error1Failure.class, JUnit5Test.class
			}
		};

		TestRunData result = runTestsEclipse(tests);

		assertThat(result.getTestCount()).as("testCount")
			.isEqualTo(9);

		// @formatter:off
		assertThat(result.getNameMap()
			.keySet()).as("executed")
			.contains(
					nameOf(With2Failures.class),
					nameOf(With2Failures.class, "test1"),
					nameOf(With2Failures.class, "test2"),
					nameOf(With2Failures.class, "test3"),
					nameOf(JUnit4Test.class),
					nameOf(JUnit4Test.class, "somethingElse"),
					nameOf(With1Error1Failure.class),
					nameOf(With1Error1Failure.class, "test1"),
					nameOf(With1Error1Failure.class, "test2"),
					nameOf(With1Error1Failure.class, "test3"),
					nameOf(JUnit5Test.class, "somethingElseAgain"),
					nameOf(testBundles.get(0)),
					nameOf(testBundles.get(1))
					);

		assertThat(result).as("result")
			.hasFailedTest(With2Failures.class, "test1", AssertionError.class)
			.hasSuccessfulTest(With2Failures.class, "test2")
			.hasFailedTest(With2Failures.class, "test3", CustomAssertionError.class)
			.hasSuccessfulTest(JUnit4Test.class, "somethingElse")
			.hasErroredTest(With1Error1Failure.class, "test1", RuntimeException.class)
			.hasSuccessfulTest(With1Error1Failure.class, "test2")
			.hasFailedTest(With1Error1Failure.class, "test3", AssertionError.class)
			.hasSuccessfulTest(JUnit5Test.class, "somethingElseAgain")
			;
		// @formatter:on
	}

	@Test
	public void eclipseListener_worksInContinuousMode_withControlSocket() throws Exception {
		try (ServerSocket controlSock = new ServerSocket(0)) {
			controlSock.setSoTimeout(10000);
			int controlPort = controlSock.getLocalPort();
			builder.set("launch.services", "true")
				.set(TESTER_CONTINUOUS, "true")
				// This value should be ignored
				.set(TESTER_PORT, Integer.toString(controlPort - 2))
				.set(TESTER_CONTROLPORT, Integer.toString(controlPort));
			createLP();
			addTestBundle(With2Failures.class, JUnit4Test.class);

			runTester();
			try (Socket sock = controlSock.accept()) {
				InputStream inStr = sock.getInputStream();
				DataOutputStream outStr = new DataOutputStream(sock.getOutputStream());

				readWithTimeout(inStr);
				TestRunListener listener = startEclipseJUnitListener();
				outStr.writeInt(eclipseJUnitPort);
				outStr.flush();
				listener.waitForClientToFinish(10000);

				TestRunData result = listener.getLatestRunData();

				if (result == null) {
					Assertions.fail("Result was null" + listener);
					return;
				}

				assertThat(result.getTestCount()).as("testCount")
					.isEqualTo(5);

				// @formatter:off
				assertThat(result.getNameMap()
					.keySet()).as("executed")
				.contains(
					nameOf(With2Failures.class),
					nameOf(With2Failures.class, "test1"),
					nameOf(With2Failures.class, "test2"),
					nameOf(With2Failures.class, "test3"),
					nameOf(JUnit4Test.class),
					nameOf(JUnit4Test.class, "somethingElse"),
					nameOf(testBundles.get(0))
					);

				assertThat(result).as("result")
					.hasFailedTest(With2Failures.class, "test1", AssertionError.class)
					.hasSuccessfulTest(With2Failures.class, "test2")
					.hasFailedTest(With2Failures.class, "test3", CustomAssertionError.class)
					.hasSuccessfulTest(JUnit4Test.class, "somethingElse")
					;
				// @formatter:on
				addTestBundle(With1Error1Failure.class, JUnit5Test.class);

				readWithTimeout(inStr);
				listener = startEclipseJUnitListener();
				outStr.writeInt(eclipseJUnitPort);
				outStr.flush();
				listener.waitForClientToFinish(10000);

				result = listener.getLatestRunData();

				if (result == null) {
					Assertions.fail("Eclipse didn't capture output from the second run");
					return;
				}
				int i = 2;
				assertThat(result.getTestCount()).as("testCount:" + i)
					.isEqualTo(4);

				// @formatter:off
				assertThat(result.getNameMap()
					.keySet()).as("executed:2")
				.contains(
					nameOf(With1Error1Failure.class),
					nameOf(With1Error1Failure.class, "test1"),
					nameOf(With1Error1Failure.class, "test2"),
					nameOf(With1Error1Failure.class, "test3"),
					nameOf(JUnit5Test.class, "somethingElseAgain"),
					nameOf(testBundles.get(1))
					);

				assertThat(result).as("result:2")
					.hasErroredTest(With1Error1Failure.class, "test1", RuntimeException.class)
					.hasSuccessfulTest(With1Error1Failure.class, "test2")
					.hasFailedTest(With1Error1Failure.class, "test3", AssertionError.class)
					.hasSuccessfulTest(JUnit5Test.class, "somethingElseAgain")
					;
					// @formatter:on
			}
		}
	}

	@Test
	public void eclipseListener_reportsComparisonFailures() throws InterruptedException {
		Class<?>[] tests = {
			JUnit3ComparisonTest.class, JUnit4ComparisonTest.class, JUnit5SimpleComparisonTest.class, JUnit5Test.class,
			JUnitMixedComparisonTest.class
		};

		TestRunData result = runTestsEclipse(tests);

		final String[] order = {
			"1", "2", "3.1", "3.2", "3.4", "4"
		};

		// @formatter:off
		assertThat(result).as("result")
			.hasFailedTest(JUnit3ComparisonTest.class, "testComparisonFailure", junit.framework.ComparisonFailure.class, "expected", "actual")
			.hasFailedTest(JUnit4ComparisonTest.class, "comparisonFailure", ComparisonFailure.class, "expected", "actual")
			.hasFailedTest(JUnit5SimpleComparisonTest.class, "somethingElseThatFailed", AssertionFailedError.class, "expected", "actual")
			.hasFailedTest(JUnitMixedComparisonTest.class, "multipleComparisonFailure", MultipleFailuresError.class,
				Stream.of(order).map(x -> "expected" + x).collect(Collectors.joining("\n\n")),
				Stream.of(order).map(x -> "actual" + x).collect(Collectors.joining("\n\n"))
				)
			;
		// @formatter:on
		TestFailure f = result.getFailureByName(nameOf(JUnit5SimpleComparisonTest.class, "emptyComparisonFailure"));
		assertThat(f).as("emptyComparisonFailure")
			.isNotNull();
		if (f != null) {
			assertThat(f.status).as("emptyComparisonFailure:status")
				.isEqualTo(STATUS_FAILURE);
			assertThat(f.expected).as("emptyComparisonFailure:expected")
				.isNull();
			assertThat(f.actual).as("emptyComparisonFailure:actual")
				.isNull();
		}
	}

	static class ActivatorJUnitPlatformTest_WithCustomAssertions extends AbstractActivatorTest {

		public ActivatorJUnitPlatformTest_WithCustomAssertions() {
			super("aQute.tester.junit.platform.Activator", "biz.aQute.tester.junit-platform");
		}

		// Needed to override this method so that we could create a bundle
		// with the custom assertions in it that the test bundle could
		// reference. If we reference them via the framework classloader,
		// they will link to a different version of the JUnit 3/4 classes
		// and so the code-under-test will not recognise the assertions
		// as being subclasses of the JUnit 3/4 classes.
		@Override
		protected void createLP() {
			builder.usingClassLoader(SERVICELOADER_MASK);
			builder.excludeExport(CustomMultipleFailuresError.class.getPackage()
				.getName());
			super.createLP();

			BundleBuilder bb = lp.bundle();
			Stream
				.of(CustomAssertionFailedError.class, CustomJUnit4ComparisonFailure.class,
					CustomJUnit3ComparisonFailure.class, CustomMultipleFailuresError.class)
				.forEach(clazz -> {
					bb.addResourceWithCopy(clazz);
					bb.exportPackage(clazz.getPackage()
						.getName());
				});
			bb.start();
		}

		@Test
		public void eclipseListener_reportsComparisonFailures_forSubclasses() throws InterruptedException {
			Class<?>[] tests = {
				JUnitComparisonSubclassTest.class
			};
			TestRunData result = runTestsEclipse(tests);

			final String[] order = {
				"1", "2", "3.1", "3.2", "3.4", "4"
			};

			// @formatter:off
			assertThat(result).as("result")
				.hasFailedTest(JUnitComparisonSubclassTest.class, "multipleComparisonFailure", CustomMultipleFailuresError.class,
					Stream.of(order).map(x -> "expected" + x).collect(Collectors.joining("\n\n")),
					Stream.of(order).map(x -> "actual" + x).collect(Collectors.joining("\n\n"))
					)
				;
			// @formatter:on
		}

		@BeforeEach
		public void setUp(TestInfo info) throws Exception {
			this.info = info;
			builder = new LaunchpadBuilder();
			builder.bndrun(tester + ".bndrun")
				.excludeExport("aQute.tester.bundle.*")
				.excludeExport("org.junit*")
				.excludeExport("junit.*");
			setResultsDir();
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

	@Test
	public void eclipseListener_reportsParameterizedTests() throws InterruptedException {
		TestRunData result = runTestsEclipse(JUnit5ParameterizedTest.class);

		TestEntry methodTest = result.getTest(JUnit5ParameterizedTest.class, "parameterizedMethod");

		if (methodTest == null) {
			Assertions.fail(
				"Couldn't find method test entry for " + nameOf(JUnit5ParameterizedTest.class, "parameterizedMethod"));
			return;
		}

		assertThat(methodTest.parameterTypes).as("parameterTypes")
			.containsExactly("java.lang.String", "float");

		List<TestEntry> parameterTests = result.getChildrenOf(methodTest.testId)
			.stream()
			.map(x -> result.getById(x))
			.collect(Collectors.toList());

		assertThat(parameterTests.stream()
			.map(x -> x.testName)).as("testNames")
				.allMatch(x -> x.equals(nameOf(JUnit5ParameterizedTest.class, "parameterizedMethod")));
		assertThat(parameterTests.stream()).as("dynamic")
			.allMatch(x -> x.isDynamicTest);
		assertThat(parameterTests.stream()
			.map(x -> x.displayName)).as("displayNames")
				.containsExactlyInAnyOrder("1 ==> param: 'one', param2: 1.0", "2 ==> param: 'two', param2: 2.0",
					"3 ==> param: 'three', param2: 3.0", "4 ==> param: 'four', param2: 4.0",
					"5 ==> param: 'five', param2: 5.0");

		Optional<TestEntry> test4 = parameterTests.stream()
			.filter(x -> x.displayName.startsWith("4 ==>"))
			.findFirst();
		if (!test4.isPresent()) {
			check(() -> Assertions.fail("Couldn't find test result for parameter 4"));
		} else {
			assertThat(parameterTests.stream()
				.filter(x -> result.getFailure(x.testId) != null)).as("failures")
					.containsExactly(test4.get());
		}
	}

	@Test
	public void eclipseListener_reportsMisconfiguredParameterizedTests() throws InterruptedException {
		TestRunData result = runTestsEclipse(JUnit5ParameterizedTest.class);

		TestEntry methodTest = result.getTest(JUnit5ParameterizedTest.class, "misconfiguredMethod");

		if (methodTest == null) {
			Assertions.fail(
				"Couldn't find method test entry for " + nameOf(JUnit5ParameterizedTest.class, "misconfiguredMethod"));
			return;
		}

		TestFailure failure = result.getFailure(methodTest.testId);
		if (failure == null) {
			check(() -> Assertions.fail("Expecting method:\n%s\nto have failed", methodTest));
		} else {
			assertThat(failure.trace).as("trace")
				.startsWith("org.junit.platform.commons.JUnitException: Could not find factory method [unknownMethod]");
		}
	}

	@Test
	public void eclipseListener_reportsCustomNames_withOddCharacters() throws InterruptedException {
		TestRunData result = runTestsEclipse(JUnit5DisplayNameTest.class);

		final String[] methodList = {
			"test1", "testWithNonASCII", "testWithNonLatin"
		};
		final String[] displayList = {
			// "Test 1", "Prüfung 2", "Δοκιμή 3"
			"Test 1", "Pr\u00fcfung 2", "\u0394\u03bf\u03ba\u03b9\u03bc\u03ae 3"
		};

		for (int i = 0; i < methodList.length; i++) {
			final String method = methodList[i];
			final String display = displayList[i];
			TestEntry methodTest = result.getTest(JUnit5DisplayNameTest.class, method);
			if (methodTest == null) {
				check(() -> Assertions
					.fail("Couldn't find method test entry for " + nameOf(JUnit5ParameterizedTest.class, method)));
				continue;
			}
			assertThat(methodTest.displayName).as(String.format("[%d] %s", i, method))
				.isEqualTo(display);
		}
	}

	@Test
	public void eclipseListener_reportsSkippedTests() throws InterruptedException {
		TestRunData result = runTestsEclipse(JUnit5Skipper.class, JUnit5ContainerSkipped.class,
			JUnit5ContainerSkippedWithCustomDisplayName.class, JUnit4Skipper.class);

		assertThat(result).as("result")
			.hasSkippedTest(JUnit5Skipper.class, "disabledTest", "with custom message")
			.hasSkippedTest(JUnit5ContainerSkipped.class, "with another message")
			.hasSkippedTest(JUnit5ContainerSkipped.class, "disabledTest2",
				"ancestor \"JUnit5ContainerSkipped\" was skipped")
			.hasSkippedTest(JUnit5ContainerSkipped.class, "disabledTest3",
				"ancestor \"JUnit5ContainerSkipped\" was skipped")
			.hasSkippedTest(JUnit5ContainerSkippedWithCustomDisplayName.class, "with a third message")
			.hasSkippedTest(JUnit5ContainerSkippedWithCustomDisplayName.class, "disabledTest2",
				"ancestor \"Skipper Class\" was skipped")
			.hasSkippedTest(JUnit5ContainerSkippedWithCustomDisplayName.class, "disabledTest3",
				"ancestor \"Skipper Class\" was skipped")
			.hasSkippedTest(JUnit4Skipper.class, "disabledTest", "This is a test");
	}

	@Test
	public void eclipseListener_reportsAbortedTests() throws InterruptedException {
		TestRunData result = runTestsEclipse(JUnit5AbortTest.class, JUnit4AbortTest.class);

		assertThat(result).as("result")
			.hasAbortedTest(JUnit5AbortTest.class, "abortedTest",
				new TestAbortedException("Assumption failed: I just can't go on"))
			.hasAbortedTest(JUnit4AbortTest.class, "abortedTest",
				new AssumptionViolatedException("Let's get outta here"));
	}

	@Test
	public void eclipseListener_handlesNoEnginesGracefully() throws Exception {
		try (LaunchpadBuilder builder = new LaunchpadBuilder()) {
			IO.close(this.builder);
			builder.bndrun("no-engines.bndrun")
				.excludeExport("org.junit*")
				.excludeExport("junit*");
			this.builder = builder;
			setResultsDir();
			TestRunData result = runTestsEclipse(JUnit5AbortTest.class, JUnit4AbortTest.class);
			assertThat(result).hasErroredTest("Initialization Error",
				new JUnitException("Couldn't find any registered TestEngines"));
		}
	}

	@Test
	public void eclipseListener_handlesNoJUnit3Gracefully() throws Exception {
		builder.excludeExport("junit.framework");
		Class<?>[] tests = {
			JUnit4ComparisonTest.class, JUnit5Test.class, JUnit5SimpleComparisonTest.class
		};

		TestRunData result = runTestsEclipse(tests);

		assertThat(result).as("result")
			.hasFailedTest(JUnit5SimpleComparisonTest.class, "somethingElseThatFailed", AssertionFailedError.class,
				"expected", "actual");
		TestFailure f = result.getFailureByName(nameOf(JUnit5SimpleComparisonTest.class, "emptyComparisonFailure"));
		assertThat(f).as("emptyComparisonFailure")
			.isNotNull();
		if (f != null) {
			assertThat(f.status).as("emptyComparisonFailure:status")
				.isEqualTo(STATUS_FAILURE);
			assertThat(f.expected).as("emptyComparisonFailure:expected")
				.isNull();
			assertThat(f.actual).as("emptyComparisonFailure:actual")
				.isNull();
		}
	}

	@Test
	public void eclipseListener_handlesNoJUnit4Gracefully() throws Exception {
		try (LaunchpadBuilder builder = new LaunchpadBuilder()) {
			IO.close(this.builder);
			builder.debug();
			builder.bndrun("no-vintage-engine.bndrun")
				.excludeExport("aQute.tester.bundle.*")
				.excludeExport("org.junit*")
				.excludeExport("junit*");
			this.builder = builder;
			setResultsDir();
			Class<?>[] tests = {
				JUnit3ComparisonTest.class, JUnit5Test.class, JUnit5SimpleComparisonTest.class
			};

			TestRunData result = runTestsEclipse(tests);
			assertThat(result).as("result")
				.hasFailedTest(JUnit5SimpleComparisonTest.class, "somethingElseThatFailed", AssertionFailedError.class,
					"expected", "actual");
			TestFailure f = result.getFailureByName(nameOf(JUnit5SimpleComparisonTest.class, "emptyComparisonFailure"));
			assertThat(f).as("emptyComparisonFailure")
				.isNotNull();
			if (f != null) {
				assertThat(f.status).as("emptyComparisonFailure:status")
					.isEqualTo(STATUS_FAILURE);
				assertThat(f.expected).as("emptyComparisonFailure:expected")
					.isNull();
				assertThat(f.actual).as("emptyComparisonFailure:actual")
					.isNull();
			}
		}
	}

	@Test
	public void testerUnresolvedTrue_isPassedThroughToBundleEngine() {
		builder.set(TESTER_UNRESOLVED, "true");
		AtomicReference<Bundle> bundleRef = new AtomicReference<>();
		TestRunData result = runTestsEclipse(() -> {
			bundleRef.set(bundle().importPackage("some.unknown.package")
				.install());
		}, JUnit3Test.class, JUnit4Test.class);

		assertThat(result).hasSuccessfulTest("Unresolved bundles");
	}

	@Test
	public void testerUnresolvedFalse_isPassedThroughToBundleEngine() {
		builder.set(TESTER_UNRESOLVED, "false");
		AtomicReference<Bundle> bundleRef = new AtomicReference<>();
		TestRunData result = runTestsEclipse(() -> {
			bundleRef.set(bundle().importPackage("some.unknown.package")
				.install());
		}, JUnit3Test.class, JUnit4Test.class);

		assertThat(result.getNameMap()
			.get("Unresolved bundles")).isNull();
	}

	@Test
	public void testerNames_withParameters_isHonouredByTester() {
		builder.set(TESTER_NAMES,
			"'" + ParameterizedTesterNamesTest.class.getName() + ":parameterizedMethod(java.lang.String,float)'");
		runTests(0, JUnit3Test.class, JUnit4Test.class, ParameterizedTesterNamesTest.class);
		assertThat(testBundler.getCurrentThread(JUnit3Test.class)).as("JUnit3 thread")
			.isNull();
		assertThat(testBundler.getCurrentThread(JUnit4Test.class)).as("JUnit4 thread")
			.isNull();
		assertThat(testBundler.getInteger("countParameterized", ParameterizedTesterNamesTest.class))
			.as("parameterizedMethod")
			.isEqualTo(5);
		assertThat(testBundler.getInteger("countOther", ParameterizedTesterNamesTest.class)).as("countOther")
			.isEqualTo(0);
	}

	@Test
	public void xmlReporter_generatesCompleteXmlFile() throws Exception {
		final ExitCode exitCode = runTests(JUnit3Test.class, With1Error1Failure.class, With2Failures.class);

		final String fileName = "TEST-" + testBundle.getSymbolicName() + "-" + testBundle.getVersion() + ".xml";
		File xmlFile = getResultsDir().resolve(fileName)
			.toFile();
		Assertions.assertThat(xmlFile)
			.as("xmlFile")
			.exists();
		AtomicReference<Document> docContainer = new AtomicReference<>();

		Assertions.assertThatCode(() -> {
			DocumentBuilderFactory dbFactory = XML.newDocumentBuilderFactory();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			docContainer.set(dBuilder.parse(xmlFile));
		})
			.doesNotThrowAnyException();

		Document doc = docContainer.get();

		XmlAssert.assertThat(doc)
			.nodesByXPath("/testsuite")
			.hasSize(1);
		XmlAssert.assertThat(doc)
			.nodesByXPath(testSuite() + "/testcase")
			.hasSize(7);
		XmlAssert.assertThat(doc)
			.nodesByXPath(testCase(JUnit3Test.class, "testSomething"))
			.hasSize(1);
		XmlAssert.assertThat(doc)
			.nodesByXPath(testCaseError(With1Error1Failure.class, "test1", RuntimeException.class))
			.hasSize(1);
		XmlAssert.assertThat(doc)
			.nodesByXPath(testCase(With1Error1Failure.class, "test2"))
			.hasSize(1);
		XmlAssert.assertThat(doc)
			.nodesByXPath(testCaseFailure(With1Error1Failure.class, "test3", AssertionError.class))
			.hasSize(1);
		XmlAssert.assertThat(doc)
			.nodesByXPath(testCaseFailure(With2Failures.class, "test1", AssertionError.class))
			.hasSize(1);
		XmlAssert.assertThat(doc)
			.nodesByXPath(testCase(With2Failures.class, "test2"))
			.hasSize(1);
		XmlAssert.assertThat(doc)
			.nodesByXPath(testCaseFailure(With2Failures.class, "test3", CustomAssertionError.class))
			.hasSize(1);
	}

	// Unlike JUnit 4, Jupiter skips the tests when the parent container
	// fails and does not report the children as test failures.
	@Test
	public void exitCode_countsJupiterContainerErrorsAndFailures() {
		runTests(2, JUnit5ContainerFailure.class, JUnit5ContainerError.class);
	}

	public static class ListenerGenerator implements BundleActivator, InvocationHandler {

		List<String> invocations;

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			if (method.getDeclaringClass()
				.equals(TestExecutionListener.class)) {
				invocations.add(method.getName());
			}
			return null;
		}

		ServiceRegistration<TestExecutionListener> reg;

		@Override
		public void start(BundleContext context) throws Exception {
			TestExecutionListener listener = (TestExecutionListener) Proxy
				.newProxyInstance(ListenerGenerator.class.getClassLoader(), new Class<?>[] {
					TestExecutionListener.class
				}, this);
			invocations = new ArrayList<>();
			reg = context.registerService(TestExecutionListener.class, listener, null);
		}

		@Override
		public void stop(BundleContext context) throws Exception {
			reg.unregister();
		}
	}

	@Test
	public void testListeners_areInvoked() throws Exception {
		AtomicReference<Bundle> bRef = new AtomicReference<>();

		final ExitCode exitCode = runTests(() -> {
			Bundle b = lp.bundle()
				.addResourceWithCopy(ListenerGenerator.class)
				.bundleActivator(ListenerGenerator.class.getName())
				.importPackage("org.junit.platform.engine")
				.importPackage("org.junit.platform.engine.reporting")
				.importPackage("*")
				.start();
			bRef.set(b);
		}, JUnit3Test.class);

		assertThat(exitCode.exitCode).as("exit code")
			.isZero();
		BundleContext context = bRef.get()
			.getBundleContext();
		ServiceReference<?> ref = context.getServiceReference(TestExecutionListener.class);
		Object listener = context.getService(ref);
		Object invocationHandler = Proxy.getInvocationHandler(listener);
		Class<?> listenerGenerator = invocationHandler.getClass();
		Field f = listenerGenerator.getDeclaredField("invocations");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		List<String> methods = (List<String>) f.get(invocationHandler);

		assertThat(methods).containsExactly("testPlanExecutionStarted", "executionStarted", "executionStarted",
			"executionStarted", "executionStarted", "executionStarted", "executionFinished", "executionFinished",
			"executionFinished", "executionFinished", "executionFinished", "testPlanExecutionFinished");
	}
}
