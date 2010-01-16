package name.neilbartlett.eclipse.bndtools.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;

public class NewBndProjectWizardFactory implements IExecutableExtensionFactory {
	public Object create() throws CoreException {
		NewBndProjectWizardFrameworkPage frameworkPage = new NewBndProjectWizardFrameworkPage();
		NewBndProjectWizardPageOne pageOne = new NewBndProjectWizardPageOne(frameworkPage);
		NewBndProjectWizardPageTwo pageTwo = new NewBndProjectWizardPageTwo(pageOne, frameworkPage);
		
		return new NewBndProjectWizard(pageOne, frameworkPage, pageTwo);
	}
}
