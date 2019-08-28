package bndtools.release;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import bndtools.release.api.ReleaseContext.Error;
import bndtools.release.nl.Messages;

public class ErrorDialog extends Dialog {

	private ErrorList	errorList;
	private String		name;

	public ErrorDialog(Shell parentShell, String name, List<Error> errors) {
		super(parentShell);
		super.setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE | SWT.MAX | SWT.MIN);
		this.name = name;
		this.errorList = new ErrorList(errors);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		Composite c2 = new Composite(composite, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 5;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 10;
		c2.setLayout(gridLayout);
		c2.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		Label label = new Label(c2, SWT.NONE);
		label.setText(Messages.project);

		Text projName = new Text(c2, SWT.BORDER);
		projName.setEditable(false);
		projName.setText(name);

		ScrolledComposite scrolled = new ScrolledComposite(composite, SWT.H_SCROLL | SWT.V_SCROLL);

		gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 5;
		gridLayout.marginWidth = 10;
		gridLayout.marginHeight = 10;

		scrolled.setLayout(gridLayout);

		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);

		scrolled.setLayoutData(gridData);

		errorList.createControl(scrolled);

		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		scrolled.setContent(errorList.getControl());
		// scrolled.setMinSize(500, 500);
		scrolled.layout(true);

		return composite;
	}

	@Override
	protected void okPressed() {
		super.okPressed();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK button only
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.errorDialogTitle1);
	}

}
