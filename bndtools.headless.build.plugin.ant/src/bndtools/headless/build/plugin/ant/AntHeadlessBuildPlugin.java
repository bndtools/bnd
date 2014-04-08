package bndtools.headless.build.plugin.ant;

import java.io.File;
import java.io.IOException;

import org.bndtools.api.HeadlessBuildPlugin;
import org.bndtools.api.NamedPlugin;
import org.bndtools.utils.copy.bundleresource.BundleResourceCopier;
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

    public NamedPlugin getInformation() {
        return new AntHeadlessBuildPluginInformation();
    }

    public void setup(boolean cnf, File projectDir, boolean add) throws IOException {
        String baseDir;
        if (cnf) {
            baseDir = "templates/cnf/";
        } else {
            baseDir = "templates/project/";
        }
        copier.addOrRemoveDirectory(projectDir, baseDir, "/", add);
    }
}