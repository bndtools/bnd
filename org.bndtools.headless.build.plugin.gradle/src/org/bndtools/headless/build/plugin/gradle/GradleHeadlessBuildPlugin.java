package org.bndtools.headless.build.plugin.gradle;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.bndtools.api.NamedPlugin;
import org.bndtools.headless.build.manager.api.HeadlessBuildPlugin;
import org.bndtools.utils.copy.bundleresource.BundleResourceCopier;
import org.bndtools.utils.copy.bundleresource.CopyMode;
import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;

@Component
public class GradleHeadlessBuildPlugin implements HeadlessBuildPlugin {
    private final AtomicReference<VersionControlIgnoresManager> versionControlIgnoresManager = new AtomicReference<VersionControlIgnoresManager>();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setVersionControlIgnoresManager(VersionControlIgnoresManager versionControlIgnoresManager) {
        this.versionControlIgnoresManager.set(versionControlIgnoresManager);
    }

    public void unsetVersionControlIgnoresManager(VersionControlIgnoresManager versionControlIgnoresManager) {
        this.versionControlIgnoresManager.compareAndSet(versionControlIgnoresManager, null);
    }

    @Activate
    public void activate(BundleContext bundleContext) {
        copier = new BundleResourceCopier(bundleContext.getBundle());
    }

    @Deactivate
    public void deactivate() {
        copier = null;
    }

    private BundleResourceCopier copier = null;

    /*
     * HeadlessBuildPlugin
     */

    @Override
    public NamedPlugin getInformation() {
        return new GradleHeadlessBuildPluginInformation();
    }

    @Override
    @Deprecated
    public void setup(boolean cnf, File projectDir, boolean add, Set<String> enabledIgnorePlugins) throws IOException {
        setup(cnf, projectDir, add, enabledIgnorePlugins, new LinkedList<String>());
    }

    @Override
    public void setup(boolean cnf, File projectDir, boolean add, Set<String> enabledIgnorePlugins, List<String> warnings) throws IOException {
        if (!cnf) {
            return;
        }

        /* cnf */

        File workspaceRoot = projectDir.getParentFile();

        String baseDir = "templates/root/";
        copier.addOrRemoveDirectory(workspaceRoot, baseDir, "/", add ? CopyMode.ADD : CopyMode.REMOVE);

        Collection<File> files = copier.addOrRemoveDirectory(projectDir, baseDir, "/", add ? CopyMode.ADD : CopyMode.CHECK);
        for (File file : files) {
            String warning;
            if (add)
                warning = String.format("Not overwriting existing Gradle build file: %s", file);
            else
                warning = String.format("Gradle build file may need to be removed: %s", file);
            warnings.add(warning);
        }

        VersionControlIgnoresManager ignoresManager = versionControlIgnoresManager.get();
        if (ignoresManager != null) {
            List<String> ignoredEntries = new LinkedList<String>();
            ignoredEntries.add(ignoresManager.sanitiseGitIgnoreGlob(true, "/.gradle/", true));
            ignoredEntries.add(ignoresManager.sanitiseGitIgnoreGlob(true, "/reports/", true));
            ignoredEntries.add(ignoresManager.sanitiseGitIgnoreGlob(true, Workspace.getDefaults()
                .getProperty(Constants.DEFAULT_PROP_TARGET_DIR), true));

            ignoresManager.addIgnores(enabledIgnorePlugins, workspaceRoot, ignoredEntries);
        }
    }
}