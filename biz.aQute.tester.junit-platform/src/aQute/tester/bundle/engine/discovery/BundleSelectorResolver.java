package aQute.tester.bundle.engine.discovery;

import static aQute.tester.bundle.engine.BundleDescriptor.displayNameOf;
import static aQute.tester.bundle.engine.BundleEngine.CHECK_UNRESOLVED;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.FrameworkWiring;

import aQute.tester.bundle.engine.BundleDescriptor;
import aQute.tester.bundle.engine.BundleEngine;
import aQute.tester.bundle.engine.StaticFailureDescriptor;
import aQute.tester.junit.platform.utils.BundleUtils;

public class BundleSelectorResolver {

	static final Predicate<String>		JUNIT4_ANNOTATIONS	=					//
		name -> {
			switch (name) {
				case "org.junit.Test" :
				case "org.junit.Before" :
				case "org.junit.BeforeClass" :
				case "org.junit.After" :
				case "org.junit.AfterClass" :
				case "org.junit.experimental.theories.Theory" :
				case "org.junit.runners.RunWith" :
					return true;
				default :
					return false;
			}
		};

	final boolean						testUnresolved;
	final Map<Long, BundleDescriptor>	bundleMap			= new HashMap<>();

	private String dump(TestDescriptor t, String indent) {
		return indent + t + "\n" + t.getChildren()
			.stream()
			.map(x -> indent + dump(x, indent + " "))
			.collect(Collectors.joining("\n")) + "\n";
	}

	private static String uniqueIdOf(Bundle bundle) {
		return bundle.getSymbolicName() + ';' + bundle.getVersion();
	}

	final BundleContext				context;
	final EngineDiscoveryRequest	request;
	final EngineDescriptor			descriptor;
	final StaticFailureDescriptor	misconfiguredEnginesDescriptor;
	final StaticFailureDescriptor	unresolvedBundlesDescriptor;
	final StaticFailureDescriptor	unattachedFragmentsDescriptor;
	final List<BundleSelector>		bundleSelectors;
	final List<ClassSelector>		classSelectors;
	final List<MethodSelector>		methodSelectors;
	final Predicate<Bundle>			isATestBundle;
	final Predicate<Bundle>			isNotATestBundle;
	final Bundle[]					allBundles;
	final Set<TestEngine>			engines;
	final Set<String>				unresolvedClassNames	= new HashSet<>();
	final Map<String, Set<String>>	unresolvedMethodNames	= new HashMap<>();
	final Set<Class<?>>				resolvedClasses			= new HashSet<>();
	private boolean					verbose					= false;

	public static void resolve(BundleContext context, EngineDiscoveryRequest request, EngineDescriptor descriptor) {
		new BundleSelectorResolver(context, request, descriptor).resolve();
	}

	private BundleSelectorResolver(BundleContext context, EngineDiscoveryRequest request, EngineDescriptor descriptor) {
		this.context = context;
		this.request = request;
		this.descriptor = descriptor;
		final UniqueId uniqueId = descriptor.getUniqueId();
		testUnresolved = request.getConfigurationParameters()
			.getBoolean(CHECK_UNRESOLVED)
			.orElse(true);

		misconfiguredEnginesDescriptor = new StaticFailureDescriptor(uniqueId.append("test", "misconfiguredEngines"),
			"Misconfigured TestEngines");
		unresolvedBundlesDescriptor = new StaticFailureDescriptor(uniqueId.append("test", "unresolvedBundles"),
			"Unresolved bundles");
		unattachedFragmentsDescriptor = new StaticFailureDescriptor(uniqueId.append("test", "unattachedFragments"),
			"Unattached fragments");

		classSelectors = request.getSelectorsByType(ClassSelector.class);

		methodSelectors = request.getSelectorsByType(MethodSelector.class)
			.stream()
			.map(selector -> DiscoverySelectors.selectMethod(selector.getClassName(), selector.getMethodName(),
				selector.getParameterTypeNames()))
			.collect(toList());

		bundleSelectors = request.getSelectorsByType(BundleSelector.class);

		info(() -> "our class loader: " + BundleSelector.class.getClassLoader());
		info(() -> "supplied selector's class loaders: " + request.getSelectorsByType(DiscoverySelector.class)
			.stream()
			.map(Object::getClass)
			.map(Class::getClassLoader)
			.map(Object::toString)
			.collect(Collectors.joining(",")));

		allBundles = context.getBundles();

		isATestBundle = bundleSelectors.isEmpty() ? BundleUtils::hasTests
			: bundleSelectors.stream()
				.<Predicate<Bundle>> map(selector -> bundle -> {
					if (selector.selects(bundle)) {
						return true;
					}
					Bundle host = BundleUtils.getHost(bundle)
						.orElse(bundle);
					return ((host != bundle) && selector.selects(host));
				})
				.reduce(Predicate::or)
				.orElse(bundle -> false);
		isNotATestBundle = isATestBundle.negate();

		engines = findEngines();

		classSelectors.stream()
			.map(ClassSelector::getClassName)
			.forEach(unresolvedClassNames::add);
		methodSelectors.stream()
			.forEach(selector -> {
				unresolvedClassNames.add(selector.getClassName());
				unresolvedMethodNames.computeIfAbsent(selector.getClassName(), key -> new HashSet<>())
					.add(selector.getMethodName());
			});
	}

	private Set<TestEngine> findEngines() {
		return Arrays.stream(allBundles)
			.flatMap(bundle -> BundleUtils.getClassLoader(bundle)
				.map(classLoader -> {
					try {
						Stream.Builder<TestEngine> builder = Stream.builder();
						// We need to instantiate the engine objects here
						// to handle any error here.
						StreamSupport.stream(ServiceLoader.load(TestEngine.class, classLoader)
							.spliterator(), false)
							.filter(engine -> engine.getId() != BundleEngine.ENGINE_ID)
							.forEach(builder);
						return builder.build();
					} catch (ServiceConfigurationError e) {
						if (testUnresolved) {
							String bundleDesc = uniqueIdOf(bundle);
							StaticFailureDescriptor bd = new StaticFailureDescriptor(
								misconfiguredEnginesDescriptor.getUniqueId()
									.append("bundle", bundleDesc),
								displayNameOf(bundle), e);
							misconfiguredEnginesDescriptor.addChild(bd);
						}
						return Stream.<TestEngine> empty();
					}
				})
				.orElseGet(Stream::empty))
			.collect(toSet());
	}

	private void resolve() {
		FrameworkWiring frameworkWiring = context.getBundle(0)
			.adapt(FrameworkWiring.class);
		UniqueId uniqueId = descriptor.getUniqueId();
		Arrays.stream(allBundles)
			.filter(isATestBundle)
			.filter(BundleUtils::isNotFragment)
			.filter(BundleUtils::isNotResolved)
			.forEach(bundle -> {
				if (frameworkWiring.resolveBundles(Collections.singleton(bundle))) {
					return;
				}
				// Trigger BundleException with resolution failure information
				try {
					bundle.start(Bundle.START_TRANSIENT | Bundle.START_ACTIVATION_POLICY);
				} catch (BundleException resolutionFailure) {
					UniqueId bundleId = uniqueId.append("bundle", uniqueIdOf(bundle));
					BundleDescriptor bd = new BundleDescriptor(bundle, bundleId, resolutionFailure);
					descriptor.addChild(bd);
					markClassesResolved(bundle);
				}
			});

		// Attempt to resolve the bundles before checking for unattached
		// fragments, as the fragments don't attach until their host bundle
		// resolves.
		Arrays.stream(allBundles)
			.filter(isNotATestBundle)
			.filter(BundleUtils::isNotFragment)
			.filter(BundleUtils::isNotResolved)
			.forEach(bundle -> {
				if (frameworkWiring.resolveBundles(Collections.singleton(bundle)) || !testUnresolved) {
					return;
				}
				// Trigger BundleException with resolution failure information
				try {
					bundle.start(Bundle.START_TRANSIENT | Bundle.START_ACTIVATION_POLICY);
				} catch (BundleException resolutionFailure) {
					unresolvedBundlesDescriptor
						.addChild(new StaticFailureDescriptor(unresolvedBundlesDescriptor.getUniqueId()
							.append("bundle", uniqueIdOf(bundle)), displayNameOf(bundle), resolutionFailure));
				}
			});

		if (testUnresolved) {
			Arrays.stream(allBundles)
				.filter(BundleUtils::isFragment)
				.filter(isNotATestBundle)
				.filter(BundleUtils::isNotResolved)
				.forEach(fragment -> {
					unattachedFragmentsDescriptor.addChild(new StaticFailureDescriptor(
						unattachedFragmentsDescriptor.getUniqueId()
							.append("bundle", uniqueIdOf(fragment)),
						displayNameOf(fragment), new JUnitException("Fragment was not attached to a host bundle")));
				});
		}

		Arrays.stream(allBundles)
			.filter(isATestBundle)
			.filter(BundleUtils::isFragment)
			.filter(BundleUtils::isNotResolved)
			.forEach(fragment -> {
				UniqueId fragmentId = uniqueId.append("bundle", uniqueIdOf(fragment));
				BundleDescriptor bd = new BundleDescriptor(fragment, fragmentId,
					new BundleException("Test fragment was not attached to a host bundle"));
				descriptor.addChild(bd);
				markClassesResolved(fragment);
			});

		Arrays.stream(allBundles)
			.filter(BundleUtils::isResolved)
			.filter(BundleUtils::isNotFragment)
			.forEach(bundle -> {
				info(() -> "Performing discovery for bundle: " + bundle.getSymbolicName());
				String bundleDesc = uniqueIdOf(bundle);
				UniqueId bundleId = descriptor.getUniqueId()
					.append("bundle", bundleDesc);
				BundleDescriptor bd = new BundleDescriptor(bundle, bundleId);
				if (computeChildren(bd)) {
					descriptor.addChild(bd);
					bundleMap.put(bundle.getBundleId(), bd);
				}
			});

		Arrays.stream(allBundles)
			.filter(isATestBundle)
			.filter(BundleUtils::isFragment)
			.filter(BundleUtils::isResolved)
			.forEach(fragment -> {
				info(() -> "Performing discovery for fragment: " + fragment.getSymbolicName());
				Bundle host = BundleUtils.getHost(fragment)
					.get();
				BundleDescriptor bd = bundleMap.computeIfAbsent(host.getBundleId(), id -> {
					UniqueId hostId = uniqueId.append("bundle", uniqueIdOf(host));
					BundleDescriptor childDescriptor = new BundleDescriptor(host, hostId);
					descriptor.addChild(childDescriptor);
					return childDescriptor;
				});
				BundleDescriptor fd = new BundleDescriptor(fragment, bd.getUniqueId()
					.append("fragment", uniqueIdOf(fragment)));
				bd.addChild(fd);
				computeChildren(fd);
			});

		Stream.of(misconfiguredEnginesDescriptor, unresolvedBundlesDescriptor, unattachedFragmentsDescriptor)
			.filter(StaticFailureDescriptor::hasChildren)
			.forEach(descriptor::addChild);

		if (engines.isEmpty()) {
			StaticFailureDescriptor noEnginesDescriptor = new StaticFailureDescriptor(
				uniqueId.append("test", "noEngines"), "Initialization Error",
				new JUnitException("Couldn't find any registered TestEngines"));
			descriptor.addChild(noEnginesDescriptor);
			return;
		}

		if (testUnresolved) {
			if (!unresolvedClassNames.isEmpty()) {
				StaticFailureDescriptor unresolvedClassesDescriptor = new StaticFailureDescriptor(
					uniqueId.append("test", "unresolvedClasses"), "Unresolved classes");
				for (String unresolvedClass : unresolvedClassNames) {
					unresolvedClassesDescriptor.addChild(new StaticFailureDescriptor(
						unresolvedClassesDescriptor.getUniqueId()
							.append("test", unresolvedClass),
						unresolvedClass,
						new JUnitException("Couldn't find class " + unresolvedClass + " in any bundle")));
					// Don't report unresolved methods of unresolved classes
					unresolvedMethodNames.remove(unresolvedClass);
				}
				descriptor.addChild(unresolvedClassesDescriptor);
			}
			if (!unresolvedMethodNames.isEmpty()) {
				StaticFailureDescriptor unresolvedMethodsDescriptor = new StaticFailureDescriptor(
					uniqueId.append("test", "unresolvedMethods"), "Unresolved methods");
				unresolvedMethodNames.forEach((className, methods) -> {
					StaticFailureDescriptor classFailure = new StaticFailureDescriptor(
						unresolvedMethodsDescriptor.getUniqueId()
							.append("test", className),
						className);

					methods.forEach(method -> {
						StaticFailureDescriptor methodFailure = new StaticFailureDescriptor(classFailure.getUniqueId()
							.append("test", method), method,
							new JUnitException("Couldn't find method " + method + " in class " + className));
						classFailure.addChild(methodFailure);
					});
					unresolvedMethodsDescriptor.addChild(classFailure);
				});
				descriptor.addChild(unresolvedMethodsDescriptor);
			}
		}
		info(() -> dump(descriptor, ""));
	}

	void info(Supplier<String> msg, Throwable cause) {
		if (verbose) {
			System.err.println(msg.get());
			cause.printStackTrace(System.err);
		}
	}

	void info(Supplier<String> msg) {
		if (verbose) {
			System.err.println(msg.get());
		}
	}

	private void markClassesResolved(Bundle bundle) {
		BundleUtils.testCases(bundle)
			.map(testcase -> {
				int index = testcase.indexOf('#');
				return (index < 0) ? testcase : testcase.substring(0, index);
			})
			.forEach(unresolvedClassNames::remove);
	}

	private Class<?> tryToResolveTestClass(Bundle host, String className, BundleDescriptor bd) {
		try {
			Class<?> testClass = host.loadClass(className);
			// The following is necessary to attempt to resolve the
			// method parameters and force NoClassDefFoundError if they
			// can't be resolved; see
			// https://github.com/bndtools/bnd/issues/3882
			testClass.getDeclaredMethods();
			testClass.getDeclaredFields();
			// This is for https://github.com/bndtools/bnd/issues/4766, however
			// it was hard to create a test case in launchpad that would
			// reproduce the issue so there is no test case coverage for this
			// at the moment unfortunately. The ArrayStoreException can happen
			// as a result of the missing or unresolvable classes referenced in
			// the annotations.
			testClass.getAnnotations();
			return testClass;
		} catch (ClassNotFoundException | NoClassDefFoundError | ArrayStoreException cnfe) {
			info(() -> "Couldn't load and/or resolve class " + className + ": " + cnfe);
			StaticFailureDescriptor unresolvedClassDescriptor = new StaticFailureDescriptor(bd.getUniqueId()
				.append("test", className), className, cnfe);
			bd.addChild(unresolvedClassDescriptor);
		}
		return null;
	}

	private List<DiscoverySelector> getSelectorsFromTestCasesHeader(BundleDescriptor bd) {
		Bundle bundle = bd.getBundle();
		Bundle host = BundleUtils.getHost(bundle)
			.get();
		return BundleUtils.testCases(bundle)
			.map(testcase -> {
				int index = testcase.indexOf('#');
				String className = (index < 0) ? testcase : testcase.substring(0, index);
				Class<?> testClass = tryToResolveTestClass(host, className, bd);
				if (testClass != null && !resolvedClasses.contains(testClass)) {
					checkForMixedJUnit34(bd, testClass);
					if (index < 0) {
						resolvedClasses.add(testClass);
						return selectClass(testClass);
					}
					return selectMethod(testClass, DiscoverySelectors.selectMethod(testcase));
				}
				return null;
			})
			.filter(Objects::nonNull)
			.collect(toList());
	}

	// This is a workaround for JUnit issue:
	// https://github.com/junit-team/junit5/issues/2104
	private static Optional<Method> findMethod(Class<?> clazz, String method, String parameterTypes) {
		final ClassLoader orig = Thread.currentThread()
			.getContextClassLoader();
		try {
			Thread.currentThread()
				.setContextClassLoader(clazz.getClassLoader());
			return ReflectionSupport.findMethod(clazz, method, parameterTypes);
		} finally {
			Thread.currentThread()
				.setContextClassLoader(orig);
		}
	}

	private static MethodSelector selectMethod(Class<?> testClass, MethodSelector selector) {
		return findMethod(testClass, selector.getMethodName(), selector.getParameterTypeNames())
			.map(method -> DiscoverySelectors.selectMethod(testClass, method))
			.orElse(null);
	}

	private void checkForMixedJUnit34(BundleDescriptor bd, Class<?> testClass) {
		try {
			// Don't hardcode TestCase.class here as we don't want to introduce
			// a hard dependency on that package
			Class<?> junit3TestCase = testClass.getClassLoader()
				.loadClass("junit.framework.TestCase");

			if (junit3TestCase.isAssignableFrom(testClass) && hasJUnit4Annotations(testClass)) {
				StaticFailureDescriptor mixedError = new StaticFailureDescriptor(bd.getUniqueId()
					.append("test", testClass.getName()), testClass.getName(),
					new JUnitException(
						"Class extends junit.framework.TestCase and has JUnit 4 annotations; annotations will be ignored"));
				bd.addChild(mixedError);
			}

		} catch (ClassNotFoundException e) {}
	}

	private boolean hasJUnit4Annotations(Class<?> clazz) {
		if (elementHasJUnit4Annotations(clazz))
			return true;

		for (Method m : clazz.getMethods()) {
			if (elementHasJUnit4Annotations(m)) {
				return true;
			}
		}
		return false;
	}

	private boolean elementHasJUnit4Annotations(AnnotatedElement element) {
		Annotation[] annotations = element.getAnnotations();
		if (annotations == null) {
			return false;
		}
		return Stream.of(annotations)
			.map(Annotation::annotationType)
			.map(Class::getName)
			.anyMatch(JUNIT4_ANNOTATIONS);
	}

	private List<DiscoverySelector> getSelectorsFromSuppliedSelectors(BundleDescriptor bd) {
		Bundle bundle = bd.getBundle();
		Bundle host = BundleUtils.getHost(bundle)
			.get();
		Set<String> testCases = BundleUtils.testCases(bundle)
			.collect(toSet());
		List<DiscoverySelector> selectors = Stream.concat( //
			classSelectors.stream()
				.map(selector -> {
					String className = selector.getClassName();
					if (testCases.contains(className)) {
						unresolvedClassNames.remove(className);
						info(
							() -> "removing discovered class: " + className + ", that leaves: " + unresolvedClassNames);
						Class<?> testClass = tryToResolveTestClass(host, className, bd);
						if (testClass != null) {
							if (resolvedClasses.add(testClass)) {
								checkForMixedJUnit34(bd, testClass);
								return selectClass(testClass);
							}
						}
					}
					return null;
				}), //
			methodSelectors.stream()
				.map(selector -> {
					String className = selector.getClassName();
					if (testCases.contains(className)) {
						unresolvedClassNames.remove(className);
						Class<?> testClass = tryToResolveTestClass(host, className, bd);
						if (testClass == null) {
							// Don't report individual unresolved method errors
							// if the test class failed to resolve.
							unresolvedMethodNames.remove(className);
						} else if (!resolvedClasses.contains(testClass)) {
							checkForMixedJUnit34(bd, testClass);
							MethodSelector methodSelector = selectMethod(testClass, selector);
							if (methodSelector != null) {
								unresolvedMethodNames.compute(className, (k, methods) -> {
									if (methods != null) {
										methods.remove(methodSelector.getMethodName());
										if (!methods.isEmpty()) {
											return methods;
										}
									}
									return null;
								});
							}
							return methodSelector;
						}
					}
					return null;
				}))
			.filter(Objects::nonNull)
			.collect(toList());
		info(() -> "Selectors: " + selectors);
		return selectors;
	}

	class SubDiscoveryRequest implements InvocationHandler {
		final List<DiscoverySelector> selectors;

		SubDiscoveryRequest(List<DiscoverySelector> selectors) {
			this.selectors = selectors;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName()
				.equals("getSelectorsByType")) {
				@SuppressWarnings("unchecked")
				Class<? extends DiscoverySelector> selectorType = (Class<? extends DiscoverySelector>) args[0];
				info(() -> "Getting selectors from sub-request for: " + selectorType);
				return Stream.concat( //
					request.getSelectorsByType(selectorType)
						.stream()
						.filter(selector -> !(selector instanceof ClassSelector
							|| selector instanceof MethodSelector || selector instanceof BundleSelector)),
					selectors.stream()
						.filter(selectorType::isInstance))
					.map(selectorType::cast)
					.collect(toList());
			}
			return method.invoke(request, args);
		}
	}

	EngineDiscoveryRequest buildSubRequest(List<DiscoverySelector> selectors) {
		return (EngineDiscoveryRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {
			EngineDiscoveryRequest.class
		}, new SubDiscoveryRequest(selectors));
	}
	private boolean computeChildren(BundleDescriptor bd) {
		final Bundle bundle = bd.getBundle();
		final List<DiscoverySelector> selectors;
		if (classSelectors.isEmpty() && methodSelectors.isEmpty() && isATestBundle.test(bundle)) {
			info(() -> "Getting selectors from test cases header");
			selectors = getSelectorsFromTestCasesHeader(bd);
		} else {
			info(() -> "Getting selectors from supplied selectors");
			selectors = getSelectorsFromSuppliedSelectors(bd);
		}
		info(() -> "Computed selectors: " + selectors);
		if (selectors.isEmpty() && bd.getChildren()
			.size() == 0) {
			return false;
		}
		EngineDiscoveryRequest subRequest = buildSubRequest(selectors);
		engines.forEach(engine -> {
			info(() -> "Processing engine: " + engine.getId() + " for bundle " + bundle);
			try {
				TestDescriptor engineDescriptor = engine.discover(subRequest, bd.getUniqueId()
					.append("sub-engine", engine.getId()));
				bd.addChild(engineDescriptor, engine);
				info(() -> "Finished processing engine: " + engine.getId() + " for bundle " + bundle);
			} catch (Exception e) {
				info(() -> "Error processing tests for engine: " + engine.getId() + " for bundle " + bundle + ": "
					+ e.getMessage(), e);
				StaticFailureDescriptor failedEngine = new StaticFailureDescriptor(
					misconfiguredEnginesDescriptor.getUniqueId()
						.append("sub-engine", engine.getId()),
					engine.getId(), e);
				misconfiguredEnginesDescriptor.addChild(failedEngine);
			}
		});
		return true;
	}
}
