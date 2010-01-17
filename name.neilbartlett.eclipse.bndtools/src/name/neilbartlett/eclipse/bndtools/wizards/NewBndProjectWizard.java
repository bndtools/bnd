package name.neilbartlett.eclipse.bndtools.wizards;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.builder.BndIncrementalBuilder;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.dialogs.ErrorDialog;

import aQute.bnd.plugin.builder.BndBuilder;

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
