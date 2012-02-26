package bndtools.wizards.bndfile;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import bndtools.Plugin;
import bndtools.wizards.shared.AbstractTemplateSelectionWizardPage;

public class LaunchTemplateSelectionPage extends AbstractTemplateSelectionWizardPage {

    protected LaunchTemplateSelectionPage() {
        super("templateSelection");
        setTitle("Run Configuration Template");
    }

    @Override
    protected IConfigurationElement[] loadConfigurationElements() {
        return Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "launchTemplates");
    }

}
