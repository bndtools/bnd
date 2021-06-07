package bndtools.m2e;

import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectManager;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import aQute.bnd.osgi.Constants;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator {

	private MavenProjectChangedListenersTracker projectChangedListenersTracker;

	@Override
	public void start(BundleContext context) throws Exception {
		projectChangedListenersTracker = new MavenProjectChangedListenersTracker();
		MavenProjectManager projectManager = MavenPluginActivator.getDefault()
			.getMavenProjectManager();
		projectManager.addMavenProjectChangedListener(projectChangedListenersTracker);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		MavenProjectManager projectManager = MavenPluginActivator.getDefault()
			.getMavenProjectManager();
		projectManager.removeMavenProjectChangedListener(projectChangedListenersTracker);
		projectChangedListenersTracker.close();
	}

}
