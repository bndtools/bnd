package aQute.tester.bundle.engine.test;

import static aQute.tester.bundle.engine.BundleEngine.CHECK_UNRESOLVED;
import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.testkit.engine.Event.byTestDescriptor;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.EventConditions.type;
import static org.junit.platform.testkit.engine.EventConditions.uniqueIdSubstring;
import static org.junit.platform.testkit.engine.EventType.SKIPPED;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.EngineTestKit.Builder;
import org.junit.platform.testkit.engine.Event;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import aQute.bnd.osgi.Constants;
import aQute.launchpad.BundleSpecBuilder;
import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.tester.bundle.engine.BundleDescriptor;
import aQute.tester.bundle.engine.BundleEngine;
import aQute.tester.bundle.engine.BundleEngineDescriptor;
import aQute.tester.bundle.engine.StaticFailureDescriptor;
import aQute.tester.bundle.engine.discovery.BundleSelector;
import aQute.tester.bundle.engine.discovery.BundleSelectorResolver;
import aQute.tester.junit.platform.utils.BundleUtils;
import aQute.tester.test.params.CustomParameter;
import aQute.tester.test.utils.ServiceLoaderMask;
import aQute.tester.test.utils.TestBundler;
import aQute.tester.testclasses.bundle.engine.AnotherTestClass;
import aQute.tester.testclasses.bundle.engine.JUnit3And4Test;
import aQute.tester.testclasses.bundle.engine.JUnit3And5Test;
import aQute.tester.testclasses.bundle.engine.JUnit3AndVenusTest;
import aQute.tester.testclasses.bundle.engine.JUnit4Test;
import aQute.tester.testclasses.bundle.engine.JUnit5ParameterizedTest;
import aQute.tester.testclasses.bundle.engine.JUnit5Test;
import aQute.tester.testclasses.bundle.engine.TestClass;

@SuppressWarnings("restriction")
public class BundleEngineTest {
	private LaunchpadBuilder	builder				= new LaunchpadBuilder().bndrun("bundleenginetest.bndrun")
		.excludeExport("aQute.tester.bundle.engine")
		.excludeExport("aQute.tester.bundle.engine.discovery")
		.excludeExport("aQute.tester.junit.platform*")
		.excludeExport("org.junit.platform.launcher*")
		.excludeExport("org.junit.jupiter*")
		.excludeExport("org.junit.vintage*")
		.excludeExport("org.junit")
		.excludeExport("org.junit.internal*")
		.excludeExport("org.junit.matchers*")
		.excludeExport("org.junit.rules*")
		.excludeExport("org.junit.runner*")
		.excludeExport("org.junit.validator*")
		.excludeExport("junit*");

	private Launchpad			launchpad;

	static final boolean		DEBUG				= true;
	static final String			CUSTOM_LAUNCH		= "customlaunch";

	Bundle						engineBundle;

	private PrintWriter			debugStr;

	TestBundler					testBundler;

	// We have the Jupiter engine on the classpath so that the tests will run.
	// This classloader will hide it from the framework so that it doesn't
	// interfere with the test itself.
	static final ClassLoader	SERVICELOADER_MASK	= new ServiceLoaderMask();

	private String				method;

	@BeforeEach
	public void setUp(TestInfo info) {
		debugStr = DEBUG ? new PrintWriter(System.err) : new PrintWriter(IO.nullWriter);
		Method testMethod = info.getTestMethod()
			.get();
		method = getClass().getName() + "/" + testMethod.getName();
		if (!info.getTags()
			.contains(CUSTOM_LAUNCH)) {
			startLaunchpad();
		}
	}

	protected void startLaunchpad() {
		if (DEBUG) {
			builder.debug();
		}
		launchpad = builder.usingClassLoader(SERVICELOADER_MASK)
			.create(method, "BundleEngineTest");
		testBundler = new TestBundler(launchpad);
	}

	@AfterEach
	public void tearDown() {
		IO.close(launchpad);
		IO.close(builder);
	}

	public static class EngineStarter implements Supplier<TestEngine> {

		@Override
		public TestEngine get() {
			return new BundleEngine();
		}

	}

	static String descriptionOf(Bundle b) {
		return b.getSymbolicName() + ';' + b.getVersion();
	}

	public Bundle startTestBundle(Class<?>... testClasses) {
		return testBundler.startTestBundle(testClasses);
	}

	public Bundle installTestBundle(Class<?>... testClasses) throws Exception {
		return testBundler.installTestBundle(testClasses);
	}

	private BundleSpecBuilder buildTestBundle(Class<?>... testClasses) {
		return testBundler.buildTestBundle(testClasses);
	}

	@Test
	@Tag(CUSTOM_LAUNCH)
	public void outsideOfFramework_hasInitializationError() throws Exception {
		EngineTestKit.engine(new BundleEngine())
			.execute()
			.tests()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(test("noFramework"), finishedWithFailure(instanceOf(JUnitException.class),
				message(x -> x.contains("inside an OSGi framework")))));
	}

	@Test
	@Tag(CUSTOM_LAUNCH)
	public void withNoEngines_reportsMissingEngines_andSkipsMainTests() throws Exception {
		builder = new LaunchpadBuilder();
		builder = builder.bndrun("bundleenginetest-noengines.bndrun")
			.excludeExport("aQute.tester.bundle.engine")
			.excludeExport("aQute.tester.bundle.engine.discovery");
		startLaunchpad();

		Bundle testBundle = testBundler.startTestBundle(JUnit4Test.class);

		engineInFramework().execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1,
				event(test("noEngines"),
					finishedWithFailure(instanceOf(JUnitException.class),
						message(x -> x.contains("Couldn't find any registered TestEngines")))))
			.haveExactly(1, event(bundle(testBundle), skippedWithReason("Couldn't find any registered TestEngines")));
	}

	public class NonEngine {}

	@Test
	public void withEngineWithBadServiceSpec_andTesterUnresolvedTrue_reportsMisconfiguredEngines_andSkipsMainTests()
		throws Exception {
		Bundle engineBundle = testBundler.bundleWithEE()
			.includeResource("META-INF/services/" + TestEngine.class.getName())
			.literal("some.unknown.Engine # Include a comment\n" + NonEngine.class.getName())
			.addResourceWithCopy(NonEngine.class)
			.start();
		startTestBundle(JUnit4Test.class);

		engineInFramework().execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(test("misconfiguredEngines"), unresolvedBundle(engineBundle),
				finishedWithFailure(instanceOf(java.util.ServiceConfigurationError.class))));
	}

	@Test
	public void withEngineWithBadServiceSpec_andTesterUnresolvedFalse_doesntReportMisconfiguredEngines_andRunsMainTests()
		throws Exception {
		Bundle engineBundle = testBundler.bundleWithEE()
			.includeResource("META-INF/services/" + TestEngine.class.getName())
			.literal("some.unknown.Engine # Include a comment\n" + NonEngine.class.getName())
			.addResource(NonEngine.class)
			.start();
		Bundle testBundle = startTestBundle(JUnit4Test.class);

		engineInFramework().configurationParameter(CHECK_UNRESOLVED, "false")
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("misconfiguredEngines"), bundle(engineBundle)))
			.haveExactly(0,
				event(testClass(JUnit4Test.class), finishedWithFailure(instanceOf(ClassCastException.class))))
			.haveExactly(1, event(bundle(testBundle), finishedSuccessfully()))
			.haveExactly(1, event(test("aTest"), finishedSuccessfully()))
			.haveExactly(1, event(test("bTest"), finishedSuccessfully()));
	}

	public static class CustomEngine implements TestEngine {

		static final String ENGINE_ID = "custom.engine";

		@Override
		public String getId() {
			return ENGINE_ID;
		}

		@Override
		public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
			EngineDescriptor root = new EngineDescriptor(uniqueId, "Custom Engine");
			root.addChild(
				new StaticFailureDescriptor(uniqueId.append("test", "customTest"), "A Test", new Exception()));
			return root;
		}

		@Override
		public void execute(ExecutionRequest request) {
			TestDescriptor t = request.getRootTestDescriptor();
			EngineExecutionListener l = request.getEngineExecutionListener();
			l.executionStarted(t);
			for (TestDescriptor td : t.getChildren()) {
				StaticFailureDescriptor s = (StaticFailureDescriptor) td;
				s.execute(l);
			}
			request.getEngineExecutionListener()
				.executionFinished(t, TestExecutionResult.successful());
		}
	}

	@Test
	@Tag(CUSTOM_LAUNCH)
	public void withEngineWithServiceSpecCommentsAndWhitespace_loadsEngine() throws Exception {
		builder = new LaunchpadBuilder();
		builder.bndrun("bundleenginetest-noengines.bndrun")
			.excludeExport("aQute.tester.bundle.engine")
			.excludeExport("aQute.tester.bundle.engine.discovery");
		startLaunchpad();

		Bundle engineBundle = testBundler.bundleWithEE()
			.includeResource("META-INF/services/" + TestEngine.class.getName())
			.literal("# Include a comment\n \t " + CustomEngine.class.getName()
				+ " # another comment\n\n#The above was a blank line")
			.addResource(CustomEngine.class)
			.start();
		Bundle testBundle = startTestBundle(JUnit4Test.class);

		engineInFramework().execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("misconfiguredEngines"), bundle(engineBundle), finishedSuccessfully()))
			.haveExactly(1, event(bundle(testBundle), finishedSuccessfully()))
			.haveExactly(1, event(container(CustomEngine.ENGINE_ID), finishedSuccessfully()))
			.haveExactly(1, event(test("customTest"), finishedWithFailure()));
	}

	public static boolean lastSegmentMatches(UniqueId uId, String type, String contains) {
		final List<UniqueId.Segment> segments = uId.getSegments();
		UniqueId.Segment last = segments.get(segments.size() - 1);
		return last.getType()
			.equals(type)
			&& last.getValue()
				.contains(contains);
	}

	public static Condition<Event> lastSegment(String type, String contains) {
		return new Condition<>(
			byTestDescriptor(
				where(TestDescriptor::getUniqueId, uniqueId -> lastSegmentMatches(uniqueId, type, contains))),
			"last segment of type '%s' with value '%s'", type, contains);
	}

	public static Condition<Event> testClass(Class<?> testClass) {
		return test(testClass.getName());
	}

	public static Condition<Event> containerClass(Class<?> testClass) {
		return container(testClass.getName());
	}

	public static Condition<Event> bundle(Bundle bundle) {
		return container(lastSegment("bundle", descriptionOf(bundle)));
	}

	public static Condition<? super Event> inBundle(Bundle resolvedTestBundle) {
		return container(descriptionOf(resolvedTestBundle));
	}

	public static Condition<? super Event> testInBundle(Bundle resolvedTestBundle) {
		return test(descriptionOf(resolvedTestBundle));
	}

	public static Condition<Event> fragment(Bundle bundle) {
		return container(lastSegment("fragment", descriptionOf(bundle)));
	}

	public static Condition<Event> unresolvedBundle(Bundle bundle) {
		return allOf(test(), lastSegment("bundle", descriptionOf(bundle)));
	}

	public static Condition<Event> displayNameContaining(String substring) {
		return new Condition<>(byTestDescriptor(where(TestDescriptor::getDisplayName, x -> x.contains(substring))),
			"descriptor with display name containing '%s'", substring);
	}

	public static Condition<Event> withParentLastSegment(String type, String contains) {
		return new Condition<>(byTestDescriptor(x -> x.getParent()
			.map(parent -> lastSegmentMatches(parent.getUniqueId(), type, contains))
			.orElse(false)), "parent with last segment of type '%s' and value '%s'", type, contains);

	}

	public Builder engineInFramework() {
		try {
			if (engineBundle == null) {
				engineBundle = testBundler.bundleWithEE()
					.addResourceWithCopy(BundleEngine.class)
					.addResourceWithCopy(BundleEngineDescriptor.class)
					.addResourceWithCopy(BundleDescriptor.class)
					.addResourceWithCopy(StaticFailureDescriptor.class)
					.addResourceWithCopy(BundleSelector.class)
					.addResourceWithCopy(BundleUtils.class)
					.addResourceWithCopy(BundleSelectorResolver.class)
					.addResourceWithCopy(BundleSelectorResolver.SubDiscoveryRequest.class)
					.exportPackage(BundleEngine.class.getPackage()
						.getName())
					.exportPackage(BundleSelector.class.getPackage()
						.getName())
					.start();
			}
			debugStr.println("Engine bundle: " + engineBundle.getHeaders()
				.get("Import-Package"));

			@SuppressWarnings("unchecked")
			Class<? extends TestEngine> clazz = (Class<? extends TestEngine>) engineBundle
				.loadClass(BundleEngine.class.getName());

			return EngineTestKit.engine(clazz.getConstructor()
				.newInstance());
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Test
	public void bundleEngine_executesRootDescriptor() throws Exception {
		startTestBundle(JUnit4Test.class);

		engineInFramework().execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(lastSegment("engine", BundleEngine.ENGINE_ID), started()))
			.haveExactly(1, event(lastSegment("engine", BundleEngine.ENGINE_ID), finishedSuccessfully()));
	}

	@Test
	public void withUnresolvedBundles_reportsUnresolved_andSkipsMainTests() throws Exception {
		String unresolved1 = testBundler.bundleWithEE()
			.importPackage("some.unknown.pkg")
			.install()
			.getSymbolicName();
		String unresolved2 = testBundler.bundleWithEE()
			.importPackage("some.other.package")
			.install()
			.getSymbolicName();

		Bundle testBundle = startTestBundle(JUnit4Test.class);

		engineInFramework().execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(container("unresolvedBundles"), finishedSuccessfully()))
			.haveExactly(1,
				event(test(unresolved1),
					finishedWithFailure(instanceOf(BundleException.class),
						message(x -> x.contains("Unable to resolve")))))
			.haveExactly(1,
				event(test(unresolved2),
					finishedWithFailure(instanceOf(BundleException.class),
						message(x -> x.contains("Unable to resolve")))))
			.haveExactly(1, event(bundle(testBundle), skippedWithReason("Unresolved bundles")));
	}

	@Test
	public void withUnresolvedBundles_andTesterUnresolvedFalse_doesntReportsUnresolved_andRunsMainTests()
		throws Exception {
		testBundler.bundleWithEE()
			.importPackage("some.unknown.pgk")
			.install();

		Bundle testBundle = startTestBundle(JUnit4Test.class);

		engineInFramework().configurationParameter(CHECK_UNRESOLVED, "false")
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("unresolvedBundles")))
			.haveExactly(1, event(bundle(testBundle), finishedSuccessfully()))
			.haveExactly(1, event(test("aTest"), finishedSuccessfully()))
			.haveExactly(1, event(test("bTest"), finishedSuccessfully()));
	}

	@Test
	public void withUnresolvedTestBundle_andTesterUnresolvedFalse_reportsError_andSkipsBundle() throws Exception {
		Bundle unResolvedTestBundle = buildTestBundle(JUnit4Test.class).importPackage("some.unresolved.package")
			.install();
		Bundle resolvedTestBundle = startTestBundle(TestClass.class);

		engineInFramework().configurationParameter(CHECK_UNRESOLVED, "false")
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1,
				event(unresolvedBundle(unResolvedTestBundle), finishedWithFailure(instanceOf(BundleException.class))))
			.haveExactly(1, event(bundle(resolvedTestBundle), finishedSuccessfully()));
	}

	// Only generate the "Unresolved Tests" hierarchy for non-test bundles that
	// fail to resolve.
	@Test
	public void withOnlyTestBundleUnresolved_andTesterUnresolvedTrue_reportsError_andSkipsBundle() throws Exception {
		Bundle unResolvedTestBundle = buildTestBundle(JUnit4Test.class).importPackage("some.unresolved.package")
			.install();
		Bundle resolvedTestBundle = startTestBundle(TestClass.class);

		engineInFramework().execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("unresolvedBundles")))
			.haveExactly(1,
				event(unresolvedBundle(unResolvedTestBundle), finishedWithFailure(instanceOf(BundleException.class))))
			.haveExactly(1, event(bundle(resolvedTestBundle), finishedSuccessfully()));
	}

	@Test
	public void withMethodSelectors_andTestClassesHeader_runsOnlySelectedMethods() throws Exception {
		Bundle resolvedTestBundle = startTestBundle(TestClass.class, JUnit5Test.class);

		engineInFramework().selectors(selectMethod(JUnit5Test.class.getName(), "thisIsBTest"))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1,
				event(inBundle(resolvedTestBundle), containerClass(JUnit5Test.class), finishedSuccessfully()))
			.haveExactly(1, event(testInBundle(resolvedTestBundle), test("thisIsBTest"), finishedSuccessfully()))
			.haveExactly(0, event(test("thisIsATest")))
			.haveExactly(0, event(containerClass(TestClass.class), finishedSuccessfully()));
	}

	@Test
	public void withMethodSelectorsWithParams_andTestClassesHeader_runsOnlySelectedMethods() throws Exception {
		Bundle resolvedTestBundle = startTestBundle(TestClass.class, JUnit5ParameterizedTest.class);

		engineInFramework()
			.selectors(
				selectMethod(JUnit5ParameterizedTest.class.getName(), "parameterizedMethod", "java.lang.String,float"))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1,
				event(inBundle(resolvedTestBundle), lastSegment("class", JUnit5ParameterizedTest.class.getName()),
					finishedSuccessfully()))
			.haveExactly(5,
				event(testInBundle(resolvedTestBundle), test("parameterizedMethod"), finishedSuccessfully()))
			.haveExactly(0, event(test("thisIsATest")))
			.haveExactly(0, event(containerClass(TestClass.class), finishedSuccessfully()));
	}

	@Test
	@Tag(CUSTOM_LAUNCH)
	public void withMethodSelectorsWithParamsFromDifferentBundle_andTestClassesHeader_runsOnlySelectedMethods()
		throws Exception {
		builder.excludeExport("aQute.tester.test.params");
		startLaunchpad();
		Bundle parameterBundle = testBundler.bundleWithEE()
			.addResourceWithCopy(CustomParameter.class)
			.exportPackage(CustomParameter.class.getPackage()
				.getName())
			.install();

		Bundle resolvedTestBundle = startTestBundle(TestClass.class, JUnit5ParameterizedTest.class);

		Builder b = engineInFramework();
		// This assert is to check that our test environment has been configured
		// properly. If the CustomParameter class is visible to the engine
		// bundle (eg, we haven't excluded it properly), then it may work when
		// it shouldn't.
		assertThatThrownBy(() -> engineBundle.loadClass(CustomParameter.class.getName()))
			.isInstanceOf(ClassNotFoundException.class);

		b.selectors(selectMethod(JUnit5ParameterizedTest.class.getName(), "customParameter",
			"aQute.tester.test.params.CustomParameter"))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1,
				event(inBundle(resolvedTestBundle), lastSegment("class", JUnit5ParameterizedTest.class.getName()),
					finishedSuccessfully()))
			.haveExactly(3, event(testInBundle(resolvedTestBundle), test("customParameter"), finishedSuccessfully()))
			.haveExactly(0, event(test("thisIsATest")))
			.haveExactly(0, event(containerClass(TestClass.class), finishedSuccessfully()));
	}

	@Test
	public void withClassNameSelectors_andTestClassHeader_runsOnlySelectedClasses() throws Exception {
		Bundle resolvedTestBundle = startTestBundle(TestClass.class, JUnit4Test.class);

		engineInFramework().selectors(selectClass(TestClass.class.getName()))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(containerClass(JUnit4Test.class)))
			.haveExactly(1,
				event(inBundle(resolvedTestBundle), containerClass(TestClass.class), finishedSuccessfully()));
	}

	@Test
	public void withUnresolvedClassSelectors_andTesterUnresolvedFalse_doesntReportError_andRunsOtherTests()
		throws Exception {
		Bundle resolvedTestBundle = startTestBundle(TestClass.class);

		engineInFramework().configurationParameter(CHECK_UNRESOLVED, "false")
			.selectors(selectClass("some.unknown.Clazz"), selectClass(TestClass.class.getName()))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("unresolvedClasses")))
			// .haveExactly(0,
			// event(unresolvedClass("some.unknown.Clazz"),
			// finishedWithFailure(instanceOf(ClassNotFoundException.class))))
			.haveExactly(1,
				event(inBundle(resolvedTestBundle), containerClass(TestClass.class), finishedSuccessfully()));
	}

	@Test
	public void withUnresolvedClassSelectors_andTesterUnresolvedTrue_reportsError_andRunsOtherTests() throws Exception {
		Bundle resolvedTestBundle = startTestBundle(TestClass.class);

		engineInFramework().selectors(selectClass("some.unknown.Clazz"), selectClass(TestClass.class.getName()))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(container("unresolvedClasses"), finishedSuccessfully()))
			.haveExactly(1, event(test("some.unknown.Clazz"), finishedWithFailure(instanceOf(JUnitException.class))))
			.haveExactly(1, event(bundle(resolvedTestBundle), skippedWithReason("Unresolved classes")));
	}

	@Test
	public void withMethodSelectorForUnresolvedClass_andTesterUnresolvedFalse_doesntReportError_andRunsOtherTests()
		throws Exception {
		Bundle resolvedTestBundle = startTestBundle(TestClass.class);

		engineInFramework().configurationParameter(CHECK_UNRESOLVED, "false")
			.selectors(selectMethod("some.unknown.Clazz", "someMethod"), selectClass(TestClass.class.getName()))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("unresolvedClasses")))
			.haveExactly(1,
				event(inBundle(resolvedTestBundle), containerClass(TestClass.class), finishedSuccessfully()));
	}

	@Test
	public void withMethodSelectorForUnresolvedClass_TesterUnresolvedTrue_reportsError_andRunsOtherTests()
		throws Exception {
		Bundle resolvedTestBundle = startTestBundle(TestClass.class);

		engineInFramework()
			.selectors(selectMethod("some.unknown.Clazz", "someMethod"), selectClass(TestClass.class.getName()))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(container("unresolvedClasses"), finishedSuccessfully()))
			.haveExactly(1, event(test("some.unknown.Clazz"), finishedWithFailure(instanceOf(JUnitException.class))))
			.haveExactly(0, event(container("unresolvedMethods")))
			.haveExactly(1, event(bundle(resolvedTestBundle), skippedWithReason("Unresolved classes")));
	}

	@Test
	public void withMethodSelectorForKnownClassButUnresolvedMethod_TesterUnresolvedTrue_reportsError_andRunsOtherTests()
		throws Exception {
		Bundle resolvedTestBundle = startTestBundle(TestClass.class);

		engineInFramework().selectors(selectMethod(TestClass.class.getName(), "someMethod"))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("unresolvedClasses")))
			.haveExactly(1, event(uniqueIdSubstring(TestClass.class.getName()), test("someMethod"),
				finishedWithFailure(instanceOf(JUnitException.class))));
	}

	@Test
	public void withMethodSelectorForKnownClassButUnresolvedMethod_TesterUnresolvedFalse_doesNotReportError_andRunsOtherTests()
		throws Exception {
		Bundle resolvedTestBundle = startTestBundle(TestClass.class);

		engineInFramework().configurationParameter(CHECK_UNRESOLVED, "false")
			.selectors(selectMethod(TestClass.class.getName(), "someMethod"))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("unresolvedClasses")))
			.haveExactly(0, event(container("unresolvedMethods")))
			.haveExactly(0, event(test("someMethod")));
	}

	@Test
	public void withClassSelector_forUnresolvedTestBundle_andTesterUnresolvedTrue_reportsUnresolvedBundle_butNotUnresolvedClass()
		throws Exception {
		BundleSpecBuilder bb = buildTestBundle(TestClass.class);
		Bundle unResolvedTestBundle = bb.importPackage("some.unknown.pkg")
			.install();

		engineInFramework().selectors(selectClass(TestClass.class.getName()))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("unresolvedClasses")))
			.haveExactly(1,
				event(unresolvedBundle(unResolvedTestBundle), finishedWithFailure(instanceOf(BundleException.class))));
	}

	@Test
	public void withClassSelector_forUnattachedTestFragment_andTesterUnresolvedTrue_reportsUnattachedFragment_butNotUnresolvedClass()
		throws Exception {
		BundleSpecBuilder bb = buildTestBundle(TestClass.class);
		Bundle unAttachedTestFragment = bb.fragmentHost("some.unknown.bundle")
			.install();

		engineInFramework().selectors(selectClass(TestClass.class.getName()))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("unresolvedClasses")))
			.haveExactly(1,
				event(unresolvedBundle(unAttachedTestFragment), finishedWithFailure(instanceOf(BundleException.class),
					message("Test fragment was not attached to a host bundle"))));
	}

	// Only generate the "Unresolved Tests" hierarchy for classes specified in
	// tester.testcases
	@Test
	public void withTestClassHeaderUnresolved_andTesterUnresolvedFalse_reportsError_andRunsOtherClasses()
		throws Exception {
		Bundle unResolvedTestBundle = testBundler.bundleWithEE()
			.addResourceWithCopy(JUnit4Test.class)
			.header("Test-Cases", JUnit4Test.class.getName() + ", some.other.Clazz")
			.start();
		Bundle resolvedTestBundle = startTestBundle(TestClass.class);

		engineInFramework().configurationParameter(CHECK_UNRESOLVED, "false")
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("unresolvedClasses")))
			.haveExactly(1,
				event(test("some.other.Clazz"), withParentLastSegment("bundle", unResolvedTestBundle.getSymbolicName()),
					finishedWithFailure(instanceOf(ClassNotFoundException.class))))
			.haveExactly(1, event(test("aTest"), finishedSuccessfully()))
			.haveExactly(1, event(test("bTest"), finishedSuccessfully()))
			.haveExactly(1, event(bundle(resolvedTestBundle), finishedSuccessfully()));
	}

	// Only generate the "Unresolved Tests" hierarchy for classes specified in
	// tester.testcases
	@Test
	public void withTestClassHeaderUnresolved_andTesterUnresolvedTrue_reportsError_andRunsOtherClasses()
		throws Exception {
		Bundle unResolvedTestBundle = testBundler.bundleWithEE()
			.addResourceWithCopy(JUnit4Test.class)
			.header("Test-Cases", JUnit4Test.class.getName() + ", some.other.Clazz")
			.start();
		Bundle resolvedTestBundle = startTestBundle(TestClass.class);

		engineInFramework().execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("unresolvedClasses")))
			.haveExactly(1,
				event(test("some.other.Clazz"), withParentLastSegment("bundle", unResolvedTestBundle.getSymbolicName()),
					finishedWithFailure(instanceOf(ClassNotFoundException.class))))
			.haveExactly(1, event(test("aTest"), finishedSuccessfully()))
			.haveExactly(1, event(test("bTest"), finishedSuccessfully()))
			.haveExactly(1, event(bundle(resolvedTestBundle), finishedSuccessfully()));
	}

	@Test
	public void withUnresolvedClass_andUnresolvedBundle_andUnattachedFragment_reportsAll() throws Exception {
		Bundle unresolved = testBundler.bundleWithEE()
			.importPackage("some.unknown.pkg")
			.install();

		Bundle testBundle = startTestBundle(JUnit4Test.class);

		testBundler.bundleWithEE()
			.fragmentHost("some.unknown.bundle")
			.install();

		engineInFramework().selectors(selectClass("some.unknown.class"))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(container("unresolvedBundles"), finishedSuccessfully()))
			.haveExactly(1,
				event(test(unresolved.getSymbolicName()),
					finishedWithFailure(instanceOf(BundleException.class),
						message(x -> x.contains("Unable to resolve")))))
			.haveExactly(1, event(container("unattachedFragments"), finishedSuccessfully()))
			.haveExactly(1, event(container("unresolvedClasses"), finishedSuccessfully()));

		engineInFramework().execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(container("unresolvedBundles"), finishedSuccessfully()))
			.haveExactly(1,
				event(test(unresolved.getSymbolicName()),
					finishedWithFailure(instanceOf(BundleException.class),
						message(x -> x.contains("Unable to resolve")))))
			.haveExactly(1, event(container("unattachedFragments"), finishedSuccessfully()))
			.haveExactly(1, event(bundle(testBundle), type(SKIPPED)));
	}

	@Test
	public void withUnattachedFragments_reportsUnattached_andSkipsMainTests() throws Exception {
		Bundle fragment = testBundler.bundleWithEE()
			.addResourceWithCopy(NonEngine.class)
			.fragmentHost("some.missing.bundle")
			.install();

		Bundle testBundle = installTestBundle(JUnit4Test.class);
		testBundler.bundleWithEE()
			.addResourceWithCopy(NonEngine.class)
			.fragmentHost(testBundle.getSymbolicName())
			.install();
		testBundle.start();

		engineInFramework().execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(container("unattachedFragments"), finishedSuccessfully()))
			.haveExactly(1,
				event(unresolvedBundle(fragment), withParentLastSegment("test", "unattachedFragments"),
					finishedWithFailure(instanceOf(JUnitException.class),
						message(x -> x.contains("Fragment was not attached to a host bundle")))))
			.haveExactly(1, event(bundle(testBundle), skippedWithReason("Unattached fragments")));
	}

	@Test
	public void withUnattachedNonTestFragments_andTesterUnresolvedFalse_doesntReportsUnattached_andRunsMainTests()
		throws Exception {
		testBundler.bundleWithEE()
			.addResourceWithCopy(NonEngine.class)
			.fragmentHost("some.missing.bundle")
			.install();

		Bundle testBundle = startTestBundle(JUnit4Test.class);

		engineInFramework().configurationParameter(CHECK_UNRESOLVED, "false")
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("unattachedFragments")))
			.haveExactly(1, event(bundle(testBundle), finishedSuccessfully()))
			.haveExactly(1, event(test("aTest"), finishedSuccessfully()))
			.haveExactly(1, event(test("bTest"), finishedSuccessfully()));
	}

	@Test
	public void withUnattachedTestFragment_andTesterUnresolvedFalse_reportsError_andSkipsBundle() throws Exception {
		Bundle unattachedTestFragment = buildTestBundle(JUnit4Test.class).fragmentHost("some.unresolved.package")
			.install();

		Bundle testFragmentHostWithoutItsOwnTests = testBundler.bundleWithEE()
			.install();

		Bundle attachedTestFragment = buildTestBundle(TestClass.class)
			.fragmentHost(testFragmentHostWithoutItsOwnTests.getSymbolicName())
			.install();

		testFragmentHostWithoutItsOwnTests.start();

		engineInFramework().configurationParameter(CHECK_UNRESOLVED, "false")
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1,
				event(unresolvedBundle(unattachedTestFragment),
					finishedWithFailure(instanceOf(BundleException.class),
						message("Test fragment was not attached to a host bundle"))))
			.haveExactly(0, event(withParentLastSegment("bundle", descriptionOf(unattachedTestFragment))))
			.haveExactly(1,
				event(container(), lastSegment("fragment", descriptionOf(attachedTestFragment)),
					withParentLastSegment("bundle", descriptionOf(testFragmentHostWithoutItsOwnTests)),
					finishedSuccessfully()))
			.haveExactly(1,
				event(container("junit-vintage"),
					withParentLastSegment("fragment", descriptionOf(attachedTestFragment)), finishedSuccessfully()))
			.haveExactly(1, event(test("thisIsATest"), finishedSuccessfully()));
	}

	@Test
	public void withTestFragment_attachedToTestBundle_runsBothSetsOfTests() throws Exception {
		Bundle testFragmentHostWithItsOwnTests = installTestBundle(JUnit4Test.class);

		final String fragmentHost = testFragmentHostWithItsOwnTests.getSymbolicName();

		Bundle attachedTestFragment = buildTestBundle(TestClass.class).fragmentHost(fragmentHost)
			.install();

		testFragmentHostWithItsOwnTests.start();

		engineInFramework().configurationParameter(CHECK_UNRESOLVED, "false")
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1,
				event(container(), lastSegment("fragment", descriptionOf(attachedTestFragment)),
					withParentLastSegment("bundle", descriptionOf(testFragmentHostWithItsOwnTests)),
					finishedSuccessfully()))
			.haveExactly(1,
				event(container("junit-vintage"),
					withParentLastSegment("fragment", descriptionOf(attachedTestFragment)), finishedSuccessfully()))
			.haveExactly(1,
				event(container("junit-vintage"),
					withParentLastSegment("bundle", descriptionOf(testFragmentHostWithItsOwnTests)),
					finishedSuccessfully()))
			.haveExactly(1, event(test("thisIsATest"), finishedSuccessfully()))
			.haveExactly(1, event(test("aTest"), finishedSuccessfully()))
			.haveExactly(1, event(test("bTest"), finishedSuccessfully()));
	}

	// Only generate the "Unattached fragments" hierarchy for non-test fragment
	// that fail to attach.
	@Test
	public void withOnlyTestFragmentUnattached_andTesterUnresolvedTrue_reportsError_andSkipsBundle() throws Exception {
		Bundle unAttachedTestFragment = buildTestBundle(JUnit4Test.class).fragmentHost("some.unresolved.bundle")
			.install();
		Bundle testHost = testBundler.bundleWithEE()
			.install();
		Bundle attachedTestFragment = buildTestBundle(TestClass.class).fragmentHost(testHost.getSymbolicName())
			.install();

		testHost.start();

		engineInFramework().execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(container("unattachedFragments")))
			.haveExactly(1,
				event(unresolvedBundle(unAttachedTestFragment), finishedWithFailure(instanceOf(BundleException.class))))
			.haveExactly(1, event(fragment(attachedTestFragment), finishedSuccessfully()));
	}

	// Helper methods to call the bundle selector in the engine bundle's class.
	private DiscoverySelector selectBundle(String bsn, String version) {
		try {
			Class<?> bundleSelector = engineBundle.loadClass(BundleSelector.class.getName());
			Method m = bundleSelector.getMethod("selectBundle", String.class, String.class);
			return (DiscoverySelector) m.invoke(null, bsn, version);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private DiscoverySelector selectBundle(String bsn) {
		try {
			Class<?> bundleSelector = engineBundle.loadClass(BundleSelector.class.getName());
			Method m = bundleSelector.getMethod("selectBundle", String.class);
			return (DiscoverySelector) m.invoke(null, bsn);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private DiscoverySelector selectBundle(Bundle b) {
		try {
			Class<?> bundleSelector = engineBundle.loadClass(BundleSelector.class.getName());
			Method m = bundleSelector.getMethod("selectBundle", Bundle.class);
			return (DiscoverySelector) m.invoke(null, b);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Test
	public void withBundleSelectors_onlyRunsTestsInSelectedBundles() throws Exception {
		Bundle tb1 = startTestBundle(JUnit4Test.class);
		Bundle tb2 = startTestBundle(JUnit5Test.class);
		buildTestBundle(AnotherTestClass.class).bundleSymbolicName("test.bundle")
			.bundleVersion("2.3.4")
			.start();
		buildTestBundle(TestClass.class).bundleSymbolicName("test.bundle")
			.bundleVersion("1.2.3")
			.start();

		engineInFramework()
			.selectors(selectBundle(tb1), selectBundle(tb2.getSymbolicName()), selectBundle("test.bundle", "[1,2)"))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(containerClass(JUnit4Test.class), finishedSuccessfully()))
			.haveExactly(1, event(containerClass(JUnit5Test.class), finishedSuccessfully()))
			.haveExactly(1, event(containerClass(TestClass.class), finishedSuccessfully()))
			.haveExactly(0, event(containerClass(AnotherTestClass.class)));

		engineInFramework().selectors(selectBundle("test.bundle", "[2,3)"))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(containerClass(AnotherTestClass.class), finishedSuccessfully()))
			.haveExactly(0, event(containerClass(TestClass.class)));
	}

	@Test
	public void withBundleSelector_alsoRunsBundleFragments_ofSelectedBundle() throws Exception {
		Bundle tb1 = buildTestBundle(JUnit4Test.class).install();
		Bundle tb2 = buildTestBundle(JUnit5Test.class).header("Fragment-Host", tb1.getSymbolicName())
			.install();

		engineInFramework().selectors(selectBundle(tb1))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(containerClass(JUnit4Test.class), finishedSuccessfully()))
			.haveExactly(1, event(containerClass(JUnit5Test.class), finishedSuccessfully()))
			.haveExactly(1, event(bundle(tb1), finishedSuccessfully()))
			.haveExactly(1, event(fragment(tb2), finishedSuccessfully()));

		engineInFramework().selectors(selectBundle(tb2))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(fragment(tb2), finishedSuccessfully()))
			.haveExactly(0, event(containerClass(JUnit4Test.class)));
	}

	@Test
	public void withSameTestClass_exportedByMultipleBundles_onlyRunsOnce() throws Exception {
		// First bundle contains the class & exports it; second imports it from
		// first.
		BundleSpecBuilder bb = buildTestBundle(JUnit4Test.class);
		bb.exportPackage(JUnit4Test.class.getPackage()
			.getName())
			.start();
		testBundler.bundleWithEE()
			.importPackage(JUnit4Test.class.getPackage()
				.getName())
			.header("Test-Cases", JUnit4Test.class.getPackage()
				.getName())
			.start();

		engineInFramework().execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(containerClass(JUnit4Test.class), finishedSuccessfully()));
	}

	@Test
	public void withSameTestClass_exportedByMultipleBundles_andClassSelector_onlyRunsOnce() throws Exception {
		// First bundle contains the class & exports it; second imports it from
		// first.
		BundleSpecBuilder bb = buildTestBundle(JUnit4Test.class);
		bb.exportPackage(JUnit4Test.class.getPackage()
			.getName())
			.start();

		testBundler.bundleWithEE()
			.importPackage(JUnit4Test.class.getPackage()
				.getName())
			.header("Test-Cases", JUnit4Test.class.getName())
			.start();

		engineInFramework().selectors(selectClass(JUnit4Test.class.getName()))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(containerClass(JUnit4Test.class), finishedSuccessfully()));
	}

	@Test
	public void testClass_inFragment_withClassSelector_runsOnlyInFragment() throws Exception {
		Bundle testFragmentHostWithoutItsOwnTests = testBundler.bundleWithEE()
			.bundleSymbolicName("host.bundle")
			.install();

		Bundle attachedTestFragment = buildTestBundle(TestClass.class)
			.fragmentHost(testFragmentHostWithoutItsOwnTests.getSymbolicName())
			.bundleSymbolicName("fragment.bundle")
			.install();

		testFragmentHostWithoutItsOwnTests.start();

		engineInFramework().selectors(selectClass(TestClass.class))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(uniqueIdSubstring("host.bundle"), test("thisIsATest"), finishedSuccessfully()))
			.haveExactly(1, event(uniqueIdSubstring("fragment.bundle"), test("thisIsATest"), finishedSuccessfully()));
	}

	@Test
	public void testClass_inFragment_withMethodSelector_runsOnlyInFragment() throws Exception {
		Bundle testFragmentHostWithoutItsOwnTests = testBundler.bundleWithEE()
			.bundleSymbolicName("host.bundle")
			.install();

		Bundle attachedTestFragment = buildTestBundle(JUnit4Test.class)
			.fragmentHost(testFragmentHostWithoutItsOwnTests.getSymbolicName())
			.bundleSymbolicName("fragment.bundle")
			.install();

		testFragmentHostWithoutItsOwnTests.start();

		engineInFramework().selectors(selectMethod(JUnit4Test.class, "aTest"))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1, event(uniqueIdSubstring("host.bundle"), test("aTest"), finishedSuccessfully()))
			.haveExactly(1, event(uniqueIdSubstring("fragment.bundle"), test("aTest"), finishedSuccessfully()))
			.haveExactly(0, event(test("bTest")));
	}

	@Test
	public void testClass_andBundleWithDynamicImport_withMethodSelector_runsOnlyInTestBundle() {
		Bundle test = testBundler.buildTestBundle(TestClass.class)
			.exportPackage(TestClass.class.getPackage()
				.getName())
			.start();

		Bundle dynamic = testBundler.bundleWithEE()
			.bundleSymbolicName("dynamic.bundle")
			.header(Constants.DYNAMICIMPORT_PACKAGE, "*")
			.start();

		engineInFramework().selectors(selectMethod(TestClass.class, "thisIsATest"))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, event(uniqueIdSubstring("dynamic.bundle"), containerClass(TestClass.class)))
			.haveExactly(1,
				event(uniqueIdSubstring(test.getSymbolicName()), test("thisIsATest"), finishedSuccessfully()));
	}

	@Test
	public void testClass_withBothJUnit3And4_raisesAnError() {
		Bundle test = startTestBundle(JUnit3And4Test.class);

		engineInFramework().execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1,
				event(uniqueIdSubstring(test.getSymbolicName()), testClass(JUnit3And4Test.class),
					finishedWithFailure(instanceOf(JUnitException.class),
						message(
							x -> x.matches("^(?si).*TestCase.*JUnit 4 annotations.*annotations will be ignored.*$")))));

	}

	@Test
	public void withClassSelector_testClass_withBothJUnit3And4_raisesAnError() {
		Bundle test = startTestBundle(JUnit3And4Test.class);

		engineInFramework().selectors(selectClass(JUnit3And4Test.class))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1,
				event(uniqueIdSubstring(test.getSymbolicName()), testClass(JUnit3And4Test.class), finishedWithFailure(
					instanceOf(JUnitException.class),
					message(x -> x.matches("^(?si).*TestCase.*JUnit 4 annotations.*annotations will be ignored.*$")))));

	}

	@Test
	public void withMethodSelector_testClass_withBothJUnit3And4_raisesAnError() {
		Bundle test = startTestBundle(JUnit3And4Test.class);

		engineInFramework().selectors(selectMethod(JUnit3And4Test.class, "testSomething"))
			.execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(1,
				event(uniqueIdSubstring(test.getSymbolicName()), testClass(JUnit3And4Test.class), finishedWithFailure(
					instanceOf(JUnitException.class),
					message(x -> x.matches("^(?si).*TestCase.*JUnit 4 annotations.*annotations will be ignored.*$")))));

	}

	@Test
	public void testClass_withBothJUnit3AndOtherJUnit_doesntRaiseAnError() {
		// "Venus" is a hypothetical not-yet-released JUnit implementation (the
		// planet before Jupiter); need to make sure that our JUnit 4 testing
		// is fairly future proof in the face of future JUnit releases.
		Bundle test = startTestBundle(JUnit3And5Test.class, JUnit3AndVenusTest.class);

		engineInFramework().execute()
			.all()
			.debug(debugStr)
			.assertThatEvents()
			.haveExactly(0, finishedWithFailure(instanceOf(JUnitException.class)));
	}
}
