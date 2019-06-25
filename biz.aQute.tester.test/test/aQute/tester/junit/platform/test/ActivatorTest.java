package aQute.tester.junit.platform.test;

import static aQute.junit.constants.TesterConstants.TESTER_UNRESOLVED;
import static aQute.tester.test.utils.TestRunData.nameOf;
import static org.eclipse.jdt.internal.junit.model.ITestRunListener2.STATUS_FAILURE;

import java.io.File;
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
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;
import org.opentest4j.TestAbortedException;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.xmlunit.assertj.XmlAssert;

import aQute.launchpad.LaunchpadBuilder;
import aQute.lib.io.IO;
import aQute.tester.test.assertions.CustomAssertionError;
import aQute.tester.test.utils.ServiceLoaderMask;
import aQute.tester.test.utils.TestEntry;
import aQute.tester.test.utils.TestFailure;
import aQute.tester.test.utils.TestRunData;
import aQute.tester.test.utils.TestRunListener;
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
import aQute.tester.testclasses.junit.platform.JUnit5ContainerSkipped;
import aQute.tester.testclasses.junit.platform.JUnit5ContainerSkippedWithCustomDisplayName;
import aQute.tester.testclasses.junit.platform.JUnit5DisplayNameTest;
import aQute.tester.testclasses.junit.platform.JUnit5ParameterizedTest;
import aQute.tester.testclasses.junit.platform.JUnit5SimpleComparisonTest;
import aQute.tester.testclasses.junit.platform.JUnit5Skipper;
import aQute.tester.testclasses.junit.platform.JUnit5Test;
import aQute.tester.testclasses.junit.platform.JUnitMixedComparisonTest;
import aQute.tester.testclasses.junit.platform.Mixed35Test;
import aQute.tester.testclasses.junit.platform.Mixed45Test;

public class ActivatorTest extends AbstractActivatorTest {
	public ActivatorTest() {
		super("aQute.tester.junit.platform.Activator", "biz.aQute.tester.junit-platform");
	}

	// We have the Jupiter engine on the classpath so that the tests will run.
	// This classloader will hide it from the framework-under-test.
	static final ClassLoader SERVICELOADER_MASK = new ServiceLoaderMask();

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

	@Test
	public void eclipseListener_reportsParameterizedTests() throws InterruptedException {
		TestRunData result = runTestsEclipse(JUnit5ParameterizedTest.class);

		TestEntry methodTest = result.getTest(JUnit5ParameterizedTest.class, "parameterizedMethod");

		if (methodTest == null) {
			fail("Couldn't find method test entry for " + nameOf(JUnit5ParameterizedTest.class, "parameterizedMethod"));
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
			fail("Couldn't find test result for parameter 4");
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
			fail("Couldn't find method test entry for " + nameOf(JUnit5ParameterizedTest.class, "misconfiguredMethod"));
			return;
		}

		TestFailure failure = result.getFailure(methodTest.testId);
		if (failure == null) {
			fail("Expecting method:\n%s\nto have failed", methodTest);
		} else {
			assertThat(failure.trace).as("trace")
				.startsWith("org.junit.platform.commons.JUnitException: Could not find method: unknownMethod");
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
				fail("Couldn't find method test entry for " + nameOf(JUnit5ParameterizedTest.class, method));
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
			setTmpDir();
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
			setTmpDir();
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
	public void xmlReporter_generatesCompleteXmlFile() throws Exception {
		final ExitCode exitCode = runTests(JUnit3Test.class, With1Error1Failure.class, With2Failures.class);

		final String fileName = "TEST-" + testBundle.getSymbolicName() + "-" + testBundle.getVersion() + ".xml";
		File xmlFile = new File(getTmpDir(), fileName);
		Assertions.assertThat(xmlFile)
			.as("xmlFile")
			.exists();
		AtomicReference<Document> docContainer = new AtomicReference<>();

		Assertions.assertThatCode(() -> {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
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
}
