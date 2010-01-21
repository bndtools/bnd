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
