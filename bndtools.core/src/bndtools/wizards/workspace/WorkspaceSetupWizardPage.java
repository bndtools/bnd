package bndtools.wizards.workspace;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import aQute.bnd.build.Project;
import bndtools.Plugin;

public class WorkspaceSetupWizardPage extends WizardPage {

    private final WorkspaceLocationPart locationPart = new WorkspaceLocationPart();

    public WorkspaceSetupWizardPage() {
        super("Workspace Setup");
    }

    @Override
    public void createControl(Composite parent) {
        setTitle("Setup Bnd/OSGi Workspace");
        setDescription("Create a workspace folder with initial configuration");
        setImageDescriptor(Plugin.imageDescriptorFromPlugin("icons/bndtools-wizban.png")); //$NON-NLS-1$

        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 20;
        composite.setLayout(layout);
        setControl(composite);

        Control locationControl = locationPart.createControl(composite);
        locationControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        locationPart.addPropertyChangeListener(WorkspaceLocationPart.PROP_LOCATION, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                LocationSelection location = locationPart.getLocation();
                String locationError = location.validate();
                setErrorMessage(locationError);
                setPageComplete(locationError == null);
            }
        });

        // Check for existing workspace/cnf
        IProject cnfProject = ResourcesPlugin.getWorkspace().getRoot().getProject(Project.BNDCNF);
        if (cnfProject != null && cnfProject.exists()) {
            File cnfDir = cnfProject.getLocation().toFile();
            setMessage(String.format("This Eclipse workspace is already configured as a bnd workspace. You will not be able to create or import a bnd workspace from elsewhere.", cnfDir), WARNING);
        }
    }

    public void setLocation(LocationSelection location) {
        locationPart.setLocation(location);
    }

    public LocationSelection getLocation() {
        return locationPart.getLocation();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        locationPart.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        locationPart.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        locationPart.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        locationPart.removePropertyChangeListener(propertyName, listener);
    }

}
