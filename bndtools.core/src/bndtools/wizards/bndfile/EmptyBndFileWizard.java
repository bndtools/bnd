package bndtools.wizards.bndfile;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import aQute.lib.exceptions.Exceptions;
import bndtools.Plugin;

public class EmptyBndFileWizard extends Wizard implements INewWizard {

	protected IStructuredSelection	selection;
	protected IWorkbench			workbench;

	protected NewBndFileWizardPage	mainPage;

	@Override
	public void addPages() {
		mainPage = new NewBndFileWizardPage("newFilePage", selection); //$NON-NLS-1$
		mainPage.setFileExtension("bnd"); //$NON-NLS-1$
		mainPage.setAllowExistingResources(false);

		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		final EnableSubBundlesOperation operation = new EnableSubBundlesOperation(getShell(),
			ResourcesPlugin.getWorkspace(), mainPage.getContainerFullPath());

		try {
			getContainer().run(false, false, monitor -> {
				try {
					IWorkspace ws = ResourcesPlugin.getWorkspace();
					ws.run(operation, monitor);

					if (monitor.isCanceled())
						throw new InterruptedException();
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			});
		} catch (InvocationTargetException e) {
			ErrorDialog.openError(getShell(), "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "An error occurred while enabling sub-bundles",
					Exceptions.unrollCause(e, InvocationTargetException.class)));
			return false;
		} catch (InterruptedException e) {
			return false;
		}

		// Open editor on new file.
		InputStream newBundleFileContent = operation.getNewBundleInputStream();
		mainPage.setInitialContents(newBundleFileContent);
		IFile file = mainPage.createNewFile();
		if (file == null)
			return false;

		IWorkbenchWindow dw = workbench.getActiveWorkbenchWindow();
		try {
			if (dw != null) {
				IWorkbenchPage page = dw.getActivePage();
				if (page != null) {
					IDE.openEditor(page, file, true);
				}
			}
		} catch (PartInitException e) {
			ErrorDialog.openError(getShell(), Messages.EmptyBndFileWizard_errorTitleNewBndFile, null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.EmptyBndFileWizard_errorOpeningBndEditor, e));
		}

		return true;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

}
