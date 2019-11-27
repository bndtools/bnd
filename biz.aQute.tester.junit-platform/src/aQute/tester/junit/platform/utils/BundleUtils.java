package aQute.tester.junit.platform.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import aQute.lib.strings.Strings;

public final class BundleUtils {

	private static final Attributes.Name TESTCASES = new Attributes.Name(aQute.bnd.osgi.Constants.TESTCASES);

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
		return testCases(bundle).anyMatch(s -> true);
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
		URL url = bundle.getEntry("META-INF/MANIFEST.MF");
		if (url == null) {
			return Stream.empty();
		}
		try (InputStream in = url.openStream()) {
			Manifest manifest = new Manifest(in);
			return testCases(manifest.getMainAttributes()
				.getValue(TESTCASES));
		} catch (IOException e) {
			return Stream.empty();
		}
	}

	public static Stream<String> testCases(String testCases) {
		return Strings.splitQuotedAsStream(testCases, false)
			.map(entry -> entry.replace(':', '#'));
	}
}
