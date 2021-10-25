package aQute.remote.agent;

import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public final class PackageWirings {

	private PackageWirings() {
		throw new IllegalAccessError("Cannot be instantiated");
	}

	public static boolean isWired(String packageName, BundleContext context) {
		final BundleWiring wiring = context.getBundle()
			.adapt(BundleWiring.class);
		for (BundleWire wire : wiring.getRequiredWires(PACKAGE_NAMESPACE)) {
			String pkg = (String) wire.getCapability()
				.getAttributes()
				.get(PACKAGE_NAMESPACE);
			BundleWiring providerWiring = wire.getProviderWiring();
			if (pkg.startsWith(packageName) && providerWiring != null) {
				return true;
			}
		}
		return false;
	}

	public static boolean isScrWired(BundleContext context) {
		return PackageWirings.isWired("org.osgi.service.component.runtime", context);
	}

	public static boolean isConfigAdminWired(BundleContext context) {
		return PackageWirings.isWired("org.osgi.service.cm", context);
	}

	public static boolean isMetatypeWired(BundleContext context) {
		return PackageWirings.isWired("org.osgi.service.metatype", context);
	}

	public static boolean isEventAdminWired(BundleContext context) {
		return PackageWirings.isWired("org.osgi.service.event", context);
	}

	public static boolean isLogWired(BundleContext context) {
		return PackageWirings.isWired("org.osgi.service.log", context);
	}

}
