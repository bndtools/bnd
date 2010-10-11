package bndtools.wizards.workspace;

import java.util.Collection;

import org.apache.felix.bundlerepository.Requirement;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

public class UnsatisfiedRequirementsDialog extends TitleAreaDialog {
    private Table tblMandatory;
    private Table tblOptional;
    private final Collection<Requirement> mandatory;
    private final Collection<Requirement> optional;

    public UnsatisfiedRequirementsDialog(Shell parentShell, Collection<Requirement> mandatory, Collection<Requirement> optional) {
        super(parentShell);
        setShellStyle(SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);

        this.mandatory = mandatory;
        this.optional = optional;
    }

    @Override
    // Make "Cancel" the default if mandatory requirements exist
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        if (!mandatory.isEmpty())
            getShell().setDefaultButton(getButton(IDialogConstants.CANCEL_ID));
        return control;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("Unsatisfied Dependencies");

        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout(1, false));

        Composite pnlMandatory = new Composite(composite, SWT.NONE);
        pnlMandatory.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        pnlMandatory.setLayout(new GridLayout(1, false));

        Label lblMandatoryRequirements = new Label(pnlMandatory, SWT.NONE);
        lblMandatoryRequirements.setText("Mandatory Requirements:");

        tblMandatory = new Table(pnlMandatory, SWT.BORDER | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
        tblMandatory.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        tblMandatory.setHeaderVisible(true);
        tblMandatory.setLinesVisible(true);

        TableViewer mandatoryViewer = new TableViewer(tblMandatory);
        mandatoryViewer.setContentProvider(new ArrayContentProvider());
        mandatoryViewer.setLabelProvider(new RequirementLabelProvider());
        mandatoryViewer.setInput(mandatory);

        Label label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        Composite pnlOptional = new Composite(composite, SWT.NONE);
        pnlOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        pnlOptional.setLayout(new GridLayout(1, false));

        Label lblOptionalRequirements = new Label(pnlOptional, SWT.NONE);
        lblOptionalRequirements.setText("Optional Requirements");

        tblOptional = new Table(pnlOptional, SWT.BORDER | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
        tblOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        tblOptional.setHeaderVisible(true);
        tblOptional.setLinesVisible(true);

        TableViewer optionalViewer = new TableViewer(tblOptional);
        optionalViewer.setContentProvider(new ArrayContentProvider());
        optionalViewer.setLabelProvider(new RequirementLabelProvider());
        optionalViewer.setInput(optional);

        return composite;
    }
}
