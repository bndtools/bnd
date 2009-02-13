/*******************************************************************************
 * Copyright (c) 2009 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools.internal.pkgselection;

import java.util.Set;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class PackageSelectionDialog extends ElementListSelectionDialog {

	private final IPackageLister packageLister;
	private final boolean includeNonSource;
	private final Set<String> excludes;

	public PackageSelectionDialog(Shell parentShell,
			IPackageLister packageLister, boolean includeNonSource, Set<String> excludes) {
		super(parentShell, new PackageNameLabelProvider());
		this.packageLister = packageLister;
		this.includeNonSource = includeNonSource;
		this.excludes = excludes;
	}

	@Override
	public int open() {
		try {
			setElements(packageLister.getPackages(includeNonSource, excludes));
			return super.open();
		} catch (PackageListException e) {
			return CANCEL;
		}
	}

}
