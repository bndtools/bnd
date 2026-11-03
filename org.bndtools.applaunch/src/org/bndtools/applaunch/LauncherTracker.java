package org.bndtools.applaunch;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Waits for the bnd launcher to be ready, applies the Eclipse runtime options
 * found in the program arguments, starts the Equinox application container
 * and registers the {@link ApplicationLauncher} which runs the Eclipse
 * application on the main thread.
 */
class LauncherTracker extends ServiceTracker<Object, ServiceRegistration<?>> {

	private static final String					LAUNCHER_ARGUMENTS	= "launcher.arguments";
	private static final String					EQUINOX_APP			= "org.eclipse.equinox.app";

	// Location properties and the filter of the Location service managing them
	private static final Map<String, String>	LOCATIONS			= Map.of(
		RuntimeOptions.PROP_CONFIG_AREA, Location.CONFIGURATION_FILTER,
		RuntimeOptions.PROP_INSTALL_AREA, Location.INSTALL_FILTER,
		RuntimeOptions.PROP_INSTANCE_AREA, Location.INSTANCE_FILTER,
		RuntimeOptions.PROP_USER_AREA, Location.USER_FILTER);

	private final Logger						log					= Logger.getLogger(Activator.class.getPackage()
		.getName());

	LauncherTracker(BundleContext context) {
		super(context, createFilter(), null);
	}

	private static Filter createFilter() {
		try {
			return FrameworkUtil.createFilter("(&(objectClass=aQute.launcher.Launcher)(launcher.ready=true))"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ServiceRegistration<?> addingService(ServiceReference<Object> reference) {
		RuntimeOptions options = RuntimeOptions.parse(launcherArguments(reference));
		configure(options);
		if (options.initialize()) {
			log.info("-initialize specified: registering main-thread callback without starting an application.");
			return BndApplicationLauncher.registerInitializationCallback(context);
		}
		startEquinoxApp();

		log.fine("Registering ApplicationLauncher service.");
		return context.registerService(ApplicationLauncher.class, new BndApplicationLauncher(context), null);
	}

	@Override
	public void removedService(ServiceReference<Object> reference, ServiceRegistration<?> service) {
		service.unregister();
	}

	private static String[] launcherArguments(ServiceReference<?> reference) {
		Object property = reference.getProperty(LAUNCHER_ARGUMENTS);
		return property instanceof String[] ? (String[]) property : new String[0];
	}

	private void configure(RuntimeOptions options) {
		ServiceReference<EnvironmentInfo> reference = context.getServiceReference(EnvironmentInfo.class);
		EnvironmentInfo envInfo = reference == null ? null : context.getService(reference);
		if (envInfo == null) {
			log.warning("EnvironmentInfo service not available; Eclipse runtime options are ignored: "
				+ Arrays.toString(options.allArgs()));
			return;
		}
		try {
			if (envInfo instanceof EquinoxConfiguration) {
				EquinoxConfiguration config = (EquinoxConfiguration) envInfo;
				config.setAllArgs(options.allArgs());
				config.setFrameworkArgs(options.frameworkArgs());
				config.setAppArgs(options.applicationArgs());
			} else {
				log.warning("EnvironmentInfo is not an Equinox configuration; command line arguments are not published: "
					+ Arrays.toString(options.allArgs()));
			}
			options.properties()
				.forEach((key, value) -> apply(envInfo, key, value));
			log.log(Level.FINE, "Configured framework arguments {0}, application arguments {1}, properties {2}",
				new Object[] {
					Arrays.toString(options.frameworkArgs()), Arrays.toString(options.applicationArgs()),
					options.properties()
				});
		} finally {
			context.ungetService(reference);
		}
	}

	void apply(EnvironmentInfo envInfo, String key, String value) {
		String filter = LOCATIONS.get(key);
		if (filter != null) {
			setLocation(key, value, filter);
		} else if (RuntimeOptions.PROP_CLEAN.equals(key)) {
			log.warning("-clean cannot clear Equinox caches after framework startup; set osgi.clean=true with -runproperties before the framework starts.");
		} else if (RuntimeOptions.PROP_DEBUG.equals(key) || RuntimeOptions.PROP_DEV.equals(key)
			|| RuntimeOptions.PROP_CONSOLE.equals(key)) {
			log.warning(MessageFormat.format(
				"{0} cannot be applied after framework startup; set it with -runproperties before the framework starts.",
				key));
		} else if (RuntimeOptions.PROP_FRAMEWORK.equals(key)) {
			log.warning(MessageFormat.format(
				"{0} cannot be changed to {1} after the framework has started; use -runfw instead.", key, value));
		} else {
			envInfo.setProperty(key, value);
		}
	}

	private void setLocation(String property, String value, String filter) {
		URL url;
		try {
			url = RuntimeOptions.toLocationURL(value);
		} catch (IOException e) {
			log.log(Level.WARNING, MessageFormat.format("Unable to set {0} to {1}", property, value), e);
			return;
		}
		if (url == null) {
			log.warning(MessageFormat.format(
				"{0}={1} cannot be applied after framework startup; set it with -runproperties before the framework starts.",
				property, value));
			return;
		}
		Collection<ServiceReference<Location>> references;
		try {
			references = context.getServiceReferences(Location.class, filter);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException(e);
		}
		if (references.isEmpty()) {
			log.warning(MessageFormat.format("No Location service for {0}; the value {1} is ignored.", property, value));
			return;
		}
		ServiceReference<Location> reference = references.iterator()
			.next();
		Location location = context.getService(reference);
		try {
			if (location.isSet()) {
				if (!sameLocation(location.getURL(), url)) {
					log.warning(MessageFormat.format(
						"{0} is already set to {1} and cannot be changed to {2} after the framework has started; set it with -runproperties instead.",
						property, location.getURL(), value));
				}
			} else {
				location.set(url, false);
				log.log(Level.FINE, "Set {0} to {1}", new Object[] {
					property, location.getURL()
				});
			}
		} catch (IOException | IllegalStateException e) {
			log.log(Level.WARNING, MessageFormat.format("Unable to set {0} to {1}", property, value), e);
		} finally {
			context.ungetService(reference);
		}
	}

	private static boolean sameLocation(URL current, URL requested) throws IOException {
		if ("file".equalsIgnoreCase(current.getProtocol()) && "file".equalsIgnoreCase(requested.getProtocol())) {
			return toFile(current).getCanonicalFile()
				.equals(toFile(requested).getCanonicalFile());
		}
		return current.toExternalForm()
			.equals(requested.toExternalForm());
	}

	private static File toFile(URL url) {
		try {
			URI uri = new URI(url.toExternalForm());
			String path = uri.isOpaque() ? uri.getSchemeSpecificPart() : uri.getPath();
			return new File(path);
		} catch (URISyntaxException e) {
			// Equinox may produce file URLs with unescaped characters
			return new File(url.getPath());
		}
	}

	private void startEquinoxApp() {
		for (Bundle bundle : context.getBundles()) {
			if (EQUINOX_APP.equals(getBsn(bundle))) {
				try {
					bundle.start();
				} catch (BundleException e) {
					log.log(Level.SEVERE,
						"Unable to start bundle org.eclipse.equinox.app. Eclipse application cannot start.", e);
				}
				return;
			}
		}
		log.warning("Unable to find bundle org.eclipse.equinox.app. Eclipse application will not start.");
	}

	private static String getBsn(Bundle bundle) {
		String bsn = bundle.getHeaders()
			.get(Constants.BUNDLE_SYMBOLICNAME);
		if (bsn == null) {
			return null;
		}
		int semiColonIndex = bsn.indexOf(';');
		if (semiColonIndex > -1)
			bsn = bsn.substring(0, semiColonIndex);
		return bsn;
	}
}
