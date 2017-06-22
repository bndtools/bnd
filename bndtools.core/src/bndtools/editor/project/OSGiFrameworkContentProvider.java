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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import bndtools.Plugin;
import bndtools.model.repo.LoadingContentElement;

public class OSGiFrameworkContentProvider implements IStructuredContentProvider {
    private static final ILogger logger = Logger.getLogger(OSGiFrameworkContentProvider.class);

    List<OSGiFramework> frameworks = new ArrayList<OSGiFramework>();
    private final Deferred<List<OSGiFramework>> contentReadyQueue = new Deferred<>();

    private StructuredViewer structuredViewer;
    private Workspace workspace;
    private LoadingContentJob loadingJob;

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        frameworks.clear();
        loadingJob = null;
        workspace = (Workspace) newInput;
        structuredViewer = (StructuredViewer) viewer;
    }

    private IStatus refreshProviders() {
        List<IStatus> statuses = new ArrayList<>();

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

            List<RepositoryPlugin> repositories;
            try {
                repositories = (workspace != null) ? workspace.getRepositories() : Collections.<RepositoryPlugin> emptyList();
            } catch (Exception e) {
                return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, e.getMessage(), e);
            }

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
                                String msg = String.format("Error finding repository entry for OSGi framework %s, version %s.", bsn, version.toString());
                                logger.logError(msg, e);
                                statuses.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, msg, e));
                            }
                        }
                } catch (Exception e) {
                    String msg = String.format("Error searching repository for OSGi framework %s.", bsn);
                    logger.logError(msg, e);
                    statuses.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, msg, e));
                }
            }
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                structuredViewer.refresh(true);
            }
        });

        if (statuses.size() > 0) {
            return new MultiStatus(Plugin.PLUGIN_ID, IStatus.ERROR, statuses.toArray(new IStatus[0]), "Errors while refreshing OSGi framework providers.", null);
        }

        return Status.OK_STATUS;
    }

    @Override
    public void dispose() {}

    @Override
    public Object[] getElements(Object inputElement) {
        if (frameworks.size() == 0) {
            if (loadingJob == null) {
                loadingJob = new LoadingContentJob("Refreshing OSGi Framework content...") {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        IStatus status = refreshProviders();

                        contentReadyQueue.resolve(frameworks);

                        return status;
                    }
                };
                loadingJob.schedule();
            }

            return loadingJob.getLoadingContent();
        }

        return frameworks.toArray();
    }

    public void onContentReady(final Success<List<OSGiFramework>,Void> callback) {
        Promise<List<OSGiFramework>> p = contentReadyQueue.getPromise();
        p.then(callback, null).then(null, callbackFailure);
    }

    private static final Failure callbackFailure = new Failure() {
        @Override
        public void fail(Promise< ? > resolved) throws Exception {
            logger.logError("onContentReady callback failed", resolved.getFailure());
        }
    };

    private static abstract class LoadingContentJob extends Job {

        public LoadingContentJob(String name) {
            super(name);
        }

        public Object[] getLoadingContent() {
            return loadingContent;
        }

        private final Object[] loadingContent = new Object[] {
                new LoadingContentElement()
        };

    }
}
