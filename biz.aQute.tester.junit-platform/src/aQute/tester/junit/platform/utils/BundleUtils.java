package aQute.tester.junit.platform.utils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

import aQute.lib.strings.Strings;

public final class BundleUtils {

	public static Optional<Bundle> getHost(Bundle bundle) {
		if (isNotFragment(bundle)) {
			return Optional.of(bundle);
		}
		return Optional.ofNullable(bundle.adapt(BundleRevision.class))
			.map(BundleRevision::getWiring)
			.flatMap(wiring -> wiring.getRequiredWires(BundleRevision.HOST_NAMESPACE)
				.stream()
				.map(wire -> wire.getProviderWiring()
					.getBundle())
				.findFirst());
	}

	public static List<Bundle> getFragments(Bundle bundle) {
		if (isFragment(bundle)) {
			return Collections.emptyList();
		}
		return Optional.ofNullable(bundle.adapt(BundleRevision.class))
			.map(BundleRevision::getWiring)
			.map(wiring -> wiring.getProvidedWires(BundleRevision.HOST_NAMESPACE)
				.stream()
				.map(wire -> wire.getRequirerWiring()
					.getBundle())
				.collect(Collectors.toList()))
			.orElseGet(Collections::emptyList);
	}

	public static ClassLoader getClassLoader(Bundle bundle) {
		return getHost(bundle).map(host -> host.adapt(BundleWiring.class))
			.map(BundleWiring::getClassLoader)
			.orElse(null);
	}

	public static boolean isNotResolved(Bundle bundle) {
		return (bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) != 0;
	}

	public static boolean isResolved(Bundle bundle) {
		return !isNotResolved(bundle);
	}

	public static boolean isNotAttached(Bundle bundle) {
		return !isAttached(bundle);
	}

	public static boolean isAttached(Bundle bundle) {
		return getHost(bundle).isPresent();
	}

	public static boolean hasTests(Bundle bundle) {
		return bundle.getHeaders()
			.get(aQute.bnd.osgi.Constants.TESTCASES) != null;
	}

	public static boolean isFragment(Bundle bundle) {
		return bundle.getHeaders()
			.get(aQute.bnd.osgi.Constants.FRAGMENT_HOST) != null;
	}

	public static boolean isNotFragment(Bundle bundle) {
		return !isFragment(bundle);
	}

	public static boolean hasNoTests(Bundle bundle) {
		return !hasTests(bundle);
	}

	public static Stream<String> testCases(Bundle bundle) {
		return testCases(bundle.getHeaders()
			.get(aQute.bnd.osgi.Constants.TESTCASES));
	}

	public static Stream<String> testCases(String testCases) {
		return Strings.splitAsStream(testCases)
			.map(entry -> entry.replace(':', '#'));
	}

}
