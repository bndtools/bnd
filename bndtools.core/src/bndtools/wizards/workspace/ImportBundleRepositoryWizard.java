package bndtools.wizards.workspace;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;

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
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import aQute.bnd.service.RepositoryPlugin;
import bndtools.Plugin;
import bndtools.tasks.repo.LocalRepositoryTasks;

public class ImportBundleRepositoryWizard extends Wizard implements IImportWizard {

    private final RepositorySelectionPage repoPage = new RepositorySelectionPage("repoPage");
    private final RemoteRepositoryBundleSelectionPage bundlePage = new RemoteRepositoryBundleSelectionPage("bundlePage");

    private IWorkbench workbench;
    private IStructuredSelection selection;

    public ImportBundleRepositoryWizard() {
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

    void setDescription(String description) {
        repoPage.setDescription(description);
    }

    @Override
    public void addPages() {
        repoPage.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                bundlePage.setRepository(repoPage.getSelectedRepository());
            }
        });
        addPage(repoPage);
        addPage(bundlePage);
    }

    @Override
    public boolean performFinish() {
        final MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Problems occurred importing bundle(s) to local repository.", null);
        final Collection<URL> urls = bundlePage.getSelectedURLs();
        try {
            getContainer().run(false, false, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
                            public void run(IProgressMonitor monitor) throws CoreException {
                                SubMonitor progress = SubMonitor.convert(monitor, "Copying files to repository", 4 + urls.size());

                                LocalRepositoryTasks.configureBndWorkspace(progress.newChild(1));
                                RepositoryPlugin localRepo = LocalRepositoryTasks.getLocalRepository();
                                LocalRepositoryTasks.installImplicitRepositoryContents(localRepo, status, progress.newChild(2));

                                int workRemaining = urls.size() + 1;
                                for (URL url : urls) {
                                    progress.setWorkRemaining(workRemaining);
                                    try {
                                        LocalRepositoryTasks.installBundle(localRepo, url);
                                        progress.worked(1);
                                    } catch (IOException e) {
                                        status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error copying bundle URL {0}.", url), e));
                                    } catch (CoreException e) {
                                        status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error copying bundle URL {0}.", url), e));
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