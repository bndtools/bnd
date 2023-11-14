package org.bndtools.builder;

import org.bndtools.builder.facade.BuilderFacade;
import org.bndtools.facade.ExtensionFacade;
import org.osgi.framework.BundleContext;

public class BuilderPlugin extends org.eclipse.core.runtime.Plugin {
	public static final String		PLUGIN_ID	= "bndtools.builder";

	static final Class<?>			BUILDER_FACADE		= BuilderFacade.class;
	static final Class<?>			EXTENSION_FACADE	= ExtensionFacade.class;

	private static BuilderPlugin instance = null;

	public static BuilderPlugin getInstance() {
		synchronized (BuilderPlugin.class) {
			return instance;
		}
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		synchronized (BuilderPlugin.class) {
			instance = this;
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		synchronized (BuilderPlugin.class) {
			instance = null;
		}
		super.stop(context);
	}

}
