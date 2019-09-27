package org.bndtools.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.build.api.BuildListener;
import org.bndtools.build.api.BuildListener.BuildState;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import bndtools.central.Central;

public class BuildListeners {
	private static final ILogger								logger	= Logger.getLogger(BuildListeners.class);
	private final BuildListener.BuildState						state	= BuildState.released;

	private final List<BuildListener>							listeners;
	private final ServiceTracker<BuildListener, BuildListener>	listenerTracker;
	private IProject											project;
	private IPath[]												paths;

	public BuildListeners() {
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
			.getConfigurationElementsFor(BndtoolsConstants.CORE_PLUGIN_ID, "buildListeners");
		listeners = new ArrayList<>(elements.length);

		for (IConfigurationElement elem : elements) {
			try {
				BuildListener listener = (BuildListener) elem.createExecutableExtension("class");
				listeners.add(listener);
			} catch (Exception e) {
				logger.logError("Unable to instantiate build listener: " + elem.getAttribute("name"), e);
			}
		}

		BundleContext context = FrameworkUtil.getBundle(BuildListeners.class)
			.getBundleContext();

		listenerTracker = new ServiceTracker<BuildListener, BuildListener>(context, BuildListener.class, null) {
			@Override
			public BuildListener addingService(ServiceReference<BuildListener> reference) {
				BuildListener listener = super.addingService(reference);
				synchronized (listeners) {
					switch (state) {
						case starting :
							listener.buildStarting(project);
							//$FALL-THROUGH$
						case built :
							listener.builtBundles(project, paths);
							//$FALL-THROUGH$
						case released :
						default :
							break;

					}
					listeners.add(listener);
				}
				return listener;
			}

			@Override
			public void removedService(ServiceReference<BuildListener> reference, BuildListener service) {
				listeners.remove(service);
				super.removedService(reference, service);
			}
		};
		listenerTracker.open();
	}

	public void fireBuildStarting(final IProject project) {
		this.project = project;
		forEachListener(listener -> listener.buildStarting(project));
	}

	public void fireBuiltBundles(final IProject project, final IPath[] paths) {
		this.project = project;
		this.paths = paths;
		forEachListener(listener -> listener.builtBundles(project, paths));
	}

	public void fireReleased(final IProject project) {
		forEachListener(listener -> listener.released(project));
	}

	private void forEachListener(Consumer<BuildListener> function) {
		synchronized (listeners) {
			for (BuildListener listener : listeners) {
				try {
					function.accept(listener);
				} catch (Exception e) {
					logger.logError("BuildListener error", e);
				}
			}
		}
	}

	/**
	 * Call this to make sure that any references to the listeners are no longer
	 * held.
	 */
	public void release(IProject project) {
		fireReleased(project);
		listeners.clear();
		listenerTracker.close();
	}

	public void updateListeners(File[] buildFiles, IProject project) throws Exception {
		// Notify the build listeners
		if (buildFiles.length > 0) {
			IPath[] paths = new IPath[buildFiles.length];
			for (int i = 0; i < buildFiles.length; i++)
				paths[i] = Central.toPath(buildFiles[i]);
			fireBuiltBundles(project, paths);
		}
	}

}
