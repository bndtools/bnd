package name.neilbartlett.eclipse.bndtools.wizards;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import aQute.bnd.plugin.Activator;

public class WrappingBndFileWizard extends Wizard implements INewWizard {

	private IStructuredSelection selection;
	
	private ClasspathEditorWizardPage classpathPage;
	private NewWrappingBndFileWizardPage newFilePage;

	private IWorkbench workbench;

	@Override
	public boolean performFinish() {
		newFilePage.setPaths(classpathPage.getPaths());
		IFile newFile = newFilePage.createNewFile();
		if(newFile != null) {
			try {
				IDE.openEditor(workbench.getActiveWorkbenchWindow().getActivePage(), newFile);
			} catch (PartInitException e) {
				Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "", e));
			}
		}
		return newFile != null;
		/*
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				if(newFile == null)
					monitor.setCanceled(true);
			}
		};
		try {
			ResourcesPlugin.getWorkspace().run(runnable, null);
			return true;
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "New Bnd File", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error creating new Bnd resource.", e));
			return false;
		} catch (OperationCanceledException e) {
			return false;
		}
		*/
	}
	
	@Override
	public void addPages() {
		newFilePage = new NewWrappingBndFileWizardPage("newFilePage", selection);
		newFilePage.setFileExtension("bnd");
		newFilePage.setAllowExistingResources(false);

		addPage(newFilePage);
		
		classpathPage = new ClasspathEditorWizardPage("classpath", newFilePage);
		addPage(classpathPage);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}
}
