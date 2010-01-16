package name.neilbartlett.eclipse.bndtools.wizards;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;

public class NewBndProjectWizardPageOne extends NewJavaProjectWizardPageOne {
	
	private final NewBndProjectWizardFrameworkPage frameworkPage;

	NewBndProjectWizardPageOne(NewBndProjectWizardFrameworkPage frameworkPage) {
		this.frameworkPage = frameworkPage;
		setTitle("Create a Bnd OSGi Project");
		setDescription("Create a Bnd OSGi Project in the workspace or an external location.");
	}
	
	@Override
	public IClasspathEntry[] getDefaultClasspathEntries() {
		IClasspathEntry[] result = super.getDefaultClasspathEntries();
		IClasspathEntry frameworkEntry = frameworkPage.getFrameworkClasspathEntry();
		
		if(frameworkEntry != null) {
			IClasspathEntry[] tmp = new IClasspathEntry[result.length + 1];
			System.arraycopy(result, 0, tmp, 0, result.length);
			tmp[result.length] = frameworkEntry;
			
			result = tmp;
		}
		
		return result;
	}
}
