package bndtools;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.bndtools.api.NamedPlugin;
import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class VersionControlIgnoresManagerTracker extends ServiceTracker<VersionControlIgnoresManager,VersionControlIgnoresManager> implements VersionControlIgnoresManager {
    private final AtomicReference<ServiceReference<VersionControlIgnoresManager>> managerReference = new AtomicReference<ServiceReference<VersionControlIgnoresManager>>();
    private final AtomicReference<VersionControlIgnoresManager> manager = new AtomicReference<VersionControlIgnoresManager>();

    public VersionControlIgnoresManagerTracker(BundleContext context) {
        super(context, VersionControlIgnoresManager.class, null);
    }

    /*
     * ServiceTracker
     */

    @Override
    public VersionControlIgnoresManager addingService(ServiceReference<VersionControlIgnoresManager> reference) {
        VersionControlIgnoresManager manager = super.addingService(reference);
        this.managerReference.set(reference);
        this.manager.set(manager);
        return manager;
    }

    @Override
    public void remove(ServiceReference<VersionControlIgnoresManager> reference) {
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
     * VersionControlIgnoresManager
     */

    @Override
    public String sanitiseGitIgnoreGlob(boolean rooted, String ignoreGlob, boolean directory) {
        VersionControlIgnoresManager manager = this.manager.get();
        if (manager == null) {
            return ignoreGlob;
        }

        return manager.sanitiseGitIgnoreGlob(rooted, ignoreGlob, directory);
    }

    @Override
    public void addIgnores(Set<String> plugins, File dstDir, String ignores) {
        VersionControlIgnoresManager manager = this.manager.get();
        if (manager == null) {
            return;
        }

        manager.addIgnores(plugins, dstDir, ignores);
    }

    @Override
    public void addIgnores(Set<String> plugins, File dstDir, List<String> ignores) {
        VersionControlIgnoresManager manager = this.manager.get();
        if (manager == null) {
            return;
        }

        manager.addIgnores(plugins, dstDir, ignores);
    }

    @Override
    public Set<String> getPluginsForProjectRepositoryProviderId(String repositoryProviderId) {
        VersionControlIgnoresManager manager = this.manager.get();
        if (manager == null) {
            return null;
        }

        return manager.getPluginsForProjectRepositoryProviderId(repositoryProviderId);
    }

    @Override
    public Collection<NamedPlugin> getAllPluginsInformation() {
        VersionControlIgnoresManager manager = this.manager.get();
        if (manager == null) {
            return Collections.emptySet();
        }

        return manager.getAllPluginsInformation();
    }

    @Override
    public void createProjectIgnores(Set<String> plugins, File projectDir, Map<String,String> sourceOutputLocations, String targetDir) {
        VersionControlIgnoresManager manager = this.manager.get();
        if (manager == null) {
            return;
        }

        manager.createProjectIgnores(plugins, projectDir, sourceOutputLocations, targetDir);
    }
}