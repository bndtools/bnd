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

import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;

class NewBndProjectWizard extends JavaProjectWizard {

	private final NewBndProjectWizardPageOne pageOne;
	private final NewBndProjectWizardFrameworkPage frameworkPage;
	private final NewJavaProjectWizardPageTwo pageTwo;

	NewBndProjectWizard(NewBndProjectWizardPageOne pageOne, NewBndProjectWizardFrameworkPage frameworkPage, NewJavaProjectWizardPageTwo pageTwo) {
		super(pageOne, pageTwo);
		
		this.pageOne = pageOne;
		this.frameworkPage = frameworkPage;
		this.pageTwo = pageTwo;
	}
	
	@Override
	public void addPages() {
		addPage(pageOne);
		addPage(frameworkPage);
		addPage(pageTwo);
	}

}
