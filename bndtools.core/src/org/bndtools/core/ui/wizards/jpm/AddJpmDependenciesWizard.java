package org.bndtools.core.ui.wizards.jpm;

import java.net.URI;
import java.util.Set;

import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.service.repository.SearchableRepository;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import bndtools.Central;

public class AddJpmDependenciesWizard extends Wizard {

    private final URI uri;
    private final JpmDependencyWizardPage depsPage;

    public AddJpmDependenciesWizard(URI uri) {
        this.uri = uri;
        this.depsPage = new JpmDependencyWizardPage(uri);

        setNeedsProgressMonitor(true);
        addPage(depsPage);
    }

    public boolean init() {
        try {
            SearchableRepository repo = Central.getWorkspace().getPlugin(SearchableRepository.class);
            Set<ResourceDescriptor> resources = repo.getResources(uri, false);

            for (ResourceDescriptor resource : resources) {
                System.out.println(resource);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean performFinish() {
        // TODO Auto-generated method stub
        return false;
    }

}
