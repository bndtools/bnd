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
package bndtools.editor.exports;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import bndtools.internal.pkgselection.IPackageFilter;
import bndtools.internal.pkgselection.IPackageLister;
import bndtools.internal.pkgselection.PackageSelectionDialog;
import bndtools.pieces.ExportVersionPolicy;
import bndtools.pieces.ExportVersionPolicyPiece;

public class ExportedPackageSelectionDialog extends PackageSelectionDialog {
	
	final ExportVersionPolicyPiece policyPiece = new ExportVersionPolicyPiece();
	
	public ExportedPackageSelectionDialog(Shell parentShell,
			IPackageLister packageLister, IPackageFilter filter, String message) {
		super(parentShell, packageLister, filter, message);
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite contents = (Composite) super.createDialogArea(parent);
		Control control = policyPiece.createVersionPolicyGroup(contents, SWT.NONE, "Exported Package Version");
		control.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return contents;
	}

	public ExportVersionPolicy getExportVersionPolicy() {
		return policyPiece.getExportVersionPolicy();
	}
	
	public void setExportVersionPolicy(ExportVersionPolicy exportVersionPolicy) {
		policyPiece.setExportVersionPolicy(exportVersionPolicy);
	}

	public void setSpecifiedVersion(String specifiedVersion) {
		policyPiece.setSpecifiedVersion(specifiedVersion);
	}

	public String getSpecifiedVersion() {
		return policyPiece.getSpecifiedVersion();
	}
}