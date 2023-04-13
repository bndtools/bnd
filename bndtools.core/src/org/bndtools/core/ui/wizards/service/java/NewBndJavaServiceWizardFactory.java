package org.bndtools.core.ui.wizards.service.java;

import org.bndtools.core.ui.wizards.service.NewBndServiceWizard;
import org.bndtools.core.ui.wizards.service.NewBndServiceWizardPageOne;
import org.bndtools.core.ui.wizards.service.NewBndServiceWizardPageTwo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;

public class NewBndJavaServiceWizardFactory implements IExecutableExtension, IExecutableExtensionFactory {

	private IConfigurationElement	config;
	private String					propertyName;
	private Object					data;

	@SuppressWarnings("restriction")
	@Override
	public NewBndServiceWizard create() throws CoreException {
		NewBndServiceWizardPageOne pageOne = new NewBndServiceWizardPageOne();
		pageOne.setTitle("Create a Java OSGi Service");
		NewBndJavaServiceWizard wizard = new NewBndJavaServiceWizard(pageOne, new NewBndServiceWizardPageTwo(pageOne));
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
