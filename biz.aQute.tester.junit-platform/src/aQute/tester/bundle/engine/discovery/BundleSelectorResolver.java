package aQute.tester.bundle.engine.discovery;

import static aQute.tester.bundle.engine.BundleEngine.CHECK_UNRESOLVED;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
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

	public static String uniqueIdOf(Bundle bundle) {
		return bundle.getSymbolicName() + ';' + bundle.getVersion();
	}

	public static String displayNameOf(Bundle bundle) {
		final Optional<String> bundleName = Optional.ofNullable(bundle.getHeaders()
			.get(aQute.bnd.osgi.Constants.BUNDLE_NAME));
		return "[" + bundle.getBundleId() + "] " + bundleName.orElse(bundle.getSymbolicName()) + ';'
			+ bundle.getVersion();
	}

	final EngineDiscoveryRequest								request;
	final EngineDescriptor										b;
	final StaticFailureDescriptor								misconfiguredEnginesDescriptor;
	final StaticFailureDescriptor								unresolvedBundlesDescriptor;
	final StaticFailureDescriptor								unattachedFragmentsDescriptor;
	final List<BundleSelector>									bundleSelectors;
	final List<String>											classSelectors;
	final List<MethodSelector>									methodSelectors;
	final Predicate<Bundle>										isATestBundle;
	final Predicate<Bundle>										isNotATestBundle;
	final Bundle[]												allBundles;
	final Set<TestEngine>										engines;
	final Function<BundleDescriptor, List<DiscoverySelector>>	selectorResolver;
	final Set<String>											unresolvedClasses	= new HashSet<>();
	final Set<Class<?>>											resolvedClasses		= new HashSet<>();
	private boolean												verbose				= false;

	public BundleSelectorResolver(BundleContext context, EngineDiscoveryRequest request, EngineDescriptor b2) {
		this.request = request;
		this.b = b2;
		final UniqueId uniqueId = b2.getUniqueId();
		testUnresolved = request.getConfigurationParameters()
			.getBoolean(CHECK_UNRESOLVED)
			.orElse(true);

		misconfiguredEnginesDescriptor = new StaticFailureDescriptor(uniqueId.append("test", "misconfiguredEngines"),
			"Misconfigured TestEngines");
		unresolvedBundlesDescriptor = new StaticFailureDescriptor(uniqueId.append("test", "unresolvedBundles"),
			"Unresolved bundles");
		unattachedFragmentsDescriptor = new StaticFailureDescriptor(uniqueId.append("test", "unattachedFragments"),
			"Unattached fragments");

		classSelectors = request.getSelectorsByType(ClassSelector.class)
			.stream()
			.map(ClassSelector::getClassName)
			.collect(Collectors.toList());

		methodSelectors = request.getSelectorsByType(MethodSelector.class)
			.stream()
			.map(x -> selectMethod(x.getClassName(), x.getMethodName(), x.getMethodParameterTypes()))
			.collect(Collectors.toList());

		bundleSelectors = request.getSelectorsByType(BundleSelector.class);

		System.err.println("our bundleSelectors: " + BundleSelector.class.getClassLoader());
		System.err.println("supplied bundleSelectors: " + request.getSelectorsByType(DiscoverySelector.class)
			.stream()
			.map(Object::getClass)
			.map(Class::getClassLoader)
			.map(Object::toString)
			.collect(Collectors.joining(",")));

		allBundles = context.getBundles();

		isATestBundle = bundleSelectors.isEmpty() ? BundleUtils::hasTests
			: bundleSelectors.stream()
				.map(selector -> (Predicate<Bundle>) bundle -> {
					if (bundleMatches(selector, bundle)) {
						return true;
					}
					Bundle host = BundleUtils.getHost(bundle)
						.orElse(null);
					return (host != null && host != bundle && bundleMatches(selector, host));
				})
				.reduce(Predicate::or)
				.orElse(x -> false);
		isNotATestBundle = x -> !isATestBundle.test(x);

		engines = new HashSet<>(11);
		findEngines();

		selectorResolver = classSelectors.isEmpty() && methodSelectors.isEmpty() ? this::getSelectorsFromTestCasesHeader
			: this::getSelectorsFromSuppliedSelectors;
		unresolvedClasses.addAll(classSelectors);
		// TODO: Test
		// for (MethodSelector mSelector : methodSelectors) {
		// unresolvedClasses.add(mSelector.getClassName());
		// }
	}

	private static boolean bundleMatches(BundleSelector selector, Bundle bundle) {
		return selector.getVersion()
			.includes(bundle.getVersion())
			&& selector.getSymbolicName()
				.contentEquals(bundle.getSymbolicName());
	}

	private void findEngines() {
		for (Bundle bundle : allBundles) {
			final ClassLoader cl = BundleUtils.getClassLoader(bundle);
			if (cl != null) {
				try {
					StreamSupport.stream(ServiceLoader.load(TestEngine.class, cl)
						.spliterator(), false)
						.filter(e -> e.getId() != BundleEngine.ENGINE_ID)
						.forEach(engines::add);
				} catch (Error e) {
					if (testUnresolved) {
						final String bundleDesc = uniqueIdOf(bundle);
						final StaticFailureDescriptor bd = new StaticFailureDescriptor(
							misconfiguredEnginesDescriptor.getUniqueId()
								.append("bundle", bundleDesc),
							displayNameOf(bundle), e);
						misconfiguredEnginesDescriptor.addChild(bd);
					}
				}
			}
		}
	}

	public TestDescriptor resolve() {
		final UniqueId uniqueId = b.getUniqueId();
		Stream.of(allBundles)
			.filter(isATestBundle)
			.filter(BundleUtils::isNotFragment)
			.filter(BundleUtils::isNotResolved)
			.forEach(bundle -> {
				final String bundleDesc = uniqueIdOf(bundle);
				try {
					bundle.start();
				} catch (BundleException e) {
					UniqueId bundleId = uniqueId.append("bundle", bundleDesc);
					BundleDescriptor bd = new BundleDescriptor(bundle, bundleId, e);
					b.addChild(bd);
					markClassesResolved(bundle);
				}
			});

		// Attempt to start the bundles before checking for unattached
		// fragments, as the fragments don't attach until their host bundle
		// starts.
		Stream.of(allBundles)
			.filter(isNotATestBundle)
			.filter(BundleUtils::isNotFragment)
			.filter(BundleUtils::isNotResolved)
			.forEach(bundle -> {
				final String bundleDesc = uniqueIdOf(bundle);
				try {
					bundle.start();
				} catch (BundleException e) {
					if (testUnresolved) {
						unresolvedBundlesDescriptor
							.addChild(new StaticFailureDescriptor(unresolvedBundlesDescriptor.getUniqueId()
								.append("bundle", bundleDesc), displayNameOf(bundle), e));
					}
				}
			});

		if (testUnresolved) {
			Stream.of(allBundles)
				.filter(BundleUtils::isFragment)
				.filter(isNotATestBundle)
				.filter(BundleUtils::isNotAttached)
				.forEach(bundle -> {
					final String bundleDesc = uniqueIdOf(bundle);
					unattachedFragmentsDescriptor.addChild(new StaticFailureDescriptor(
						unattachedFragmentsDescriptor.getUniqueId()
							.append("bundle", bundleDesc),
						displayNameOf(bundle), new JUnitException("Fragment was not attached to a host bundle")));
				});
		}

		Stream.of(allBundles)
			.filter(isATestBundle)
			.filter(BundleUtils::isFragment)
			.filter(BundleUtils::isNotAttached)
			.forEach(bundle -> {
				final String bundleDesc = uniqueIdOf(bundle);
				UniqueId bundleId = uniqueId.append("bundle", bundleDesc);
				BundleDescriptor bd = new BundleDescriptor(bundle, bundleId,
					new BundleException("Test fragment was not attached to a host bundle"));
				b.addChild(bd);
				markClassesResolved(bundle);
			});

		Stream.of(allBundles)
			.filter(BundleUtils::isResolved)
			.filter(BundleUtils::isNotFragment)
			.forEach(bundle -> {
				addEnginesToBundle(bundle);
			});

		Stream.of(allBundles)
			.filter(isATestBundle)
			.filter(BundleUtils::isFragment)
			.filter(BundleUtils::isAttached)
			.forEach(bundle -> {
				info(() -> "Attached test fragment: " + bundle);
				Bundle parent = BundleUtils.getHost(bundle)
					.get();
				BundleDescriptor bd = bundleMap.get(parent.getBundleId());
				if (bd == null) {
					UniqueId parentId = uniqueId.append("bundle", uniqueIdOf(parent));
					bd = new BundleDescriptor(parent, parentId);
					b.addChild(bd);
					bundleMap.put(parent.getBundleId(), bd);
				}
				BundleDescriptor fd = new BundleDescriptor(bundle, bd.getUniqueId()
					.append("fragment", uniqueIdOf(bundle)), null);
				bd.addChild(fd);
				addEnginesToBundleDescriptor(fd);
			});

		Stream.of(misconfiguredEnginesDescriptor, unresolvedBundlesDescriptor, unattachedFragmentsDescriptor)
			.filter(StaticFailureDescriptor::hasChildren)
			.forEach(b::addChild);

		if (engines.isEmpty()) {
			TestDescriptor t = new StaticFailureDescriptor(uniqueId.append("test", "noEngines"), "Initialization Error",
				new JUnitException("Couldn't find any registered TestEngines"));
			b.addChild(t);
			return b;
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
			b.addChild(unresolvedClassesDescriptor);
		}
		info(() -> dump(b, ""));
		return b;
	}

	void info(Supplier<String> msg, Throwable cause) {
		if (verbose) {
			System.err.println(msg.get());
			cause.printStackTrace();
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
		List<DiscoverySelector> selectors = new ArrayList<>();
		Bundle bundle = bd.getBundle();
		BundleUtils.testCases(bundle)
			.forEach(testcase -> {
				int index = testcase.indexOf('#');
				String className = (index < 0) ? testcase : testcase.substring(0, index);
				try {
					Class<?> testClass = BundleUtils.getHost(bundle)
						.get()
						.loadClass(className);
					if (!resolvedClasses.contains(testClass)) {
						resolvedClasses.add(testClass);
						if (index < 0) {
							selectors.add(selectClass(testClass));
						} else {
							MethodSelector methodSelector = selectMethod(testcase);
							selectors.add(selectMethod(testClass, methodSelector.getMethodName(),
								methodSelector.getMethodParameterTypes()));
						}
					}
				} catch (ClassNotFoundException cnfe) {
					final StaticFailureDescriptor unresolvedClassDescriptor = new StaticFailureDescriptor(
						bd.getUniqueId()
							.append("test", testcase),
						testcase, cnfe);
					bd.addChild(unresolvedClassDescriptor);
				}
			});
		return selectors;
	}

	private List<DiscoverySelector> getSelectorsFromSuppliedSelectors(BundleDescriptor bd) {
		List<DiscoverySelector> selectors = new ArrayList<>();
		Bundle bundle = bd.getBundle();
		classSelectors.forEach(x -> {
			try {
				Class<?> testClass = BundleUtils.getHost(bundle)
					.get()
					.loadClass(x);
				unresolvedClasses.remove(x);
				info(() -> "removing resolved class: " + testClass + ", that leaves: " + unresolvedClasses);
				if (!resolvedClasses.contains(testClass)) {
					resolvedClasses.add(testClass);
					selectors.add(selectClass(testClass));
				}
			} catch (ClassNotFoundException cnfe) {
				info(() -> "Unresolved class: " + cnfe + ", bundle: " + bundle.getSymbolicName(), cnfe);
			}
		});
		methodSelectors.forEach(x -> {
			try {
				Class<?> testClass = BundleUtils.getHost(bundle)
					.get()
					.loadClass(x.getClassName());
				selectors.add(selectMethod(testClass, x.getMethodName(), x.getMethodParameterTypes()));
				unresolvedClasses.remove(x.getClassName());
			} catch (ClassNotFoundException cnfe) {}
		});
		info(() -> "Selectors: " + selectors);
		return selectors;
	}

	public class SubDiscoveryRequest implements EngineDiscoveryRequest {
		List<DiscoverySelector> selectors;

		public SubDiscoveryRequest(List<DiscoverySelector> selectors) {
			this.selectors = selectors;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> selectorType) {
			info(() -> "Getting selectors from sub-request for: " + selectorType);
			return doGetSelectorsByType(selectorType);
		}

		private <T extends DiscoverySelector> List<T> doGetSelectorsByType(Class<T> selectorType) {
			if (selectorType.equals(ClassSelector.class) || selectorType.equals(MethodSelector.class)) {
				@SuppressWarnings("unchecked")
				List<T> retval = selectors.stream()
					.filter(x -> selectorType.isAssignableFrom(x.getClass()))
					.map(x -> (T) x)
					.collect(Collectors.toList());
				return retval;
			} else if (selectorType.equals(BundleSelector.class)) {
				return Collections.emptyList();
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

	private void addEnginesToBundleDescriptor(BundleDescriptor bd) {
		if (engines.isEmpty()) {
			return;
		}
		final Bundle bundle = bd.getBundle();
		info(() -> "Adding engines to bundle descriptor: " + bundle.getSymbolicName());
		List<DiscoverySelector> classFilters = null;
		if (classSelectors.isEmpty() && methodSelectors.isEmpty() && isATestBundle.test(bundle)) {
			classFilters = getSelectorsFromTestCasesHeader(bd);
		} else {
			classFilters = this.getSelectorsFromSuppliedSelectors(bd);
		}

		if (classFilters != null && !classFilters.isEmpty()) {
			SubDiscoveryRequest edr = new SubDiscoveryRequest(classFilters);

			engines.forEach(engine -> {
				info(() -> "Processing engine: " + engine.getId());
				TestDescriptor engineDescriptor = engine.discover(edr, bd.getUniqueId()
					.append("sub-engine", engine.getId()));
				bd.addEngine(engineDescriptor, engine);
			});
		}
	}

	private void addEnginesToBundle(Bundle bundle) {
		info(() -> "Performing engine discovery for bundle: " + bundle.getSymbolicName());
		final String bundleDesc = uniqueIdOf(bundle);
		UniqueId bundleId = b.getUniqueId()
			.append("bundle", bundleDesc);
		BundleDescriptor bd = new BundleDescriptor(bundle, bundleId);
		final List<DiscoverySelector> classFilters;
		if (classSelectors.isEmpty() && methodSelectors.isEmpty() && isATestBundle.test(bundle)) {
			info(() -> "Getting selectors from test cases header");
			classFilters = getSelectorsFromTestCasesHeader(bd);
		} else {
			info(() -> "Getting selectors from supplied selectors");
			classFilters = this.getSelectorsFromSuppliedSelectors(bd);
		}
		info(() -> "Compiled class filters: " + classFilters);

		if (classFilters != null && !classFilters.isEmpty()) {
			SubDiscoveryRequest edr = new SubDiscoveryRequest(classFilters);

			b.addChild(bd);
			bundleMap.put(bundle.getBundleId(), bd);

			engines.forEach(engine -> {
				info(() -> "Processing engine: " + engine.getId() + " for bundle " + bundle);
				TestDescriptor engineDescriptor = engine.discover(edr, bd.getUniqueId()
					.append("sub-engine", engine.getId()));
				bd.addEngine(engineDescriptor, engine);
				info(() -> "Finished processing engine: " + engine.getId() + " for bundle " + bundle);
			});
		}
	}
}
