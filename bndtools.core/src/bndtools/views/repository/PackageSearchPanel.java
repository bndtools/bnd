package bndtools.views.repository;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;

import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.version.VersionRange;

public class PackageSearchPanel extends SearchPanel {

	private String	packageName;
	private String	versionRangeStr;
	private Control	focusControl;

	@Override
	public Control createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.verticalSpacing = 10;
		container.setLayout(layout);

		Label lblInstruction = new Label(container, SWT.WRAP | SWT.LEFT);
		lblInstruction.setText("Enter a package name, which may contain wildcard characters (\"*\").");
		lblInstruction.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));

		new Label(container, SWT.NONE).setText("Package Name:");
		final Text txtPackageName = new Text(container, SWT.BORDER);
		if (packageName != null)
			txtPackageName.setText(packageName);
		txtPackageName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		txtPackageName.addModifyListener(e -> {
			packageName = txtPackageName.getText()
				.trim();
			validate();
		});

		new Label(container, SWT.NONE).setText("Version Range:");
		final Text txtVersion = new Text(container, SWT.BORDER);
		if (versionRangeStr != null)
			txtVersion.setText(versionRangeStr);
		txtVersion.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtVersion.addModifyListener(e -> {
			versionRangeStr = txtVersion.getText();
			validate();
		});

		Label lblVersionHint = new Label(container, SWT.NONE);
		lblVersionHint.setText("Example: [1.0, 2.0)");

		validate();
		this.focusControl = txtPackageName;
		return container;
	}

	private void validate() {
		try {
			String filter = null;

			if (packageName == null || packageName.trim()
				.isEmpty()) {
				setError(null);
				setRequirement(null);
				return;
			}

			VersionRange versionRange = null;
			if (versionRangeStr != null && versionRangeStr.trim()
				.length() > 0) {
				try {
					versionRange = new VersionRange(versionRangeStr);
				} catch (Exception e) {
					throw new IllegalArgumentException("Invalid version range: " + e.getMessage());
				}
			}
			setRequirement(CapReqBuilder.createPackageRequirement(packageName.trim(), versionRangeStr)
				.buildSyntheticRequirement());
			setError(null);
		} catch (Exception e) {
			setError(e.getMessage());
			setRequirement(null);
		}
	}

	@Override
	public void setFocus() {
		focusControl.setFocus();
	}

	@Override
	public Image createImage(Device device) {
		return Icons.desc("package")
			.createImage(device);
	}

	@Override
	public void saveState(IMemento memento) {
		memento.putString("packageName", packageName);
		memento.putString("versionRange", versionRangeStr);
	}

	@Override
	public void restoreState(IMemento memento) {
		packageName = memento.getString("packageName");
		versionRangeStr = memento.getString("versionRange");
	}

}
