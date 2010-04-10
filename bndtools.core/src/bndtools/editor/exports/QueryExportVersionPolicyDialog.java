package bndtools.editor.exports;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import bndtools.pieces.ExportVersionPolicy;
import bndtools.pieces.ExportVersionPolicyPiece;

public class QueryExportVersionPolicyDialog extends TitleAreaDialog {
	
	final ExportVersionPolicyPiece policyGroup = new ExportVersionPolicyPiece();
	
	boolean toggleState = false;

	public QueryExportVersionPolicyDialog(Shell shell) {
		super(shell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Export Version");
		setMessage("Specify a version that will be declared on the newly exported packages.");
		
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite panel = new Composite(composite, SWT.NONE);
		
		Control policyControl = policyGroup.createVersionPolicyComposite(panel, SWT.NONE);

		final Button toggle = new Button(panel, SWT.CHECK);
		toggle.setText("Use my selection as the default from now on.");
		toggle.setSelection(toggleState);
		
		// Listeners
		toggle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				toggleState = toggle.getSelection();
			}
		});
		
		// Layout
		GridLayout layout;
		GridData gd;
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		panel.setLayoutData(gd);
		
		layout = new GridLayout(1, false);
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		layout.verticalSpacing = 20;
		panel.setLayout(layout);
		
		gd = new GridData(SWT.LEFT, SWT.FILL, false, false);
		gd.widthHint = 250;
		policyControl.setLayoutData(gd);
		
		return composite;
	}

	public boolean getToggleState() {
		return toggleState;
	}

	public void setToggleState(boolean toggleState) {
		this.toggleState = toggleState;
	}

	public ExportVersionPolicy getExportVersionPolicy() {
		return policyGroup.getExportVersionPolicy();
	}

	public void setExportVersionPolicy(ExportVersionPolicy exportVersionPolicy) {
		policyGroup.setExportVersionPolicy(exportVersionPolicy);
	}

	public String getSpecifiedVersion() {
		return policyGroup.getSpecifiedVersion();
	}

	public void setSpecifiedVersion(String specifiedVersion) {
		policyGroup.setSpecifiedVersion(specifiedVersion);
	}
}