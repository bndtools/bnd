package name.neilbartlett.eclipse.bndtools.editor;

import name.neilbartlett.eclipse.bndtools.internal.pkgselection.IPackageFilter;
import name.neilbartlett.eclipse.bndtools.internal.pkgselection.IPackageLister;
import name.neilbartlett.eclipse.bndtools.internal.pkgselection.PackageSelectionDialog;

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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ExportedPackageSelectionDialog extends PackageSelectionDialog {
	
	public static enum ExportVersionPolicy {
		unspecified, linkWithBundle, specified
	}
	
	private static final ExportVersionPolicy DEFAULT_VERSION_POLICY = ExportVersionPolicy.linkWithBundle;
	
	private ExportVersionPolicy exportVersionPolicy = DEFAULT_VERSION_POLICY;
	private String specifiedVersion = "0.0.0";

	private Button btnUnspecified;
	private Button btnLinkToBundleVersion;
	private Button btnSpecify;
	private Label lblSpecificVersion;
	private Text txtSpecificVersion;

	public ExportedPackageSelectionDialog(Shell parentShell,
			IPackageLister packageLister, IPackageFilter filter, String message) {
		super(parentShell, packageLister, filter, message);
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

	protected Control createDialogArea(Composite parent) {
		Composite contents = (Composite) super.createDialogArea(parent);
		createVersionGroup(contents);
		return contents;
	}

	protected void createVersionGroup(Composite parent) {
		FieldDecoration infoDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
		
		Group group = new Group(parent, SWT.NONE);
		group.setText("Exported Package Version");
		
		btnUnspecified = new Button(group, SWT.RADIO);
		btnUnspecified.setText("Do not specify a version.");
		ControlDecoration decorUnspec = new ControlDecoration(btnUnspecified, SWT.RIGHT, group);
		decorUnspec.setImage(infoDecoration.getImage());
		decorUnspec.setDescriptionText("When the version is unspecified it will default to '0.0.0'.");

		btnLinkToBundleVersion = new Button(group, SWT.RADIO);
		btnLinkToBundleVersion.setText("Link with bundle version");
		ControlDecoration decorLinkBundle = new ControlDecoration(btnLinkToBundleVersion, SWT.RIGHT, group);
		decorLinkBundle.setImage(infoDecoration.getImage());
		decorLinkBundle.setDescriptionText("The package versions will kept equal to the bundle version.");
		
		btnSpecify = new Button(group, SWT.RADIO);
		btnSpecify.setText("Use a specific version:");
		lblSpecificVersion = new Label(group, SWT.NONE);
		lblSpecificVersion.setText("Version:");
		txtSpecificVersion = new Text(group, SWT.BORDER);
		
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
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(new GridLayout(2, false));
		
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
