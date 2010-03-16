package name.neilbartlett.eclipse.bndtools.wizards;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class NewBndProjectWizardBundlesPage extends WizardPage {

	public NewBndProjectWizardBundlesPage(String pageName) {
		super(pageName);
	}

	public NewBndProjectWizardBundlesPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	public void createControl(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText("Hello World");
		
		setControl(label);
	}
}
