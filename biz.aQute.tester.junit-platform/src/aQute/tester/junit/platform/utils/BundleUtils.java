package aQute.tester.junit.platform.utils;

import java.util.Optional;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import aQute.lib.strings.Strings;

public final class BundleUtils {

	public static Optional<Bundle> getHost(Bundle bundle) {
		return Optional.ofNullable(bundle.adapt(BundleWiring.class))
			.map(wiring -> wiring.getRequiredWires(HostNamespace.HOST_NAMESPACE)
				.stream()
				.map(BundleWire::getProviderWiring)
				.map(BundleWiring::getBundle)
				.findFirst()
				.orElse(bundle));
	}

	public static Stream<Bundle> getFragments(Bundle bundle) {
		return Optional.ofNullable(bundle.adapt(BundleWiring.class))
			.map(wiring -> wiring.getProvidedWires(HostNamespace.HOST_NAMESPACE)
				.stream()
				.map(BundleWire::getRequirerWiring)
				.map(BundleWiring::getBundle))
			.orElseGet(Stream::empty);
	}

	public static Optional<ClassLoader> getClassLoader(Bundle bundle) {
		return getHost(bundle).map(host -> host.adapt(BundleWiring.class))
			.map(BundleWiring::getClassLoader);
	}

	public static boolean isResolved(Bundle bundle) {
		return (bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0;
	}

	public static boolean isNotResolved(Bundle bundle) {
		return !isResolved(bundle);
	}

	public static boolean hasTests(Bundle bundle) {
		return bundle.getHeaders()
			.get(aQute.bnd.osgi.Constants.TESTCASES) != null;
	}

	public static boolean hasNoTests(Bundle bundle) {
		return !hasTests(bundle);
	}

	public static boolean isFragment(Bundle bundle) {
		return (bundle.adapt(BundleRevision.class)
			.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
	}

	public static boolean isNotFragment(Bundle bundle) {
		return !isFragment(bundle);
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
