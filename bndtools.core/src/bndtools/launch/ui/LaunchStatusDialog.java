package bndtools.launch.ui;

import org.bndtools.core.utils.jface.StatusLabelProvider;
import org.bndtools.core.utils.jface.StatusTreeContentProvider;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

public class LaunchStatusDialog extends TitleAreaDialog {

    private final IStatus status;

    private Table table;

    private TableViewer viewer;

    /**
     * Create the dialog.
     * @param parentShell
     */
    public LaunchStatusDialog(Shell parentShell, IStatus status) {
        super(parentShell);
        setShellStyle(SWT.BORDER | SWT.RESIZE | SWT.TITLE);
        this.status = status;

    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(status.getSeverity() <= IStatus.INFO ? "Launch Messages" : "Launch Problems");

        if (status.getSeverity() >= IStatus.ERROR)
            setErrorMessage(status.getMessage());
        else if (status.getSeverity() == IStatus.WARNING)
            setMessage(status.getMessage(), IMessageProvider.WARNING);
        else if (status.getSeverity() == IStatus.INFO)
            setMessage(status.getMessage(), IMessageProvider.INFORMATION);
        else
            setMessage(status.getMessage(), IMessageProvider.NONE);

        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));

        Group grpProblems = new Group(container, SWT.NONE);
        grpProblems.setLayout(new GridLayout(1, false));
        grpProblems.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        grpProblems.setText("Problems");

        table = new Table(grpProblems, SWT.BORDER | SWT.FULL_SELECTION);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        viewer = new TableViewer(table);
        viewer.setContentProvider(new StatusTreeContentProvider());
        viewer.setLabelProvider(new StatusLabelProvider());

        viewer.setInput(status);

        Label lblQuestion = new Label(container, SWT.NONE);
        lblQuestion.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblQuestion.setText("Placeholder");

        if (status.getSeverity() >= IStatus.ERROR) {
            lblQuestion.setText("One or more errors occurred while preparing the runtime environment.");
            lblQuestion.setText("The launch will be aborted");
        } else if (status.getSeverity() >= IStatus.WARNING) {
            lblQuestion.setText("One or more warnings occurred while preparing the runtime environment.");
            lblQuestion.setText("Do you want to continue launching?");
        } else {
            lblQuestion.setText("Something happened when preparing the runtime environment.");
            lblQuestion.setText("Do you want to continue launching?");
        }

        return container;
    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        if (status.getSeverity() >= IStatus.ERROR) {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        } else {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.YES_LABEL, true);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.NO_LABEL, false);
        }
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(450, 300);
    }
}
