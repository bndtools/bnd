package bndtools.ace.launch;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Plugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "bndtools.ace";

    private static Plugin instance;

    @Override
    public void start(BundleContext context) throws Exception {
        instance = this;
        super.start(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        instance = null;
    }

    public static Plugin getDefault() {
        return instance;
    }
}
