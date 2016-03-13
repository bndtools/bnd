package bndtools.wizards.workspace;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

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
        locationControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        locationPart.addPropertyChangeListener(WorkspaceLocationPart.PROP_LOCATION, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                LocationSelection location = locationPart.getLocation();
                String locationError = location.validate();
                setErrorMessage(locationError);
                setPageComplete(locationError == null);
            }
        });
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
