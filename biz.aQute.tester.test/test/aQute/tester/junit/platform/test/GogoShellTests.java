package aQute.tester.junit.platform.test;

import static aQute.junit.constants.TesterConstants.TESTER_CONTINUOUS;
import static aQute.junit.constants.TesterConstants.TESTER_CONTROLPORT;
import static aQute.junit.constants.TesterConstants.TESTER_PORT;
import static aQute.junit.constants.TesterConstants.TESTER_TRACE;
import static aQute.tester.test.utils.TestRunData.nameOf;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import aQute.launchpad.LaunchpadBuilder;
import aQute.lib.io.IO;
import aQute.tester.test.assertions.CustomAssertionError;
import aQute.tester.test.utils.TestRunData;
import aQute.tester.test.utils.TestRunListener;
import aQute.tester.testbase.AbstractActivatorTest;
import aQute.tester.testclasses.JUnit4Test;
import aQute.tester.testclasses.With1Error1Failure;
import aQute.tester.testclasses.With2Failures;
import aQute.tester.testclasses.junit.platform.JUnit5Test;

@TestInstance(Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public class GogoShellTests extends AbstractActivatorTest {

	ServerSocket		controlSock		= null;
	Socket				sock			= null;
	Object				gogo			= null;
	Method				runTests		= null;
	Method				setTesterNames	= null;
	Method				getTesterNames	= null;
	InputStream			inStr;
	DataOutputStream	outStr;

	public GogoShellTests() {
		super("aQute.tester.junit.platform.Activator", "biz.aQute.tester.junit-platform");
	}

	@BeforeAll
	void beforeAll(TestInfo info) throws Exception {

		this.info = info;
		softly = new SoftAssertions();
		name = getClass().getName();
		builder = new LaunchpadBuilder();
		builder.bndrun(tester + ".bndrun")
			.excludeExport("aQute.tester.bundle.*")
			.excludeExport("org.junit*")
			.excludeExport("junit.*");
		if (DEBUG) {
			builder.debug()
				.set(TESTER_TRACE, "true");
		}
		lp = null;
		oldManager = System.getSecurityManager();
		System.setSecurityManager(new ExitCheck());

		controlSock = new ServerSocket(0);
		controlSock.setSoTimeout(10000);
		int controlPort = controlSock.getLocalPort();
		builder.set("launch.services", "true")
			.set(TESTER_CONTINUOUS, "true")
			// This value should be ignored
			.set(TESTER_PORT, Integer.toString(controlPort - 2))
			.set(TESTER_CONTROLPORT, Integer.toString(controlPort))
			.usingClassLoader(SERVICELOADER_MASK);
		createLP();

		runTester();
		waitForTesterToWait();
		BundleContext frameworkContext = lp.getFramework()
			.getBundleContext();
		Collection<ServiceReference<Object>> gogoRefs = frameworkContext.getServiceReferences(Object.class,
			"(osgi.command.scope=tester)");

		Assertions.assertThat(gogoRefs)
			.as("references")
			.isNotNull();
		assertThat(gogoRefs).as("gogo services")
			.hasSize(1);

		ServiceReference<?> gogoRef = gogoRefs.iterator()
			.next();

		assertThat(gogoRef.getProperty("osgi.command.function")).as("function")
			.asInstanceOf(InstanceOfAssertFactories.array(String[].class))
			.containsExactly("runTests", "getTesterNames", "setTesterNames");

		gogo = frameworkContext.getService(gogoRef);
		Class<?> gogoClass = gogo.getClass();
		runTests = gogoClass.getMethod("runTests", new Class<?>[] {
			String[].class
		});

		runTests.setAccessible(true);

		setTesterNames = gogoClass.getMethod("setTesterNames", new Class<?>[] {
			String[].class
		});
		setTesterNames.setAccessible(true);
		getTesterNames = gogoClass.getMethod("getTesterNames");
		getTesterNames.setAccessible(true);

		addTestBundle(With2Failures.class, JUnit4Test.class);
		addTestBundle(With1Error1Failure.class, JUnit5Test.class);

		sock = controlSock.accept();
		inStr = sock.getInputStream();
		outStr = new DataOutputStream(sock.getOutputStream());

		readWithTimeout(inStr);
		TestRunListener listener = startEclipseJUnitListener();
		outStr.writeInt(eclipseJUnitPort);
		outStr.flush();
		listener.waitForClientToFinish(10000);

		TestRunData result = listener.getLatestRunData();

		if (result == null) {
			// Hard assert to prevent the test from continuing
			Assertions.fail("Result was null" + listener);
		}

		assertAll();
	}

	@AfterAll
	void afterAll() {
		IO.close(sock);
		IO.close(controlSock);
		System.setSecurityManager(oldManager);
		IO.close(lp);
		IO.close(builder);
	}

	@BeforeEach
	public void before() throws Exception {
		setTesterNames(null);
	}

	@Test
	void runTests_forSingleClass_works() throws Exception {
		TestRunData result = gogoRunTests(With2Failures.class.getName());

		if (result == null) {
			Assertions.fail("Eclipse didn't capture output");
			// Unreachable but prevents a compiler warning about null pointer
			// access.
			return;
		}
		assertThat(result.getTestCount()).as("testCount")
			.isEqualTo(3);

		// @formatter:off
		assertThat(result.getNameMap()
			.keySet()).as("executed")
			.contains(
				nameOf(With2Failures.class),
				nameOf(With2Failures.class, "test1"),
				nameOf(With2Failures.class, "test2"),
				nameOf(With2Failures.class, "test3"),
				nameOf(testBundles.get(0))
			);
		// @formatter:on

		assertThat(result).as("result")
			.hasFailedTest(With2Failures.class, "test1", AssertionError.class)
			.hasSuccessfulTest(With2Failures.class, "test2")
			.hasFailedTest(With2Failures.class, "test3", CustomAssertionError.class);
	}

	@Test
	void runTests_forSingleMethod_works() throws Exception {
		TestRunData result = gogoRunTests(With2Failures.class.getName() + "#test1");

		if (result == null) {
			Assertions.fail("Eclipse didn't capture output");
			return;
		}
		assertThat(result.getTestCount()).as("testCount")
			.isEqualTo(1);

		// @formatter:off
		assertThat(result.getNameMap()
			.keySet()).as("executed")
				.contains(
					nameOf(With2Failures.class),
					nameOf(With2Failures.class, "test1"),
					nameOf(testBundles.get(0))
				);
		// @formatter:on

		assertThat(result).as("result")
			.hasFailedTest(With2Failures.class, "test1", AssertionError.class);

	}

	@Test
	void runTests_forSingleBundle_works() throws Exception {
		TestRunData result = gogoRunTests(":" + testBundles.get(0)
			.getSymbolicName());

		if (result == null) {
			Assertions.fail("Eclipse didn't capture output");
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
		// @formatter:on

		assertThat(result).as("result")
			.hasFailedTest(With2Failures.class, "test1", AssertionError.class)
			.hasSuccessfulTest(With2Failures.class, "test2")
			.hasFailedTest(With2Failures.class, "test3", CustomAssertionError.class)
			.hasSuccessfulTest(JUnit4Test.class, "somethingElse");
	}

	@Test
	void runTests_forMultipleArgs() throws Exception {
		TestRunData result = gogoRunTests(JUnit5Test.class.getName(), With2Failures.class.getName());

		if (result == null) {
			Assertions.fail("Eclipse didn't capture output");
			return;
		}
		assertThat(result.getTestCount()).as("testCount")
			.isEqualTo(4);

		// @formatter:off
		assertThat(result.getNameMap()
			.keySet()).as("executed")
				.contains(
					nameOf(JUnit5Test.class),
					nameOf(JUnit5Test.class, "somethingElseAgain"),
					nameOf(With2Failures.class),
					nameOf(With2Failures.class, "test1"),
					nameOf(With2Failures.class, "test2"),
					nameOf(With2Failures.class, "test3"),
					nameOf(testBundles.get(1))
				);
		// @formatter:on
	}

	@Test
	void runTests_withNoArgs_runsAllTests() throws Exception {
		TestRunData result = gogoRunTests();

		if (result == null) {
			Assertions.fail("Eclipse didn't capture output");
			return;
		}

		assertThat(result.getTestCount()).as("testCount")
			.isEqualTo(9);

		addTestBundle(With2Failures.class, JUnit4Test.class);
		addTestBundle(With1Error1Failure.class, JUnit5Test.class);

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
					nameOf(testBundles.get(0)),
					nameOf(With1Error1Failure.class),
					nameOf(With1Error1Failure.class, "test1"),
					nameOf(With1Error1Failure.class, "test2"),
					nameOf(With1Error1Failure.class, "test3"),
					nameOf(JUnit5Test.class),
					nameOf(JUnit5Test.class, "somethingElseAgain"),
					nameOf(testBundles.get(1))
			);
		// @formatter:on
	}

	@Test
	void setTesterNames_withSingleClassName() throws Exception {
		setTesterNames(With2Failures.class.getName());

		TestRunData result = gogoRunTests();

		if (result == null) {
			Assertions.fail("Eclipse didn't capture output");
			// Unreachable but prevents a compiler warning about null pointer
			// access.
			return;
		}
		assertThat(result.getTestCount()).as("testCount")
			.isEqualTo(3);

		// @formatter:off
		assertThat(result.getNameMap()
			.keySet()).as("executed")
			.contains(
				nameOf(With2Failures.class),
				nameOf(With2Failures.class, "test1"),
				nameOf(With2Failures.class, "test2"),
				nameOf(With2Failures.class, "test3"),
				nameOf(testBundles.get(0))
			);
		// @formatter:on

		assertThat(result).as("result")
			.hasFailedTest(With2Failures.class, "test1", AssertionError.class)
			.hasSuccessfulTest(With2Failures.class, "test2")
			.hasFailedTest(With2Failures.class, "test3", CustomAssertionError.class);
	}

	@Test
	void setTesterNames_forSingleMethod() throws Exception {
		setTesterNames(With2Failures.class.getName() + "#test1");

		TestRunData result = gogoRunTests();

		if (result == null) {
			Assertions.fail("Eclipse didn't capture output");
			return;
		}
		assertThat(result.getTestCount()).as("testCount")
			.isEqualTo(1);

		// @formatter:off
		assertThat(result.getNameMap()
			.keySet()).as("executed")
				.contains(
					nameOf(With2Failures.class),
					nameOf(With2Failures.class, "test1"),
					nameOf(testBundles.get(0))
				);
		// @formatter:on

		assertThat(result).as("result")
			.hasFailedTest(With2Failures.class, "test1", AssertionError.class);
	}

	@Test
	void setTesterNames_forMultipleArgs() throws Exception {
		setTesterNames(JUnit5Test.class.getName() + " " + With2Failures.class.getName());
		TestRunData result = gogoRunTests();

		if (result == null) {
			Assertions.fail("Eclipse didn't capture output");
			return;
		}
		assertThat(result.getTestCount()).as("testCount")
			.isEqualTo(4);

		// @formatter:off
		assertThat(result.getNameMap()
			.keySet()).as("executed")
				.contains(
					nameOf(JUnit5Test.class),
					nameOf(JUnit5Test.class, "somethingElseAgain"),
					nameOf(With2Failures.class),
					nameOf(With2Failures.class, "test1"),
					nameOf(With2Failures.class, "test2"),
					nameOf(With2Failures.class, "test3"),
					nameOf(testBundles.get(1))
				);
		// @formatter:on
	}

	@Test
	void setGetTesterNames() throws Exception {
		setTesterNames(With2Failures.class.getName() + "#test1 " + With2Failures.class.getName() + "#test3");
		assertThat(getTesterNames().split("\\s*,\\s*")).containsExactlyInAnyOrder(
			With2Failures.class.getName() + "#test1", With2Failures.class.getName() + "#test3");

		setTesterNames(null);
		assertThat(getTesterNames()).isNull();
	}

	TestRunData gogoRunTests(String... args) throws Exception {
		runTests.invoke(gogo, new Object[] {
			args
		});
		readWithTimeout(inStr);
		TestRunListener listener = startEclipseJUnitListener();
		outStr.writeInt(eclipseJUnitPort);
		outStr.flush();
		listener.waitForClientToFinish(10000);

		return listener.getLatestRunData();
	}

	void setTesterNames(String names) throws Exception {
		if (names == null) {
			setTesterNames.invoke(gogo, new Object[] {
				null
			});
		} else {
			setTesterNames.invoke(gogo, new Object[] {
				names.split("\\s+")
			});
		}
	}

	String getTesterNames() throws Exception {
		return (String) getTesterNames.invoke(gogo);
	}
}
