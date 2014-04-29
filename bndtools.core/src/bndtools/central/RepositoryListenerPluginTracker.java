package bndtools.central;

import java.io.File;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;

public class RepositoryListenerPluginTracker extends ServiceTracker<RepositoryListenerPlugin,RepositoryListenerPlugin> implements RepositoryListenerPlugin {

    public RepositoryListenerPluginTracker(BundleContext context) {
        super(context, RepositoryListenerPlugin.class, null);
    }

    public void bundleAdded(RepositoryPlugin repository, Jar jar, File file) {
        Object[] snapshot = getServices();
        if (snapshot != null)
            for (Object l : snapshot) {
                ((RepositoryListenerPlugin) l).bundleAdded(repository, jar, file);
            }
    }

    public void bundleRemoved(RepositoryPlugin repository, Jar jar, File file) {
        Object[] snapshot = getServices();
        if (snapshot != null)
            for (Object l : snapshot) {
                ((RepositoryListenerPlugin) l).bundleRemoved(repository, jar, file);
            }
    }

    public void repositoryRefreshed(RepositoryPlugin repository) {
        Object[] snapshot = getServices();
        if (snapshot != null)
            for (Object l : snapshot) {
                ((RepositoryListenerPlugin) l).repositoryRefreshed(repository);
            }
    }

    public void repositoriesRefreshed() {
        Object[] snapshot = getServices();
        if (snapshot != null)
            for (Object l : snapshot) {
                ((RepositoryListenerPlugin) l).repositoriesRefreshed();
            }
    }

}
