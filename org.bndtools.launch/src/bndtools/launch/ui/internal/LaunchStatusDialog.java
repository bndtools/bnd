package bndtools.launch.ui.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.bndtools.core.ui.StatusLabelProvider;
import org.bndtools.utils.jface.StatusTreeContentProvider;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

public class LaunchStatusDialog extends TitleAreaDialog {

	private final IStatus	status;

	private Table			table;

	private TableViewer		viewer;
	private Text			txtDetails;

	/**
	 * Create the dialog.
	 *
	 * @param parentShell
	 */
	public LaunchStatusDialog(Shell parentShell, IStatus status) {
		super(parentShell);
		setShellStyle(SWT.BORDER | SWT.RESIZE | SWT.TITLE);
		this.status = status;

	}

	/**
	 * Create contents of the dialog.
	 *
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
		container.setLayout(new GridLayout(1, true));

		SashForm composite = new SashForm(container, SWT.VERTICAL);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite left = new Composite(composite, SWT.NONE);
		left.setLayout(new GridLayout(1, false));
		Composite right = new Composite(composite, SWT.NONE);
		right.setLayout(new GridLayout(1, false));

		Label lblProblems = new Label(left, SWT.NONE);
		lblProblems.setText("Problems:");

		Label lblDetails = new Label(right, SWT.NONE);
		lblDetails.setText("Details:");

		table = new Table(left, SWT.BORDER | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer = new TableViewer(table);

		txtDetails = new Text(right, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		txtDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label lblQuestion = new Label(container, SWT.NONE);
		GridData gd_lblQuestion = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gd_lblQuestion.horizontalSpan = 2;
		lblQuestion.setLayoutData(gd_lblQuestion);
		lblQuestion.setText("Placeholder");

		viewer.setContentProvider(new StatusTreeContentProvider());
		viewer.setLabelProvider(new StatusLabelProvider());
		viewer.setInput(status);

		viewer.addSelectionChangedListener(event -> {
			IStatus status = (IStatus) ((IStructuredSelection) event.getSelection()).getFirstElement();

			String detail = "";
			if (status != null) {
				ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();
				PrintStream printer = null;
				try {
					printer = new PrintStream(messageBuffer, false, "UTF-8");
					printer.println(status.toString());

					Throwable e = status.getException();
					if (e != null)
						e.printStackTrace(printer);

					printer.flush();
					detail = messageBuffer.toString("UTF-8");
				} catch (UnsupportedEncodingException e1) {
					/* just ignore this, should not happen */
				}
			}
			txtDetails.setText(detail);
		});

		if (status.getSeverity() >= IStatus.WARNING) {
			setMessage("One or more warnings occurred while preparing the runtime environment.");
			lblQuestion.setText("Continue launching anyway?");
		}

		return container;
	}

	/**
	 * Create contents of the button bar.
	 *
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (status.getSeverity() >= IStatus.WARNING) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.YES_LABEL, true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.NO_LABEL, false);
		}
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(700, 400);
	}
}
