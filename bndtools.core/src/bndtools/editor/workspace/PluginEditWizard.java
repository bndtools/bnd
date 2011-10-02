package bndtools.editor.workspace;

import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.Wizard;

public class PluginEditWizard extends Wizard {

    private final PluginPropertiesPage propertiesPage = new PluginPropertiesPage();

    public PluginEditWizard(IConfigurationElement configElement, Map<String, String> properties) {
        propertiesPage.setConfigElement(configElement);
        propertiesPage.setProperties(properties);

        addPage(propertiesPage);
    }

    @Override
    public boolean performFinish() {
        return true;
    }

}
