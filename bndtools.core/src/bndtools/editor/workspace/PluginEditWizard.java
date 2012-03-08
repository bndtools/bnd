package bndtools.editor.workspace;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.Wizard;

import aQute.libg.header.Attrs;

public class PluginEditWizard extends Wizard {

    private final PluginPropertiesPage propertiesPage = new PluginPropertiesPage();

    public PluginEditWizard(IConfigurationElement configElement, Attrs properties) {
        propertiesPage.setConfigElement(configElement);
        propertiesPage.setProperties(properties);

        addPage(propertiesPage);
    }

    @Override
    public boolean performFinish() {
        return true;
    }

    public boolean isChanged() {
        return propertiesPage.isChanged();
    }

}
