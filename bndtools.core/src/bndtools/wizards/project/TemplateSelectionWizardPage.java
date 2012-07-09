package bndtools.wizards.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import bndtools.Logger;
import bndtools.Plugin;
import bndtools.api.ILogger;
import bndtools.api.IProjectTemplate;
import bndtools.wizards.shared.AbstractTemplateSelectionWizardPage;

public class TemplateSelectionWizardPage extends AbstractTemplateSelectionWizardPage {
    private static final ILogger logger = Logger.getLogger();

    public static final String PROP_TEMPLATE = "template";

    private IProjectTemplate template;

    public TemplateSelectionWizardPage() {
        super("wizardPage");
        setTitle("Project Templates");
        setDescription("");

        propSupport.addPropertyChangeListener(PROP_ELEMENT, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                IConfigurationElement configElem = (IConfigurationElement) evt.getNewValue();
                IProjectTemplate oldTemplate = template;
                if (configElem != null) {
                    String error = null;
                    try {
                        template = (IProjectTemplate) configElem.createExecutableExtension("class");
                    } catch (CoreException e) {
                        error = e.getMessage();
                        logger.logError("Error loading project template", e);
                    }
                    propSupport.firePropertyChange(PROP_TEMPLATE, oldTemplate, template);
                    setErrorMessage(error);
                }
            }
        });
    }

    public IProjectTemplate getTemplate() {
        return template;
    }

    @Override
    protected IConfigurationElement[] loadConfigurationElements() {
        return Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "projectTemplates");
    }

}
