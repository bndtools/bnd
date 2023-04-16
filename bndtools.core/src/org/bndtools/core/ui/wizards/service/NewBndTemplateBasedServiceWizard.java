package org.bndtools.core.ui.wizards.service;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;

public class NewBndTemplateBasedServiceWizard implements IExecutableExtension, IExecutableExtensionFactory {

	private IConfigurationElement	config;
	private String					propertyName;
	private Object					data;

	@Override
	public Object create() throws CoreException {
		String templateName = null;
		if (this.data instanceof String) {
			templateName = (String) this.data;
		}
		NewBndServiceWizardPageOne pageOne = new NewBndServiceWizardPageOne();
		NewBndServiceWizard wizard = new NewBndServiceWizard(pageOne, new NewBndServiceWizardPageTwo(pageOne),
			templateName);
		wizard.setInitializationData(config, propertyName, data);
		return wizard;

	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
		throws CoreException {
		this.config = config;
		this.propertyName = propertyName;
		this.data = data;
	}

}
