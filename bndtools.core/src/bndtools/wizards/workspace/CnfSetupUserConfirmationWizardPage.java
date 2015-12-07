package bndtools.wizards.workspace;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import bndtools.Plugin;

public class CnfSetupUserConfirmationWizardPage extends WizardPage {

    public final String PROP_DECISION = "decision";
    public final String PROP_CREATE_IN_ECLIPSE_WORKSPACE = "createInEclipseWorkspace";
    public final String PROP_EXTERNAL_LOCATION = "externalLocation";

    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    private CnfSetupDecision decision;
    private boolean createInEclipseWorkspace = true;
    private String externalLocation = "";
    private boolean valid = false;

    private Text txtExternalLocation;
    private Button btnBrowseExternalLocation;

    public CnfSetupUserConfirmationWizardPage(CnfSetupDecision decision) {
        super(CnfSetupUserConfirmationWizardPage.class.getSimpleName());
        this.decision = decision;

        setMessage(Messages.CnfSetupUserConfirmationWizardPage_this_message);
        setImageDescriptor(Plugin.imageDescriptorFromPlugin("icons/bndtools-wizban.png")); //$NON-NLS-1$
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

        btnCreateInEclipseWorkspace.setSelection(createInEclipseWorkspace);
        btnCreateExternal.setSelection(!createInEclipseWorkspace);
        txtExternalLocation.setText(externalLocation != null ? externalLocation : "");

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
                boolean oldCreateIn = createInEclipseWorkspace;
                String oldExtLoc = externalLocation;

                createInEclipseWorkspace = btnCreateInEclipseWorkspace.getSelection();
                externalLocation = txtExternalLocation.getText();

                updateEnablement();
                validate();
                getContainer().updateButtons();

                propSupport.firePropertyChange(PROP_CREATE_IN_ECLIPSE_WORKSPACE, oldCreateIn, createInEclipseWorkspace);
                propSupport.firePropertyChange(PROP_EXTERNAL_LOCATION, oldExtLoc, externalLocation);
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
        txtExternalLocation.setEnabled(!createInEclipseWorkspace);
        btnBrowseExternalLocation.setEnabled(!createInEclipseWorkspace);
    }

    private void validate() {
        String error = null;
        String warning = null;

        if (!createInEclipseWorkspace) {
            if (externalLocation == null || externalLocation.length() == 0)
                error = "Location must be specified.";
            else if (!Path.EMPTY.isValidPath(externalLocation))
                error = "Invalid location.";
            else {
                IPath path = new Path(externalLocation);
                if (!Project.BNDCNF.equals(path.lastSegment())) {
                    error = "Last path segment must be '" + Project.BNDCNF + "'";
                } else {
                    File dir = new File(externalLocation);
                    if (dir.exists() && !dir.isDirectory())
                        error = "Location already exists and is not a directory.";
                }
            }
        }

        valid = error == null;
        setErrorMessage(error);
        setMessage(warning, WARNING);
    }

    @Override
    public boolean isPageComplete() {
        return valid;
    }

    public CnfSetupDecision getDecision() {
        return decision;
    }

    public boolean isCreateInEclipseWorkspace() {
        return createInEclipseWorkspace;
    }

    public String getExternalLocation() {
        return externalLocation;
    }

    public void addPropertyChangeListener(PropertyChangeListener var0) {
        propSupport.addPropertyChangeListener(var0);
    }

    public void removePropertyChangeListener(PropertyChangeListener var0) {
        propSupport.removePropertyChangeListener(var0);
    }

}
