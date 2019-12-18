package aQute.tester.bundle.engine.discovery;

import static aQute.tester.bundle.engine.BundleDescriptor.displayNameOf;
import static aQute.tester.bundle.engine.BundleEngine.CHECK_UNRESOLVED;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
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

	final boolean						testUnresolved;
	final Map<Long, BundleDescriptor>	bundleMap	= new HashMap<>();

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
	final Set<String>				unresolvedClasses	= new HashSet<>();
	final Set<Class<?>>				resolvedClasses		= new HashSet<>();
	private boolean					verbose				= false;

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
			.map(selector -> selectMethod(selector.getClassName(), selector.getMethodName(),
				selector.getMethodParameterTypes()))
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
			.forEach(unresolvedClasses::add);
		methodSelectors.stream()
			.map(MethodSelector::getClassName)
			.forEach(unresolvedClasses::add);
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

		if (testUnresolved && !unresolvedClasses.isEmpty()) {
			StaticFailureDescriptor unresolvedClassesDescriptor = new StaticFailureDescriptor(
				uniqueId.append("test", "unresolvedClasses"), "Unresolved classes");
			for (String unresolvedClass : unresolvedClasses) {
				unresolvedClassesDescriptor.addChild(new StaticFailureDescriptor(
					unresolvedClassesDescriptor.getUniqueId()
						.append("test", unresolvedClass),
					unresolvedClass, new JUnitException("Couldn't find class " + unresolvedClass + " in any bundle")));
			}
			descriptor.addChild(unresolvedClassesDescriptor);
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
			.forEach(unresolvedClasses::remove);
	}

	private List<DiscoverySelector> getSelectorsFromTestCasesHeader(BundleDescriptor bd) {
		Bundle bundle = bd.getBundle();
		Bundle host = BundleUtils.getHost(bundle)
			.get();
		return BundleUtils.testCases(bundle)
			.map(testcase -> {
				int index = testcase.indexOf('#');
				String className = (index < 0) ? testcase : testcase.substring(0, index);
				try {
					Class<?> testClass = host.loadClass(className);
					if (!resolvedClasses.contains(testClass)) {
						if (index < 0) {
							resolvedClasses.add(testClass);
							return selectClass(testClass);
						}
						MethodSelector parsed = selectMethod(testcase);
						return selectMethod(testClass, parsed.getMethodName(), parsed.getMethodParameterTypes());
					}
				} catch (ClassNotFoundException cnfe) {
					StaticFailureDescriptor unresolvedClassDescriptor = new StaticFailureDescriptor(bd.getUniqueId()
						.append("test", testcase), testcase, cnfe);
					bd.addChild(unresolvedClassDescriptor);
				}
				return null;
			})
			.filter(Objects::nonNull)
			.collect(toList());
	}

	private List<DiscoverySelector> getSelectorsFromSuppliedSelectors(BundleDescriptor bd) {
		List<DiscoverySelector> selectors = new ArrayList<>();
		Bundle bundle = bd.getBundle();
		Bundle host = BundleUtils.getHost(bundle)
			.get();
		Set<String> testCases = BundleUtils.testCases(bundle)
			.collect(toSet());
		classSelectors.stream()
			.map(ClassSelector::getClassName)
			.filter(testCases::contains)
			.forEach(className -> {
				try {
					Class<?> testClass = host.loadClass(className);
					unresolvedClasses.remove(className);
					info(() -> "removing resolved class: " + testClass + ", that leaves: " + unresolvedClasses);
					if (resolvedClasses.add(testClass)) {
						selectors.add(selectClass(testClass));
					}
				} catch (ClassNotFoundException cnfe) {
					info(() -> "Unresolved class: " + className + ", bundle: " + bundle.getSymbolicName(), cnfe);
				}
			});
		methodSelectors.stream()
			.filter(selector -> testCases.contains(selector.getClassName()))
			.forEach(selector -> {
				try {
					Class<?> testClass = host.loadClass(selector.getClassName());
					if (!resolvedClasses.contains(testClass)) {
						selectors
							.add(selectMethod(testClass, selector.getMethodName(), selector.getMethodParameterTypes()));
						unresolvedClasses.remove(selector.getClassName());
					}
				} catch (ClassNotFoundException cnfe) {
					info(() -> "Unresolved class: " + selector.getClassName() + ", bundle: " + bundle.getSymbolicName(),
						cnfe);
				}
			});
		info(() -> "Selectors: " + selectors);
		return selectors;
	}

	public class SubDiscoveryRequest implements EngineDiscoveryRequest {
		private final List<DiscoverySelector> selectors;

		SubDiscoveryRequest(List<DiscoverySelector> selectors) {
			this.selectors = selectors;
		}

		@Override
		public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> selectorType) {
			info(() -> "Getting selectors from sub-request for: " + selectorType);
			if (selectorType.equals(ClassSelector.class) || selectorType.equals(MethodSelector.class)) {
				return selectors.stream()
					.filter(selectorType::isInstance)
					.map(selectorType::cast)
					.collect(toList());
			}
			if (selectorType.equals(BundleSelector.class)) {
				return new ArrayList<>();
			}
			return request.getSelectorsByType(selectorType);
		}

		@Override
		public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> filterType) {
			return request.getFiltersByType(filterType);
		}

		@Override
		public ConfigurationParameters getConfigurationParameters() {
			return request.getConfigurationParameters();
		}
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
		if (selectors.isEmpty()) {
			return false;
		}
		SubDiscoveryRequest subRequest = new SubDiscoveryRequest(selectors);
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
			}
		});
		return true;
	}
}
