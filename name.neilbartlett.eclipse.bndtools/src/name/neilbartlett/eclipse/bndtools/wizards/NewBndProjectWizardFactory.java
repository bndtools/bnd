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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;

public class NewBndProjectWizardFactory implements IExecutableExtensionFactory {
	public Object create() throws CoreException {
		NewBndProjectWizardPageOne pageOne = new NewBndProjectWizardPageOne();
		NewBndProjectWizardBundlesPage bundlesPage = new NewBndProjectWizardBundlesPage("bundleSelection");
		NewBndProjectWizardPageTwo pageTwo = new NewBndProjectWizardPageTwo(bundlesPage, pageOne);
		
		return new NewBndProjectWizard(pageOne, bundlesPage, pageTwo);
	}
}
