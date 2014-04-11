package bndtools.headless.build.plugin.gradle;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bndtools.api.HeadlessBuildPlugin;
import org.bndtools.api.NamedPlugin;
import org.bndtools.api.VersionControlIgnoresManager;
import org.bndtools.utils.copy.bundleresource.BundleResourceCopier;
import org.osgi.framework.BundleContext;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;

@Component
public class GradleHeadlessBuildPlugin implements HeadlessBuildPlugin {
    private BundleResourceCopier copier = null;

    @Activate
    public void activate(BundleContext bundleContext) {
        copier = new BundleResourceCopier(bundleContext.getBundle());
    }

    @Deactivate
    public void deactivate() {
        copier = null;
    }

    /*
     * HeadlessBuildPlugin
     */

    public NamedPlugin getInformation() {
        return new GradleHeadlessBuildPluginInformation();
    }

    public void setup(boolean cnf, File projectDir, boolean add, VersionControlIgnoresManager ignoresManager, Set<String> enabledIgnorePlugins) throws IOException {
        if (!cnf) {
            return;
        }

        /* cnf */

        File workspaceRoot = projectDir.getParentFile();

        String baseDir = "templates/root/";
        copier.addOrRemoveDirectory(workspaceRoot, baseDir, "/", add);

        baseDir = "templates/cnf/";
        copier.addOrRemoveDirectory(projectDir, baseDir, "/", add);

        List<String> ignoredEntries = new LinkedList<String>();
        ignoredEntries.add(ignoresManager.sanitiseGitIgnoreGlob(true, "/.gradle/", true));
        ignoredEntries.add(ignoresManager.sanitiseGitIgnoreGlob(true, "/reports/", true));
        ignoredEntries.add(ignoresManager.sanitiseGitIgnoreGlob(true, Workspace.getDefaults().getProperty(Constants.DEFAULT_PROP_TARGET_DIR), true));

        ignoresManager.addIgnores(enabledIgnorePlugins, workspaceRoot, ignoredEntries);
    }
}