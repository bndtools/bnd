package org.bndtools.applaunch;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	@Override
	public void start(BundleContext context) throws Exception {
		new LauncherTracker(context).open(true);
	}

	@Override
	public void stop(BundleContext context) throws Exception {}
}

class LauncherTracker extends ServiceTracker<Object, ServiceRegistration<ApplicationLauncher>> {

	private final Logger						log	= Logger.getLogger(Activator.class.getPackage()
		.getName());
	private ServiceReference<EnvironmentInfo>	envInfo;

	public LauncherTracker(BundleContext context) {
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
	public ServiceRegistration<ApplicationLauncher> addingService(ServiceReference<Object> reference) {
		setEquinoxConfigAppArgs(context);

		// Find and start the Equinox Application bundle
		boolean found = false;
		Bundle[] bundles = context.getBundles();
		for (Bundle bundle : bundles) {
			if ("org.eclipse.equinox.app".equals(getBsn(bundle))) {
				found = true;
				try {
					bundle.start();
				} catch (BundleException e) {
					log.log(Level.SEVERE,
						"Unable to start bundle org.eclipse.equinox.app. Eclipse application cannot start.", e);
				}
				break;
			}
		}
		if (!found)
			log.warning("Unable to find bundle org.eclipse.equinox.app. Eclipse application will not start.");

		// Register the ApplicationLauncher
		log.fine("Registering ApplicationLauncher service.");
		return context.registerService(ApplicationLauncher.class, new BndApplicationLauncher(context), null);
	}

	private void setEquinoxConfigAppArgs(BundleContext context) {
		String[] args = null;
		try {
			ServiceReference<?>[] serviceReferences = context.getServiceReferences("aQute.launcher.Launcher",
				"(launcher.arguments=*)");
			if (serviceReferences != null && serviceReferences.length > 0) {
				Object property = serviceReferences[0].getProperty("launcher.arguments");

				if (property instanceof String[]) {
					args = (String[]) property;

					envInfo = context.getServiceReference(EnvironmentInfo.class);
					if (envInfo != null) {
						EnvironmentInfo service = context.getService(envInfo);
						if (service instanceof EquinoxConfiguration) {
							EquinoxConfiguration equinoxConfig = (EquinoxConfiguration) service;
							equinoxConfig.setAppArgs(args);
						}
					}

					log.log(Level.FINE, "configured program arguments " + Arrays.toString(args));
				}
			} else {
				log.log(Level.SEVERE,
					"service aQute.launcher.Launcher with props launcher.arguments could not be retrieved");
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "command line parameters could not be configured.");
		}
	}

	@Override
	public void removedService(ServiceReference<Object> reference, ServiceRegistration<ApplicationLauncher> service) {
		if (envInfo != null)
			context.ungetService(envInfo);
		service.unregister();
	}

	private String getBsn(Bundle bundle) {
		String bsn = bundle.getHeaders()
			.get(Constants.BUNDLE_SYMBOLICNAME);
		int semiColonIndex = bsn.indexOf(';');
		if (semiColonIndex > -1)
			bsn = bsn.substring(0, semiColonIndex);
		return bsn;
	}
}
