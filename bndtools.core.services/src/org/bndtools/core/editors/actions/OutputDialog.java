package org.bndtools.core.editors.actions;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class OutputDialog extends Dialog {
    private String message;
    private String title;

    public OutputDialog(Shell parentShell, String title, String message) {
        super(parentShell);
        this.title = title;
        this.message = message;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        getShell().setText(title);
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new FillLayout());

        Text text = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        text.setEditable(false);
        text.setText(message);

        return container;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

	@Override
	protected Point getInitialSize() {
		return new Point(600, 400); // Fixed size: width x height
	}
}
