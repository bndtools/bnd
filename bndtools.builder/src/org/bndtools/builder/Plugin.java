package org.bndtools.builder;

import org.osgi.framework.BundleContext;

public class Plugin extends org.eclipse.core.runtime.Plugin {

	private static Plugin instance = null;

	public static Plugin getInstance() {
		synchronized (Plugin.class) {
			return instance;
		}
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		synchronized (Plugin.class) {
			instance = this;
		}
	}

	public void stop(BundleContext context) throws Exception {
		synchronized (Plugin.class) {
			instance = null;
		}
		super.stop(context);
	}

}
