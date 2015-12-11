package bndtools.wizards.workspace;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import aQute.bnd.build.Workspace;
import bndtools.Plugin;

public class CnfSetupUserConfirmationWizardPage extends WizardPage {

    public static final String PROP_DECISION = "decision";
    public static final String PROP_LOCATION = "location";

    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    private CnfSetupDecision decision = CnfSetupDecision.SETUP;
    private LocationSelection location;

    private boolean valid = false;

    private Text txtExternalLocation;
    private Button btnBrowseExternalLocation;

    public CnfSetupUserConfirmationWizardPage(IPath existingWorkspace) {
        super(CnfSetupUserConfirmationWizardPage.class.getSimpleName());

        setMessage(Messages.CnfSetupUserConfirmationWizardPage_this_message);
        setImageDescriptor(Plugin.imageDescriptorFromPlugin("icons/bndtools-wizban.png")); //$NON-NLS-1$

        if (existingWorkspace != null) {
            location = new LocationSelection(false, existingWorkspace.toString());
        } else {
            location = LocationSelection.WORKSPACE;
        }
    }

    @Override
    public void createControl(Composite parent) {
        Composite p = new Composite(parent, SWT.NONE);
        setControl(p);

        Text text = new Text(p, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        text.setBackground(p.getBackground());
        text.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));

        Group grpSetup = new Group(p, SWT.NONE);
        final Button btnSetup = new Button(grpSetup, SWT.RADIO);
        final Button btnSkip = new Button(grpSetup, SWT.RADIO);
        final Button btnNever = new Button(grpSetup, SWT.RADIO);

        Group grpLocation = new Group(p, SWT.NONE);
        final Button btnCreateInEclipseWorkspace = new Button(grpLocation, SWT.RADIO);
        final Button btnCreateExternal = new Button(grpLocation, SWT.RADIO);
        txtExternalLocation = new Text(grpLocation, SWT.BORDER);
        btnBrowseExternalLocation = new Button(grpLocation, SWT.PUSH);

        // LABELS
        setTitle(Messages.CnfSetupCreateTitle);
        text.setText(Messages.CnfSetupCreateExplanation);
        grpSetup.setText("Setup");

        btnSetup.setText(Messages.CnfSetupCreate);
        btnSkip.setText(Messages.CnfSetupCreateSkip);
        btnNever.setText(Messages.CnfSetupNever);

        grpLocation.setText("Location");
        btnCreateInEclipseWorkspace.setText(String.format("Create in Eclipse Workspace%n(%s).", ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString()));
        btnCreateExternal.setText("Create in:");
        btnBrowseExternalLocation.setText("Browse");

        // INIT CONTROLS
        btnSetup.setSelection(decision == CnfSetupDecision.SETUP);
        btnSkip.setSelection(decision == CnfSetupDecision.SKIP);
        btnNever.setSelection(decision == CnfSetupDecision.NEVER);

        btnCreateInEclipseWorkspace.setSelection(location.eclipseWorkspace);
        btnCreateExternal.setSelection(!location.eclipseWorkspace);
        txtExternalLocation.setText(location.externalPath != null ? location.externalPath : "");

        updateEnablement();
        validate();

        // EVENTS
        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                CnfSetupDecision old = decision;

                if (btnSetup.getSelection())
                    decision = CnfSetupDecision.SETUP;
                else if (btnSkip.getSelection())
                    decision = CnfSetupDecision.SKIP;
                else if (btnNever.getSelection())
                    decision = CnfSetupDecision.NEVER;

                validate();
                updateEnablement();
                getContainer().updateButtons();

                propSupport.firePropertyChange(PROP_DECISION, old, decision);
            }
        };
        btnSetup.addSelectionListener(listener);
        btnSkip.addSelectionListener(listener);
        btnNever.addSelectionListener(listener);

        Listener locationListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                LocationSelection oldLocation = location;
                location = new LocationSelection(btnCreateInEclipseWorkspace.getSelection(), txtExternalLocation.getText());

                updateEnablement();
                validate();
                getContainer().updateButtons();

                propSupport.firePropertyChange(PROP_LOCATION, oldLocation, location);
            }
        };
        btnCreateExternal.addListener(SWT.Selection, locationListener);
        btnCreateInEclipseWorkspace.addListener(SWT.Selection, locationListener);
        txtExternalLocation.addListener(SWT.Modify, locationListener);

        btnBrowseExternalLocation.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                String path = dialog.open();
                if (path != null) {
                    String cnf = File.separator + Workspace.CNFDIR;
                    if (!path.endsWith(cnf))
                        path = path + cnf;
                    txtExternalLocation.setText(path);
                }
            }
        });

        // LAYOUT
        GridLayout gl;

        gl = new GridLayout();
        gl.verticalSpacing = 20;
        p.setLayout(gl);

        GridDataFactory.fillDefaults().grab(true, true).hint(250, SWT.DEFAULT).applyTo(text);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(grpSetup);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(grpLocation);

        gl = new GridLayout();
        grpSetup.setLayout(gl);

        gl = new GridLayout(3, false);
        grpLocation.setLayout(gl);
        btnCreateInEclipseWorkspace.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        txtExternalLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        validate();
    }

    private void updateEnablement() {
        txtExternalLocation.setEnabled(!location.eclipseWorkspace);
        btnBrowseExternalLocation.setEnabled(!location.eclipseWorkspace);
    }

    private void validate() {
        String error = location.validate();
        valid = error == null;
        setErrorMessage(error);
    }

    @Override
    public boolean isPageComplete() {
        return valid;
    }

    public CnfSetupDecision getDecision() {
        return decision;
    }

    public LocationSelection getLocation() {
        return location;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propSupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propSupport.removePropertyChangeListener(propertyName, listener);
    }

}
