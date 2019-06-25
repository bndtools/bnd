package aQute.tester.junit.platform.utils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

public final class BundleUtils {

	public static Optional<Bundle> getHost(Bundle bundle) {
		if (isNotFragment(bundle)) {
			return Optional.of(bundle);
		}
		return Optional.ofNullable(bundle.adapt(BundleRevision.class))
			.filter(revision -> revision.getWiring() != null)
			.map(revision -> revision.getWiring()
				.getRequiredWires(BundleRevision.HOST_NAMESPACE))
			.flatMap(wires -> wires.stream()
				.map(wire -> wire.getProviderWiring()
					.getBundle())
				.findFirst());
	}

	public static List<Bundle> getFragments(Bundle bundle) {
		if (isNotFragment(bundle)) {
			BundleRevision revision = bundle.adapt(BundleRevision.class);
			if (revision != null) {
				BundleWiring wiring = revision.getWiring();
				if (wiring != null) {
					return wiring.getProvidedWires(BundleRevision.HOST_NAMESPACE)
						.stream()
						.map(wire -> wire.getRequirerWiring()
							.getBundle())
						.collect(Collectors.toList());
				}
			}
		}
		return Collections.emptyList();
	}

	public static ClassLoader getClassLoader(Bundle bundle) {
		Optional<Bundle> host = getHost(bundle);
		if (!host.isPresent()) {
			return null;
		}
		BundleWiring wiring = host.get()
			.adapt(BundleWiring.class);
		return wiring != null ? wiring.getClassLoader() : null;
	}

	public static boolean isNotResolved(Bundle bundle) {
		return (bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) != 0;
	}

	public static boolean isResolved(Bundle bundle) {
		return !isNotResolved(bundle);
	}

	public static boolean isNotAttached(Bundle bundle) {
		return !BundleUtils.isAttached(bundle);
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

}
