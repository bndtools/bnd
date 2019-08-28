package bndtools.internal.pkgselection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class PackageSelectionDialog extends ElementListSelectionDialog {

	private final IPackageLister	packageLister;
	private final IPackageFilter	filter;

	private boolean					sourceOnly	= true;

	public PackageSelectionDialog(Shell parentShell, IPackageLister packageLister, IPackageFilter filter,
		String message) {
		super(parentShell, new PackageNameLabelProvider());
		this.packageLister = packageLister;
		this.filter = filter;
		setTitle("Select Packages");
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
			setElements(packageLister.getPackages(!sourceOnly, filter));
			return super.open();
		} catch (PackageListException e) {
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
		btnSourceOnly.setText("Show source packages only");
		btnSourceOnly.setSelection(sourceOnly);

		btnSourceOnly.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sourceOnly = btnSourceOnly.getSelection();
				try {
					setListElements(packageLister.getPackages(!sourceOnly, filter));
				} catch (PackageListException e1) {
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
