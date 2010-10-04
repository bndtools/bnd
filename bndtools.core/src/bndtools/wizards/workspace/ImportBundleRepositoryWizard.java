package bndtools.wizards.workspace;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.utils.log.Logger;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.osgi.framework.BundleContext;

import aQute.bnd.service.RepositoryPlugin;
import bndtools.Plugin;
import bndtools.tasks.repo.LocalRepositoryTasks;

public class ImportBundleRepositoryWizard extends Wizard implements IImportWizard {

    private final RepositoryAdmin repoAdmin;
    private final RemoteRepositoryBundleSelectionPage bundlePage;

    private IWorkbench workbench;
    private IStructuredSelection selection;

    public ImportBundleRepositoryWizard() {
        BundleContext bc = Plugin.getDefault().getBundleContext();
        repoAdmin = new RepositoryAdminImpl(bc, new Logger(bc));
        try {
            repoAdmin.addRepository(new URL("file:/Users/neil/Projects/DistributedOSGi/bindex_test/repository.xml"));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        bundlePage = new RemoteRepositoryBundleSelectionPage("bundlePage", repoAdmin);

        setNeedsProgressMonitor(true);
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;
    }

    @Override
    public boolean needsProgressMonitor() {
        return true;
    }

    @Override
    public void addPages() {
        addPage(bundlePage);
    }

    @Override
    public boolean performFinish() {
        final MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Problems occurred importing bundle(s) to local repository.", null);

        Collection<Resource> selected = bundlePage.getSelectedResources();
        if(selected == null || selected.isEmpty())
            return false;

        final Collection<Resource> adding = new ArrayList<Resource>(selected.size());
        adding.addAll(selected);

        Collection<Resource> requiredResources = new LinkedList<Resource>();
        Collection<Resource> optionalResources = new LinkedList<Resource>();
        boolean resolved;
        try {
            Resolver resolver = repoAdmin.resolver();
            for (Resource resource : bundlePage.getSelectedResources()) {
                resolver.add(resource);
            }

            resolved = resolver.resolve(Resolver.NO_SYSTEM_BUNDLE | Resolver.NO_LOCAL_RESOURCES);
            Resource[] tmp;

            tmp = resolver.getRequiredResources();
            if (tmp != null)
                for (Resource resource : tmp) requiredResources.add(resource);

            tmp = resolver.getOptionalResources();
            if (tmp != null)
                for (Resource resource : tmp) optionalResources.add(resource);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!requiredResources.isEmpty() || !optionalResources.isEmpty()) {
            RequiredBundlesDialog requiredBundlesDialog = new RequiredBundlesDialog(getShell(), requiredResources, optionalResources);
            requiredBundlesDialog.setBlockOnOpen(true);
            if (requiredBundlesDialog.open() == Window.OK) {
                adding.addAll(requiredBundlesDialog.getAllSelected());
            }
        }

        try {
            getContainer().run(false, false, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
                            public void run(IProgressMonitor monitor) throws CoreException {
                                SubMonitor progress = SubMonitor.convert(monitor, "Copying files to repository", 4 + adding.size());

                                LocalRepositoryTasks.configureBndWorkspace(progress.newChild(1));
                                RepositoryPlugin localRepo = LocalRepositoryTasks.getLocalRepository();
                                LocalRepositoryTasks.installImplicitRepositoryContents(status, progress.newChild(2));

                                int workRemaining = adding.size() + 1;
                                for (Resource resource : adding) {
                                    progress.setWorkRemaining(workRemaining);
                                    try {
                                        URL url = new URL(resource.getURI());
                                        LocalRepositoryTasks.installBundle(localRepo, url);
                                        progress.worked(1);
                                    } catch (IOException e) {
                                        status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error copying resource {0}.", resource.getId()), e));
                                    } catch (CoreException e) {
                                        status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error copying resource {0}.", resource.getId()), e));
                                    }
                                    workRemaining --;
                                }
                                LocalRepositoryTasks.refreshWorkspaceForRepository(progress.newChild(1));
                            }
                        }, monitor);
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });

            if(!status.isOK())
                ErrorDialog.openError(getShell(), "Warning", null, status);
            return true;
        } catch (InvocationTargetException e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error creating workspace configuration project.", e.getCause()));
        } catch (InterruptedException e) {
            // Can't happen?
        }
        return false;
    }
}