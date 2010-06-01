package bndtools.repository.orbit;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    public static final String PLUGIN_ID = "bndtools.repository.orbit";

    public static volatile BundleContext context = null;

    public void start(BundleContext context) throws Exception {
        Activator.context = context;
    }

    public void stop(BundleContext context) throws Exception {
        Activator.context = null;
    }

}
