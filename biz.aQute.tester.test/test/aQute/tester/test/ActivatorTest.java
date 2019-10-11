package aQute.tester.test;

import static aQute.junit.constants.TesterConstants.TESTER_UNRESOLVED;
import static aQute.tester.test.utils.TestRunData.nameOf;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.xmlunit.assertj.XmlAssert;

import aQute.junit.UnresolvedTester;
import aQute.tester.test.assertions.CustomAssertionError;
import aQute.tester.test.utils.TestRunData;
import aQute.tester.testbase.AbstractActivatorTest;
import aQute.tester.testclasses.JUnit3Test;
import aQute.tester.testclasses.JUnit4Test;
import aQute.tester.testclasses.With1Error1Failure;
import aQute.tester.testclasses.With2Failures;
import aQute.tester.testclasses.tester.JUnit3NonStaticBC;
import aQute.tester.testclasses.tester.JUnit3NonStaticFieldBC;
import aQute.tester.testclasses.tester.JUnit3StaticFieldBC;
import junit.framework.AssertionFailedError;

// This suppression is because we're not building in the same project.
@SuppressWarnings("restriction")
public class ActivatorTest extends AbstractActivatorTest {

	public ActivatorTest() {
		super("aQute.junit.Activator", "biz.aQute.tester");
	}

	@Test
	public void eclipseReporter_reportsResults() throws InterruptedException {
		TestRunData result = runTestsEclipse(With2Failures.class, JUnit4Test.class, With1Error1Failure.class);

		assertThat(result.getTestCount()).as("testCount")
			.isEqualTo(8);

		// @formatter:off
		assertThat(result.getNameMap()
			.keySet()).as("executed")
			.containsExactlyInAnyOrder(
					nameOf(With2Failures.class),
					nameOf(With2Failures.class, "test1"),
					nameOf(With2Failures.class, "test2"),
					nameOf(With2Failures.class, "test3"),
					nameOf(JUnit4Test.class),
					nameOf(JUnit4Test.class, "somethingElse"),
					nameOf(JUnit4Test.class, "theOther"),
					nameOf(With1Error1Failure.class),
					nameOf(With1Error1Failure.class, "test1"),
					nameOf(With1Error1Failure.class, "test2"),
					nameOf(With1Error1Failure.class, "test3"));

		// Note: in the old Tester, all failures were (incorrectly) reported as errors.
		// This test verifies the actual behaviour, rather than the desired behaviour.
		assertThat(result)
			.hasErroredTest(With2Failures.class, "test1", AssertionError.class)
			.hasSuccessfulTest(With2Failures.class, "test2")
			.hasErroredTest(With2Failures.class, "test3", CustomAssertionError.class)
			.hasSuccessfulTest(JUnit4Test.class, "somethingElse")
			.hasErroredTest(With1Error1Failure.class, "test1", RuntimeException.class)
			.hasSuccessfulTest(With1Error1Failure.class, "test2")
			.hasErroredTest(With1Error1Failure.class, "test3", AssertionError.class)
			;
		// @formatter:on
	}

	// This functionality doesn't work under JUnit 4 for the old tester.
	@Test
	public void run_setsBundleContext_forJUnit3() {
		runTests(0, JUnit3Test.class, JUnit3NonStaticBC.class, JUnit3StaticFieldBC.class, JUnit3NonStaticFieldBC.class);
		assertThat(testBundler.getStatic(JUnit3Test.class, AtomicReference.class, "bundleContext")
			.get()).as("static setBundleContext()")
				.isSameAs(testBundle.getBundleContext());
		assertThat(testBundler.getStatic(JUnit3NonStaticFieldBC.class, AtomicReference.class, "actualBundleContext")
			.get()).as("static setBundleContext() - in method")
				.isSameAs(testBundle.getBundleContext());

		assertThat(testBundler.getBundleContext(JUnit3NonStaticBC.class)).as("nonstatic setBundleContext()")
			.isSameAs(testBundle.getBundleContext());
		assertThat(testBundler.getActualBundleContext(JUnit3NonStaticBC.class))
			.as("nonstatic setBundleContext() - in method")
			.isSameAs(testBundle.getBundleContext());

		assertThat(testBundler.getActualBundleContext(JUnit3StaticFieldBC.class)).as("static context field")
			.isSameAs(testBundle.getBundleContext());
		assertThat(testBundler.getActualBundleContext(JUnit3NonStaticFieldBC.class)).as("nonstatic context field")
			.isSameAs(testBundle.getBundleContext());
	}

	@Test
	public void testerUnresolvedTrue_withUnresolvedBundle_fails() {
		builder.set(TESTER_UNRESOLVED, "true");
		AtomicReference<Bundle> bundleRef = new AtomicReference<>();
		TestRunData result = runTestsEclipse(() -> {
			bundleRef.set(bundle().importPackage("some.unknown.package")
				.install());
		}, JUnit3Test.class, JUnit4Test.class);

		assertThat(result).hasFailedTest(UnresolvedTester.class, "testAllResolved", AssertionFailedError.class);

	}

	@Test
	public void testerUnresolvedFalse_withUnresolvedBundle_doesntFail() {
		builder.set(TESTER_UNRESOLVED, "false");
		AtomicReference<Bundle> bundleRef = new AtomicReference<>();
		TestRunData result = runTestsEclipse(() -> {
			bundleRef.set(bundle().importPackage("some.unknown.package")
				.install());
		}, JUnit3Test.class, JUnit4Test.class);

		assertThat(result.getNameMap()
			.get(UnresolvedTester.class.getName())).isNull();
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
			.nodesByXPath(testCaseError(With1Error1Failure.class, "test3", AssertionError.class))
			.hasSize(1);
		XmlAssert.assertThat(doc)
			.nodesByXPath(testCaseError(With2Failures.class, "test1", AssertionError.class))
			.hasSize(1);
		XmlAssert.assertThat(doc)
			.nodesByXPath(testCase(With2Failures.class, "test2"))
			.hasSize(1);
		XmlAssert.assertThat(doc)
			.nodesByXPath(testCaseError(With2Failures.class, "test3", CustomAssertionError.class))
			.hasSize(1);
	}
}
