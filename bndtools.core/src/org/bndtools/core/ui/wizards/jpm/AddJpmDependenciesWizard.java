package org.bndtools.core.ui.wizards.jpm;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

import bndtools.Plugin;
import aQute.bnd.service.repository.SearchableRepository;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;

public class AddJpmDependenciesWizard extends Wizard {

    @SuppressWarnings("unused")
    private final URI uri;
    private final JpmDependencyWizardPage depsPage;
    private final Set<ResourceDescriptor> result = new HashSet<ResourceDescriptor>();

    public AddJpmDependenciesWizard(URI uri) {
        this.uri = uri;
        this.depsPage = new JpmDependencyWizardPage(uri);

        setNeedsProgressMonitor(true);
        addPage(depsPage);
    }

    @Override
    public boolean performFinish() {
        result.clear();

        result.addAll(depsPage.getDirectResources());
        result.addAll(depsPage.getSelectedIndirectResources());

        final Set<ResourceDescriptor> indirectResources;
        if (depsPage.getIndirectResources() != null) {
            indirectResources = new HashSet<SearchableRepository.ResourceDescriptor>(depsPage.getIndirectResources());
            indirectResources.removeAll(result);
        } else {
            indirectResources = Collections.<ResourceDescriptor> emptySet();
        }
        final MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Errors occurred while processing JPM4J dependencies.", null);
        IRunnableWithProgress runnable = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                SubMonitor progress = SubMonitor.convert(monitor, result.size() + indirectResources.size());
                progress.setTaskName("Processing dependencies...");

                // Process all resources (including non-selected ones) into the repository
                for (ResourceDescriptor resource : result)
                    processResource(resource, status, progress.newChild(1));
                for (ResourceDescriptor resource : indirectResources)
                    processResource(resource, status, progress.newChild(1));
            }
        };

        try {
            getContainer().run(true, true, runnable);

            if (!status.isOK())
                ErrorDialog.openError(getShell(), "Errors", null, status);

            return true;
        } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), "Error", e.getCause().getMessage());
            return false;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public Set<ResourceDescriptor> getResult() {
        return Collections.unmodifiableSet(result);
    }

    @SuppressWarnings("unused")
    private void processResource(ResourceDescriptor resource, MultiStatus status, IProgressMonitor monitor) {
        SearchableRepository repo = depsPage.getRepository();
        try {
            if (!resource.included)
                repo.addResource(resource);
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error adding resource to local JPM4J index: " + resource.bsn + " [" + resource.version + "]", e));
        }
    }

}
