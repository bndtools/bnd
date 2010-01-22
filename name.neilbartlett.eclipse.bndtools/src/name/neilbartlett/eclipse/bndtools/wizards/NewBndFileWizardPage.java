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
package name.neilbartlett.eclipse.bndtools.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

public class NewBndFileWizardPage extends WizardNewFileCreationPage {

	public NewBndFileWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
	}
	
	@Override
	protected String getNewFileLabel() {
		return "Bnd File:";
	}
}
