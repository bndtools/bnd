package bndtools.wizards.project;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;

public class NewBndProjectWizardFactory implements IExecutableExtension, IExecutableExtensionFactory {

	private IConfigurationElement	config;
	private String					propertyName;
	private Object					data;

	@Override
	public NewBndProjectWizard create() throws CoreException {
		NewBndProjectWizardPageOne pageOne = new NewBndProjectWizardPageOne();
		NewBndProjectWizardPageTwo pageTwo = new NewBndProjectWizardPageTwo(pageOne);

		NewBndProjectWizard wizard = new NewBndProjectWizard(pageOne, pageTwo);
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
