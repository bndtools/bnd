package bndtools.internal.testcaseselection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class TestCaseSelectionDialog extends ElementListSelectionDialog {

	private final ITestCaseLister	testCaseLister;
	private final ITestCaseFilter	filter;

	private boolean					sourceOnly	= true;

	public TestCaseSelectionDialog(Shell parentShell, ITestCaseLister testCaseLister, ITestCaseFilter filter,
		String message) {
		super(parentShell, new TestCaseLabelProvider());
		this.testCaseLister = testCaseLister;
		this.filter = filter;
		setTitle(Messages.TestCaseSelectionDialog_title_select_tests);
		setMessage(message);
		setMultipleSelection(true);

	}

	public boolean isSourceOnly() {
		return sourceOnly;
	}

	public void setSourceOnly(boolean sourceOnly) {
		this.sourceOnly = sourceOnly;
	}

	@Override
	public int open() {
		try {
			setElements(testCaseLister.getTestCases(!sourceOnly, filter));
			return super.open();
		} catch (TestCaseListException e) {
			return CANCEL;
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite contents = (Composite) super.createDialogArea(parent);

		createSourceOnlyCheckbox(contents);

		return contents;
	}

	protected void createSourceOnlyCheckbox(Composite contents) {
		final Button btnSourceOnly = new Button(contents, SWT.CHECK);
		btnSourceOnly.setText(Messages.TestCaseSelectionDialog_btnSourceOnly);
		btnSourceOnly.setSelection(sourceOnly);

		btnSourceOnly.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sourceOnly = btnSourceOnly.getSelection();
				try {
					setListElements(testCaseLister.getTestCases(!sourceOnly, filter));
				} catch (TestCaseListException e1) {
					setListElements(new Object[0]);
				}
				updateOkState();
			}
		});

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		btnSourceOnly.setLayoutData(gd);
	}

	@Override
	protected void handleEmptyList() {
		// Replace super implemenentation; don't disable the fields when the
		// initial list is empty
		updateOkState();
	}
}
