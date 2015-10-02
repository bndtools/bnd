package bndtools;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.bndtools.api.NamedPlugin;
import org.bndtools.headless.build.manager.api.HeadlessBuildManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class HeadlessBuildManagerTracker extends ServiceTracker<HeadlessBuildManager,HeadlessBuildManager>implements HeadlessBuildManager {
    private final AtomicReference<ServiceReference<HeadlessBuildManager>> managerReference = new AtomicReference<ServiceReference<HeadlessBuildManager>>();
    private final AtomicReference<HeadlessBuildManager> manager = new AtomicReference<HeadlessBuildManager>();

    public HeadlessBuildManagerTracker(BundleContext context) {
        super(context, HeadlessBuildManager.class, null);
    }

    /*
     * ServiceTracker
     */

    @Override
    public HeadlessBuildManager addingService(ServiceReference<HeadlessBuildManager> reference) {
        HeadlessBuildManager manager = super.addingService(reference);
        this.managerReference.set(reference);
        this.manager.set(manager);
        return manager;
    }

    @Override
    public void remove(ServiceReference<HeadlessBuildManager> reference) {
        if (managerReference.compareAndSet(reference, null)) {
            manager.set(null);
        }

        super.remove(reference);
    }

    @Override
    public void close() {
        manager.set(null);
        managerReference.set(null);
        super.close();
    }

    /*
     * HeadlessBuildManager
     */

    @Override
    public Collection<NamedPlugin> getAllPluginsInformation() {
        HeadlessBuildManager manager = this.manager.get();
        if (manager == null) {
            return Collections.emptySet();
        }

        return manager.getAllPluginsInformation();
    }

    @Override
    public void setup(Set<String> plugins, boolean cnf, File projectDir, boolean add, Set<String> enabledIgnorePlugins, List<String> warnings) {
        HeadlessBuildManager manager = this.manager.get();
        if (manager == null) {
            return;
        }
        manager.setup(plugins, cnf, projectDir, add, enabledIgnorePlugins, warnings);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setup(Set<String> plugins, boolean cnf, File projectDir, boolean add, Set<String> enabledIgnorePlugins) {
        HeadlessBuildManager manager = this.manager.get();
        if (manager == null) {
            return;
        }

        manager.setup(plugins, cnf, projectDir, add, enabledIgnorePlugins);
    }
}