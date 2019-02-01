package org.bndtools.applaunch;

import java.util.logging.Level;
import java.util.logging.Logger;

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
	public void stop(BundleContext context) throws Exception {
	}
}

class LauncherTracker extends ServiceTracker<Object, ServiceRegistration<ApplicationLauncher>> {

	private final Logger log = Logger.getLogger(Activator.class.getPackage()
		.getName());

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

	@Override
	public void removedService(ServiceReference<Object> reference, ServiceRegistration<ApplicationLauncher> service) {
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