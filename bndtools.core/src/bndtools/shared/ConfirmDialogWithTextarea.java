package bndtools.shared;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A confirmation dialog that displays a scrollable, copy-pasteable details area.
 */
public class ConfirmDialogWithTextarea extends MessageDialog {

    private final String details;

    public ConfirmDialogWithTextarea(Shell parentShell, String title, String message, String details) {
        super(parentShell, title, null, message,
              MessageDialog.QUESTION,
              new String[] { "Install", "Cancel" }, 0);
        this.details = details;
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        Text text = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP);
        text.setText(details);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 500;
        gd.heightHint = 150;
        text.setLayoutData(gd);
        return text;
    }

    /** Returns true if the user clicked "Install" (index 0). */
    public static boolean open(Shell shell, String title, String message, String details) {
        ConfirmDialogWithTextarea dialog = new ConfirmDialogWithTextarea(shell, title, message, details);
        return dialog.open() == 0;
    }
}