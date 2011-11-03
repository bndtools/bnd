package bndtools.wizards.workspace;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import bndtools.Plugin;
import bndtools.wizards.workspace.CnfSetupWizard.RequiredOperation;

public class CnfImportOrOpenWizardPage extends WizardPage {

    public static final String PROP_OPERATION = "operation";

    private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
    private final IPath cnfPath;
    private RequiredOperation operation = RequiredOperation.Import;

    private boolean suppressEvents = false;
    private Button btnImport;
    private Button btnReplace;

    private boolean shown = false;


    /**
     * Create the wizard.
     * @param cnfPath
     */
    public CnfImportOrOpenWizardPage(IPath cnfPath) {
        super("wizardPage");
        this.cnfPath = cnfPath;
        setTitle("Import Bnd Configuration Project");
        setDescription("The bnd configuration project already exists, but is not open in Eclipse. Would you like to import it?");
        setImageDescriptor(Plugin.imageDescriptorFromPlugin("icons/bndtools-wizban.png")); //$NON-NLS-1$
    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        GridLayout gl_container = new GridLayout(2, false);
        gl_container.marginHeight = 20;
        gl_container.verticalSpacing = 10;
        container.setLayout(gl_container);

        Label lblLocation = new Label(container, SWT.NONE);
        lblLocation.setText("Location:");

        Text txtLocation = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        txtLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        txtLocation.setText(cnfPath.toString());

        SelectionListener selectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!suppressEvents) {
                    try {
                        suppressEvents = true;
                        setOperation(btnImport.getSelection() ? RequiredOperation.Import : RequiredOperation.Create);
                    } finally {
                        suppressEvents = false;
                    }
                }
            }
        };
        btnImport = new Button(container, SWT.RADIO);
        btnImport.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 5, 1));
        btnImport.setText("Import and open in Eclipse workspace");
        btnImport.setSelection(operation == RequiredOperation.Import);

        btnReplace = new Button(container, SWT.RADIO);
        btnReplace.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        btnReplace.setText("Replace with a new configuration project, using Bndtools templates.");
        btnReplace.setToolTipText("Replace with a new configuration project, using Bndtools templates.");
        btnReplace.addSelectionListener(selectionListener);
        btnReplace.setSelection(operation == RequiredOperation.Create);

        btnImport.addSelectionListener(selectionListener);
    }

    public RequiredOperation getOperation() {
        return operation;
    }

    public void setOperation(RequiredOperation operation) {
        if (operation == RequiredOperation.Nothing)
            throw new IllegalArgumentException();

        RequiredOperation old = this.operation;
        this.operation = operation;
        propertySupport.firePropertyChange(PROP_OPERATION, old, operation);

        if (getControl() != null && !getControl().isDisposed() && !suppressEvents) {
            try {
                suppressEvents = true;
                btnImport.setSelection(operation == RequiredOperation.Import);
                btnReplace.setSelection(operation == RequiredOperation.Create);
            } finally {
                suppressEvents = false;
            }
        }

        updateMessage();
    }

    private void updateMessage() {
        if (operation == RequiredOperation.Create)
            setMessage("WARNING: the entire directory on disk will be deleted and replaced!", WARNING);
        else
            setMessage(null, WARNING);
    }

    @Override
    public boolean isPageComplete() {
        return shown  && super.isPageComplete();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible && !shown) {
            shown  = true;
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(propertyName, listener);
    }

}
