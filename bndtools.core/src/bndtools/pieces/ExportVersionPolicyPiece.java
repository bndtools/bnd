package bndtools.pieces;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import bndtools.Plugin;
import bndtools.editor.exports.Messages;

public class ExportVersionPolicyPiece {
	
	private static final ExportVersionPolicy DEFAULT_VERSION_POLICY = ExportVersionPolicy.linkWithBundle;
	
	private ExportVersionPolicy exportVersionPolicy = DEFAULT_VERSION_POLICY;
	private String specifiedVersion = Plugin.DEFAULT_VERSION.toString();

	private Button btnUnspecified;
	private Button btnLinkToBundleVersion;
	private Button btnSpecify;
	private Label lblSpecificVersion;
	private Text txtSpecificVersion;

	private GridLayout layout;

	/**
	 * Create the UI piece as a decorated Group control, which has a title and
	 * may be recessed or bevelled. Clients should call either this method or
	 * {@link #createVersionPolicyComposite(Composite, int)}, but NOT both.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @param style
	 *            The style for the Group control.
	 * @param title
	 *            The title string to apply to the group.
	 * @return
	 */
	public Control createVersionPolicyGroup(Composite parent, int style, String title) {
		Group group = new Group(parent, style);
		group.setText(title);
		fillComposite(group);
		return group;
	}
	
	/**
	 * Create the UI piece as an undecorated Composite control. Clients should
	 * call either this method or
	 * {@link #createVersionPolicyGroup(Composite, int, String)}, but NOT both.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @param style
	 *            The style for the Composite control.
	 * @return
	 */
	public Control createVersionPolicyComposite(Composite parent, int style) {
		Composite composite = new Composite(parent, SWT.NONE);
		fillComposite(composite);
		return composite;
	}
	
	/**
	 * Return the layout object used to configure the internal layout of the
	 * control. This method must be called after either the
	 * {@link #createVersionPolicyGroup(Composite, int, String)} or
	 * {@link #createVersionPolicyComposite(Composite, int)} method.
	 * 
	 * @return
	 */
	public GridLayout getLayout() {
		return layout;
	}
	
	protected void fillComposite(Composite composite) {
		FieldDecoration infoDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
		
		btnUnspecified = new Button(composite, SWT.RADIO);
		btnUnspecified.setText(Messages.ExportVersionPolicyPiece_labelUnspecifiedVersion);
		ControlDecoration decorUnspec = new ControlDecoration(btnUnspecified, SWT.RIGHT, composite);
		decorUnspec.setImage(infoDecoration.getImage());
		decorUnspec.setDescriptionText(Messages.ExportVersionPolicyPiece_tooltipUnspecifiedVersion);

		btnLinkToBundleVersion = new Button(composite, SWT.RADIO);
		btnLinkToBundleVersion.setText(Messages.ExportVersionPolicyPiece_labelLinkWithBundle);
		ControlDecoration decorLinkBundle = new ControlDecoration(btnLinkToBundleVersion, SWT.RIGHT, composite);
		decorLinkBundle.setImage(infoDecoration.getImage());
		decorLinkBundle.setDescriptionText(Messages.ExportVersionPolicyPiece_tooltipLinkWithBundle);
		
		btnSpecify = new Button(composite, SWT.RADIO);
		btnSpecify.setText(Messages.ExportVersionPolicyPiece_labelSpecificVersion);
		lblSpecificVersion = new Label(composite, SWT.NONE);
		lblSpecificVersion.setText(Messages.ExportVersionPolicyPiece_labelVersion);
		txtSpecificVersion = new Text(composite, SWT.BORDER);
		
		// Initialise
		updateVersionGroupControls();
		updateVersionGroupControlEnablement();
		
		// Listeners
		SelectionAdapter selectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(btnUnspecified.getSelection()) {
					exportVersionPolicy = ExportVersionPolicy.unspecified;
				} else if(btnLinkToBundleVersion.getSelection()) {
					exportVersionPolicy = ExportVersionPolicy.linkWithBundle;
				} else {
					exportVersionPolicy = ExportVersionPolicy.specified;
				}
				updateVersionGroupControlEnablement();
			}
		};
		btnUnspecified.addSelectionListener(selectionListener);
		btnLinkToBundleVersion.addSelectionListener(selectionListener);
		btnSpecify.addSelectionListener(selectionListener);
		
		txtSpecificVersion.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				specifiedVersion = txtSpecificVersion.getText();
			}
		});
		
		// Layout
		layout = new GridLayout(2, false);
		composite.setLayout(layout);
		
		GridData gd;
		gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		btnUnspecified.setLayoutData(gd);
		gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		btnLinkToBundleVersion.setLayoutData(gd);
		gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		btnSpecify.setLayoutData(gd);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		txtSpecificVersion.setLayoutData(gd);
		
	}
	
	public ExportVersionPolicy getExportVersionPolicy() {
		return exportVersionPolicy;
	}

	public void setExportVersionPolicy(ExportVersionPolicy exportVersionPolicy) {
		this.exportVersionPolicy = exportVersionPolicy;
	}
	
	public String getSpecifiedVersion() {
		return specifiedVersion;
	}

	public void setSpecifiedVersion(String specifiedVersion) {
		this.specifiedVersion = specifiedVersion;
	}
	private void updateVersionGroupControls() {
		btnUnspecified.setSelection(exportVersionPolicy == ExportVersionPolicy.unspecified);
		btnLinkToBundleVersion.setSelection(exportVersionPolicy == ExportVersionPolicy.linkWithBundle);
		btnSpecify.setSelection(exportVersionPolicy == ExportVersionPolicy.specified);
		txtSpecificVersion.setText(specifiedVersion);
	}

	private void updateVersionGroupControlEnablement() {
		lblSpecificVersion.setEnabled(exportVersionPolicy == ExportVersionPolicy.specified);
		txtSpecificVersion.setEnabled(exportVersionPolicy == ExportVersionPolicy.specified);
	}
}
