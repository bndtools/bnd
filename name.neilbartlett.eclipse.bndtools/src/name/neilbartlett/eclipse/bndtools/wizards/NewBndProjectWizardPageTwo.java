package name.neilbartlett.eclipse.bndtools.wizards;

import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;

public class NewBndProjectWizardPageTwo extends NewJavaProjectWizardPageTwo {
	
	private final NewBndProjectWizardFrameworkPage frameworkPage;

	public NewBndProjectWizardPageTwo(NewJavaProjectWizardPageOne pageOne, NewBndProjectWizardFrameworkPage frameworkPage) {
		super(pageOne);
		this.frameworkPage = frameworkPage;
	}
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if(!visible && getContainer().getCurrentPage() == frameworkPage) {
			removeProvisonalProject();
		}
	}
}
