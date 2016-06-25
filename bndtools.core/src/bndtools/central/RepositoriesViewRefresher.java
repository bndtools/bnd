package bndtools.central;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.function.Function;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.Plugin;

public class RepositoriesViewRefresher implements RepositoryListenerPlugin {

    public interface RefreshModel {
        List<RepositoryPlugin> getRepositories();
    }

    // private static final ILogger logger = Logger.getLogger(RepositoriesViewRefresher.class);
    private boolean redo = false;
    private boolean busy = false;
    private final ServiceRegistration<RepositoryListenerPlugin> registration;
    private final Map<TreeViewer,RefreshModel> viewers = new ConcurrentHashMap<>();

    RepositoriesViewRefresher() {
        ServiceRegistration<RepositoryListenerPlugin> reg = null;
        Bundle bundle = FrameworkUtil.getBundle(RepositoriesViewRefresher.class);
        if (bundle != null) {
            BundleContext context = bundle.getBundleContext();
            if (context != null)
                reg = context.registerService(RepositoryListenerPlugin.class, this, null);
        }
        this.registration = reg;
    }

    public void refreshRepositories(final RepositoryPlugin target) {

        synchronized (this) {
            if (busy) {
                redo = true;
                return;
            }

            busy = true;
            redo = false;
        }

        //
        // Since this can delay, we move this to the background
        //

        new WorkspaceJob("Updating repositories content") {

            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                try {
                    if (monitor == null)
                        monitor = new NullProgressMonitor();

                    Set<RepositoryPlugin> repos = new HashSet<>();
                    if (target != null)
                        repos.add(target);
                    else {
                        for (RefreshModel m : viewers.values()) {
                            repos.addAll(m.getRepositories());
                        }
                    }

                    ensureLoaded(monitor, repos);

                    // get repositories first, then do UI thread work

                    final Map<Entry<TreeViewer,RefreshModel>,List<RepositoryPlugin>> entryRepos = new HashMap<>();

                    for (Map.Entry<TreeViewer,RefreshModel> entry : viewers.entrySet()) {
                        entryRepos.put(entry, entry.getValue().getRepositories());
                    }

                    //
                    // And now back to the UI thread
                    //

                    getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {

                            synchronized (RepositoriesViewRefresher.this) {
                                redo = false;
                            }

                            for (Map.Entry<TreeViewer,RefreshModel> entry : viewers.entrySet()) {

                                TreePath[] expandedTreePaths = entry.getKey().getExpandedTreePaths();

                                entry.getKey().setInput(entryRepos.get(entry));
                                if (expandedTreePaths != null && expandedTreePaths.length > 0)
                                    entry.getKey().setExpandedTreePaths(expandedTreePaths);
                            }
                            synchronized (RepositoriesViewRefresher.this) {
                                busy = false;
                                if (redo) {
                                    refreshRepositories(null);
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return Status.OK_STATUS;
            }

        }.schedule(1000);
    }

    private IStatus ensureLoaded(IProgressMonitor monitor, Collection<RepositoryPlugin> repos) {
        int n = 0;
        try {
            final RepositoryPlugin workspaceRepo = Central.getWorkspaceRepository();
            for (RepositoryPlugin repo : repos) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                monitor.beginTask(repo.getName(), n++);
                if (repo != workspaceRepo) {
                    repo.list(null); // looks silly but is here to incur any download time
                    continue;
                }
                // We must safely call bnd to list workspace repo
                try {
                    Central.bndCall(new Callable<List<String>>() {
                        @Override
                        public List<String> call() throws Exception {
                            return workspaceRepo.list(null);
                        }
                    }, monitor);
                } catch (TimeoutException | InterruptedException e) {
                    return new Status(Status.ERROR, Plugin.PLUGIN_ID, "Unable to acquire lock to refresh repository " + repo.getName(), e);
                }
            }
        } catch (Exception e) {
            return new Status(Status.ERROR, Plugin.PLUGIN_ID, "Exception refreshing repositories", e);
        }
        return Status.OK_STATUS;
    }

    public void addViewer(final TreeViewer viewer, final RefreshModel model) {
        this.viewers.put(viewer, model);
        Central.onWorkspaceInit(new Function<Workspace,Void>() {
            @Override
            public Void apply(Workspace a) {
                new Job("Updating repositories") {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        final List<RepositoryPlugin> repositories = model.getRepositories();
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                viewer.setInput(repositories);
                            }
                        });
                        return Status.OK_STATUS;
                    }
                }.schedule();
                return null;
            }
        });
    }

    public void removeViewer(TreeViewer viewer) {
        this.viewers.remove(viewer);
    }

    @Override
    public void bundleAdded(final RepositoryPlugin repository, Jar jar, File file) {
        refreshRepositories(repository);
    }

    @Override
    public void bundleRemoved(final RepositoryPlugin repository, Jar jar, File file) {
        refreshRepositories(repository);
    }

    @Override
    public void repositoryRefreshed(final RepositoryPlugin repository) {
        refreshRepositories(repository);
    }

    @Override
    public void repositoriesRefreshed() {
        refreshRepositories(null);
    }

    public void close() {
        if (registration != null)
            registration.unregister();
    }

    public static Display getDisplay() {
        Display display = Display.getCurrent();
        //may be null if outside the UI thread
        if (display == null)
            display = Display.getDefault();
        return display;
    }

    public void setRepositories(final TreeViewer viewer, final RefreshModel refresh) {
        synchronized (this) {
            if (busy) {
                redo = true;
                return;
            }
            busy = true;
        }
        new WorkspaceJob("Setting repositories") {

            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {

                ensureLoaded(monitor, refresh.getRepositories());

                SWTConcurrencyUtil.execForControl(viewer.getControl(), true, new Runnable() {

                    @Override
                    public void run() {
                        viewer.setInput(refresh.getRepositories());

                        synchronized (RepositoriesViewRefresher.this) {
                            busy = false;
                            if (redo)
                                refreshRepositories(null);
                        }
                    }

                });
                return Status.OK_STATUS;
            }
        }.schedule();
    }
}
