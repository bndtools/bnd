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
		String implProjectSuffix = null;
		String consumerProjectSuffix = null;
		// This supports the ability to optionally customize
		// the implProjectSuffix (defaults to '.impl')
		// and the consumerProjectSuffix (defaults to '.consumer')
		// From the extension point configuration data
		if (this.data instanceof String) {
			String[] args = ((String) this.data).split(";");
			if (args.length > 0) {
				templateName = args[0];
				if (args.length > 1) {
					implProjectSuffix = args[1];
					if (args.length > 2) {
						consumerProjectSuffix = args[2];
					}
				}
			}
		}
		NewBndServiceWizardPageOne pageOne = new NewBndServiceWizardPageOne();
		NewBndServiceWizard wizard = new NewBndServiceWizard(pageOne, new NewBndServiceWizardPageTwo(pageOne),
			templateName, implProjectSuffix, consumerProjectSuffix);
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
