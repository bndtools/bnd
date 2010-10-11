package bndtools.wizards.workspace;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.utils.log.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.osgi.framework.BundleContext;

import bndtools.Plugin;

public class ImportBundleRepositoryWizard extends Wizard implements IImportWizard {

    private final RepositoryAdmin repoAdmin;

    private final OBRSelectionPage repoPage;
    private final RemoteRepositoryBundleSelectionPage bundlePage;
    private final DependentResourcesWizardPage dependenciesPage;

    private IWorkbench workbench;
    private IStructuredSelection selection;

    public ImportBundleRepositoryWizard() {
        setNeedsProgressMonitor(true);

        BundleContext bc = Plugin.getDefault().getBundleContext();
        repoAdmin = new RepositoryAdminImpl(bc, new Logger(bc));

        bundlePage = new RemoteRepositoryBundleSelectionPage(repoAdmin);
        repoPage = new OBRSelectionPage(repoAdmin, bundlePage);
        dependenciesPage = new DependentResourcesWizardPage(repoAdmin);

        bundlePage.addPropertyChangeListener(RemoteRepositoryBundleSelectionPage.PROP_SELECTION, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                dependenciesPage.setSelectedResources(bundlePage.getSelectedResources());
            }
        });

        addPage(repoPage);
        addPage(bundlePage);
        addPage(dependenciesPage);
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
    public boolean performFinish() {
        final MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Problems occurred while resolving or importing bundles.", null);

        Collection<Resource> adding = new ArrayList<Resource>();
        adding.addAll(dependenciesPage.getSelected());
        adding.addAll(dependenciesPage.getRequired());

        if(adding.isEmpty())
            return true;

        AddOBRResourcesToWorkspaceTask installTask = new AddOBRResourcesToWorkspaceTask(adding, status);

        try {
            getContainer().run(true, true, installTask);

            switch (status.getSeverity()) {
            case IStatus.CANCEL:
                return false;
            case IStatus.ERROR:
                ErrorDialog.openError(getShell(), "Error", null, status);
                return false;
            case IStatus.WARNING:
                if (ErrorDialog.openError(getShell(), "Error", null, status) == Window.OK) {
                    return true;
                } else {
                    return false;
                }
            default:
                return true;
            }
        } catch (InvocationTargetException e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error creating workspace configuration project.", e.getCause()));
        } catch (InterruptedException e) {
            // Can't happen?
        }
        return false;
    }

}