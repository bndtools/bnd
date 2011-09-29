/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.osgi.framework.Version;

public class BundleSettingsWizardPage extends WizardPage {

	private final WizardNewFileCreationPage fileCreationPage;

	private Version version = null;
	private String symbolicName = null;

	private Text txtSymbolicName;
	private Text txtVersion;
	private Text txtActivator;


	protected BundleSettingsWizardPage(String pageName,
			WizardNewFileCreationPage fileCreationPage) {
		super(pageName);
		this.fileCreationPage = fileCreationPage;
	}

	public void createControl(Composite parent) {
		// Create controls
		Composite composite = new Composite(parent, SWT.NONE);

		// Basic Group
		Group grpBasic = new Group(composite, SWT.NONE);
		grpBasic.setText("Basic Settings");
		new Label(grpBasic, SWT.NONE).setText("Symbolic Name:");
		txtSymbolicName = new Text(grpBasic, SWT.BORDER);
		new Label(grpBasic, SWT.NONE).setText("Version:");
		txtVersion = new Text(grpBasic, SWT.BORDER);
		
		// Activator Group
		Group grpActivator = new Group(composite, SWT.NONE);
		grpActivator.setText("Activator");
		new Label(grpActivator, SWT.NONE).setText("Bundle Activator:");
		txtActivator = new Text(grpActivator, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		
		// Load initial values
		if (symbolicName != null) {
			txtSymbolicName.setText(symbolicName);
		}
		if (version != null) {
			txtVersion.setText(version.toString());
		}

		setPageComplete(symbolicName != null && version != null);

		// Add listeners
		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateFields();
			}
		};
		txtSymbolicName.addModifyListener(modifyListener);
		txtVersion.addModifyListener(modifyListener);

		// Layout
		GridDataFactory horizontalFill = GridDataFactory
				.createFrom(new GridData(SWT.FILL, SWT.FILL, true, false));

		composite.setLayout(new GridLayout(1, false));
		grpBasic.setLayoutData(horizontalFill.create());

		grpBasic.setLayout(new GridLayout(2, false));
		txtSymbolicName.setLayoutData(horizontalFill.create());
		txtVersion.setLayoutData(horizontalFill.create());

		// Set control
		setControl(composite);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			String fileName = fileCreationPage.getFileName();
			if (fileName == null) {
				txtSymbolicName.setText("");
			} else {
				if (fileName.endsWith(".bnd")) {
					txtSymbolicName.setText(fileName.substring(0, fileName
							.length()
							- ".bnd".length()));
				} else {
					txtSymbolicName.setText(fileName);
				}
			}
		}
	}

	private void updateFields() {
		String error = null;

		symbolicName = txtSymbolicName.getText();
		if (symbolicName != null && symbolicName.length() > 0) {
			if (symbolicName.charAt(symbolicName.length() - 1) == '.') {
				symbolicName = null;
				error = "Symbolic name must not terminate in a period.";
			}
			for (int i = 0; i < symbolicName.length(); i++) {
				if ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-.".indexOf(symbolicName.charAt(i)) == -1) { //$NON-NLS-1$
					symbolicName = null;
					error = "Invalid character in symbolic name. Only letters, numbers, underscore, hyphen or period permitted.";
					break;
				}
			}
		} else {
			symbolicName = null;
		}

		try {
			String versionStr = txtVersion.getText();
			if (versionStr != null && versionStr.length() > 0) {
				version = new Version(txtVersion.getText());
			} else {
				version = null;
			}
		} catch (IllegalArgumentException e) {
			error = "Invalid version format";
			version = null;
		}

		setErrorMessage(error);
		setPageComplete(error == null && symbolicName != null
				&& version != null);
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public Version getVersion() {
		return version;
	}
}
