package bndtools;

import java.io.File;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Jar;

public class RepositoryListenerPluginTracker extends ServiceTracker implements RepositoryListenerPlugin {

    public RepositoryListenerPluginTracker(BundleContext context) {
        super(context, RepositoryListenerPlugin.class.getName(), null);
    }

    public void bundleAdded(RepositoryPlugin repository, Jar jar, File file) {
        Object[] snapshot = getServices();
        if (snapshot != null)
            for (Object l : snapshot) {
                ((RepositoryListenerPlugin) l).bundleAdded(repository, jar, file);
            }
    }

}
