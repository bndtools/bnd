package bndtools.editor.project;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.utils.osgi.BundleUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.osgi.framework.Bundle;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import bndtools.Plugin;

public class OSGiFrameworkContentProvider implements IStructuredContentProvider {
    private static final ILogger logger = Logger.getLogger(OSGiFrameworkContentProvider.class);

    List<OSGiFramework> frameworks = new ArrayList<OSGiFramework>();

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        frameworks.clear();

        Workspace workspace = (Workspace) newInput;
        IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "osgiFrameworks");

        for (IConfigurationElement element : configElements) {
            String frameworkName = element.getAttribute("name");
            String bsn = element.getAttribute("bsn");

            URL iconUrl = null;
            String iconPath = element.getAttribute("icon");
            if (iconPath != null) {
                Bundle contributorBundle = BundleUtils.findBundle(Plugin.getDefault().getBundleContext(), element.getContributor().getName(), null);
                if (contributorBundle != null)
                    iconUrl = contributorBundle.getEntry(iconPath);
            }

            List<RepositoryPlugin> repositories = (workspace != null) ? workspace.getRepositories() : Collections.<RepositoryPlugin> emptyList();
            for (RepositoryPlugin repo : repositories) {
                try {
                    SortedSet<Version> versions = repo.versions(bsn);
                    if (versions != null)
                        for (Version version : versions) {
                            try {
                                File framework = repo.get(bsn, version, null);
                                if (framework != null)
                                    frameworks.add(new OSGiFramework(frameworkName, bsn, version, iconUrl));
                            } catch (Exception e) {
                                logger.logError(String.format("Error finding repository entry for OSGi framework %s, version %s.", bsn, version.toString()), e);
                            }
                        }
                } catch (Exception e) {
                    logger.logError(String.format("Error searching repository for OSGi framework %s.", bsn), e);
                }
            }
        }
    }

    public void dispose() {}

    public Object[] getElements(Object inputElement) {
        return frameworks.toArray();
    }

}
