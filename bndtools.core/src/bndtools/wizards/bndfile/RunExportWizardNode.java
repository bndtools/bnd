package bndtools.wizards.bndfile;

import java.util.concurrent.atomic.AtomicReference;

import org.bndtools.core.ui.IRunDescriptionExportWizard;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.api.IBndModel;

public class RunExportWizardNode implements IWizardNode {

    private final Shell shell;
    private final IConfigurationElement config;
    private final IBndModel model;
    private final Project bndProject;
    
    private final AtomicReference<IRunDescriptionExportWizard> wizardRef = new AtomicReference<IRunDescriptionExportWizard>(null);


    public RunExportWizardNode(Shell shell, IConfigurationElement config, IBndModel model, Project bndProject) {
        this.shell = shell;
        this.config = config;
        this.model = model;
        this.bndProject = bndProject;
    }

    public Point getExtent() {
        return new Point(-1, -1);
    }

    public IWizard getWizard() {
        IRunDescriptionExportWizard wizard = wizardRef.get();
        if (wizard != null)
            return wizard;

        try {
            wizard = (IRunDescriptionExportWizard) config.createExecutableExtension("class");
            wizard.setBndModel(model, bndProject);
            
            if (!wizardRef.compareAndSet(null, wizard))
                wizard = wizardRef.get();

            return wizard;
        } catch (Exception e) {
            ErrorDialog.openError(shell, "Error", null,  new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to create selected export wizard", e));
            return null;
        }
    }

    public boolean isContentCreated() {
        return wizardRef.get() != null;
    }

    public void dispose() {
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((config == null) ? 0 : config.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RunExportWizardNode other = (RunExportWizardNode) obj;
        if (config == null) {
            if (other.config != null)
                return false;
        } else if (!config.equals(other.config))
            return false;
        return true;
    }

}
