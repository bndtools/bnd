package org.bndtools.remoteinstall.wizard;

import static org.bndtools.remoteinstall.nls.Messages.InstallBundleWizardPage_Name;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleWizard_Error_NoConfigSelected;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleWizard_Title;

import org.bndtools.remoteinstall.dto.RemoteRuntimeConfiguration;
import org.bndtools.remoteinstall.store.RemoteRuntimeConfigurationStore;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = InstallBundleWizard.class)
public final class InstallBundleWizard extends Wizard {

    private RemoteRuntimeConfiguration configuration;

    @Reference
    private RemoteRuntimeConfigurationStore store;

    @Reference
    private InstallBundleWizardPage installBundleWizardPage;

    public InstallBundleWizard() {
        setWindowTitle(InstallBundleWizard_Title);
    }

    public RemoteRuntimeConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean performFinish() {
        final RemoteRuntimeConfiguration selectedConfiguration = installBundleWizardPage.getSelectedConfiguration();
        if (selectedConfiguration != null) {
            configuration = selectedConfiguration;
            return true;
        }
        installBundleWizardPage.setErrorMessage(InstallBundleWizard_Error_NoConfigSelected);
        return false;
    }

    @Override
    public void addPages() {
        final IWizardPage wizardPage = getPage(InstallBundleWizardPage_Name);
        if (wizardPage == null) {
            addPage(installBundleWizardPage);
        }
    }

}
