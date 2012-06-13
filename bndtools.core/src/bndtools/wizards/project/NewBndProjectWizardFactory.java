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
package bndtools.wizards.project;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;

public class NewBndProjectWizardFactory implements IExecutableExtension, IExecutableExtensionFactory {

    private IConfigurationElement config;
    private String propertyName;
    private Object data;

    public Object create() throws CoreException {
        NewBndProjectWizardPageOne pageOne = new NewBndProjectWizardPageOne();
        NewBndProjectWizardPageTwo pageTwo = new NewBndProjectWizardPageTwo(pageOne, pageOne);

        NewBndProjectWizard wizard = new NewBndProjectWizard(pageOne, pageTwo);
        wizard.setInitializationData(config, propertyName, data);

        return wizard;
    }

    public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
        this.config = config;
        this.propertyName = propertyName;
        this.data = data;
    }
}
