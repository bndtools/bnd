package bndtools.wizards.bndfile;

import java.lang.reflect.InvocationTargetException;

import org.bndtools.core.ui.IRunDescriptionExportWizard;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import aQute.lib.exceptions.Exceptions;
import bndtools.Plugin;

public class ExecutableJarExportWizard extends Wizard implements IRunDescriptionExportWizard {

	private final ExecutableJarWizardPage	destinationPage	= new ExecutableJarWizardPage();

	private Project							bndProject;

	public ExecutableJarExportWizard() {
		addPage(destinationPage);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void setBndModel(BndEditModel model, Project bndProject) {
		this.bndProject = bndProject;
	}

	@Override
	public boolean performFinish() {
		IRunnableWithProgress task;

		destinationPage.saveLastExport();
		String path = destinationPage.getJarPath();
		if (destinationPage.isFolder())
			path = destinationPage.getFolderPath();

		task = new GenerateLauncherJarRunnable(bndProject, path, destinationPage.isFolder());

		try {
			getContainer().run(true, true, task);
			return true;
		} catch (InvocationTargetException e) {
			ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
				"Error occurred during export.", Exceptions.unrollCause(e, InvocationTargetException.class)));
			return false;
		} catch (InterruptedException e) {
			return false;
		} catch (Exception e) {
			ErrorDialog.openError(getShell(), "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error occurred during export.", e));
			return false;
		}
	}

}
