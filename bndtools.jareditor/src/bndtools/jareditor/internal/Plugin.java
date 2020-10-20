package bndtools.jareditor.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Plugin extends AbstractUIPlugin {
	public static final String	IMG_CLOSE				= "CLOSE";
	public static final String	IMG_PREVIOUS			= "PREVIOUS";
	public static final String	IMG_PREVIOUS_DISABLED	= "PREVIOUS_DISABLED";
	public static final String	IMG_NEXT				= "NEXT";
	public static final String	IMG_NEXT_DISABLED		= "NEXT_DISABLED";

	public static final String	PLUGIN_ID				= "bndtools.jareditor";

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

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(IMG_CLOSE, ImageDescriptor.createFromURL(this.getBundle()
			.getEntry("/icons/close.png")));
		reg.put(IMG_NEXT, ImageDescriptor.createFromURL(this.getBundle()
			.getEntry("/icons/next.png")));
		reg.put(IMG_NEXT_DISABLED, ImageDescriptor.createFromURL(this.getBundle()
			.getEntry("/icons/next_disabled.png")));
		reg.put(IMG_PREVIOUS, ImageDescriptor.createFromURL(this.getBundle()
			.getEntry("/icons/previous.png")));
		reg.put(IMG_PREVIOUS_DISABLED, ImageDescriptor.createFromURL(this.getBundle()
			.getEntry("/icons/previous_disabled.png")));
	}
}
