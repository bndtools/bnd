package bndtools.jareditor.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Plugin extends AbstractUIPlugin {
	public static final String	PLUGIN_ID	= "bndtools.jareditor";

	private static Plugin		instance;

	public static Plugin getInstance() {
		return instance;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		instance = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);

		instance = null;
	}
}
