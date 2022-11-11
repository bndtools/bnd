package bndtools.m2e;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.osgi.Constants;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator {

	/**
	 * M2E version 2 has implicit support for whiteboard listeners and registers
	 * services. This activator helps us to work in both situations.
	 */
	private boolean								isM2E_v2;

	private MavenProjectChangedListenersTracker projectChangedListenersTracker;

	@Override
	public void start(BundleContext context) throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(MavenPlugin.class);
		isM2E_v2 = bundle.getVersion()
			.getMajor() == 2;

		if (!isM2E_v2) {
			projectChangedListenersTracker = new MavenProjectChangedListenersTracker();
			IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
			projectManager.addMavenProjectChangedListener(projectChangedListenersTracker);

			registerM2EServices(context);
		}
	}

	/**
	 * These services should only be registered if running on M2E 1.x
	 *
	 * @param context
	 */
	private void registerM2EServices(BundleContext context) {
		context.registerService(IMaven.class, MavenPlugin.getMaven(), null);
		context.registerService(IMavenProjectRegistry.class, MavenPlugin.getMavenProjectRegistry(), null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (!isM2E_v2) {
			IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
			projectManager.removeMavenProjectChangedListener(projectChangedListenersTracker);
			projectChangedListenersTracker.close();
		}
	}

}
