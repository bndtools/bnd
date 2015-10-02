package org.bndtools.headless.build.plugin.ant;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bndtools.api.NamedPlugin;
import org.bndtools.headless.build.manager.api.HeadlessBuildPlugin;
import org.bndtools.utils.copy.bundleresource.BundleResourceCopier;
import org.bndtools.utils.copy.bundleresource.CopyMode;
import org.osgi.framework.BundleContext;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;

@Component
public class AntHeadlessBuildPlugin implements HeadlessBuildPlugin {
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

    @Override
    public NamedPlugin getInformation() {
        return new AntHeadlessBuildPluginInformation();
    }

    @Override
    public void setup(boolean cnf, File projectDir, boolean add, Set<String> enabledIgnorePlugins) throws IOException {
        setup(cnf, projectDir, add, enabledIgnorePlugins, new LinkedList<String>());
    }

    @Override
    public void setup(boolean cnf, File projectDir, boolean add, Set<String> enabledIgnorePlugins, List<String> warnings) throws IOException {
        String baseDir = cnf ? "templates/cnf/" : "templates/project/";

        Collection<File> files = copier.addOrRemoveDirectory(projectDir, baseDir, "/", add ? CopyMode.ADD : CopyMode.CHECK);
        for (File file : files) {
            String warning;
            if (add)
                warning = String.format("Not overwriting existing Ant build file: %s", file);
            else
                warning = String.format("Ant build file may need to be removed: %s", file);
            warnings.add(warning);
        }
    }
}