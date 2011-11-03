package bndtools.wizards.workspace;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import bndtools.Plugin;
import bndtools.wizards.shared.AbstractTemplateSelectionWizardPage;

public class CnfTemplateSelectionWizardPage extends AbstractTemplateSelectionWizardPage {

    protected CnfTemplateSelectionWizardPage() {
        super("templateSelection");
        setTitle("Configuration Template");
        setImageDescriptor(Plugin.imageDescriptorFromPlugin("icons/bndtools-wizban.png")); //$NON-NLS-1$
    }

    @Override
    protected IConfigurationElement[] loadConfigurationElements() {
        return Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "cnfTemplates");
    }

}
