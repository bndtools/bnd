/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.wizards.bndfile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.api.IPersistableBndModel;
import bndtools.editor.model.BndEditModel;
import bndtools.preferences.BndPreferences;
import bndtools.utils.FileUtils;

public class EmptyBndFileWizard extends Wizard implements INewWizard {

	protected IStructuredSelection selection;
	protected IWorkbench workbench;

	protected NewBndFileWizardPage mainPage;

	@Override
	public void addPages() {
		mainPage = new NewBndFileWizardPage("newFilePage", selection); //$NON-NLS-1$
		mainPage.setFileExtension("bnd"); //$NON-NLS-1$
		mainPage.setAllowExistingResources(false);

		addPage(mainPage);
	}

    @Override
    public boolean performFinish() {
        final EnableSubBundlesOperation operation = new EnableSubBundlesOperation(getShell(), ResourcesPlugin.getWorkspace(), mainPage.getContainerFullPath());

        try {
            getContainer().run(false, false, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        IWorkspace ws = ResourcesPlugin.getWorkspace();
                        ws.run(operation, monitor);

                        if(monitor.isCanceled())
                            throw new InterruptedException();
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
                    "An error occurred while enabling sub-bundles", e.getCause()));
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
            if (dw != null && file != null) {
                IWorkbenchPage page = dw.getActivePage();
                if (page != null) {
                    IDE.openEditor(page, file, true);
                }
            }
        } catch (PartInitException e) {
            ErrorDialog.openError(getShell(), Messages.EmptyBndFileWizard_errorTitleNewBndFile, null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
                    Messages.EmptyBndFileWizard_errorOpeningBndEditor, e));
        }

        return true;
	}
	/**
	 * @param container
	 * @return Whether it is okay to proceed with the wizard finish processing.
	 * @throws CoreException
	 * @throws IOException
	 */
	boolean enableSubBundles(IContainer container) throws CoreException, IOException {
		// Read current setting for sub-bundles
		IFile projectFile = container.getProject().getFile(Project.BNDFILE);
		IDocument projectDoc = FileUtils.readFully(projectFile);
		if(projectDoc == null)
			projectDoc = new Document();

		IPersistableBndModel model = new BndEditModel();
		model.loadFrom(projectDoc);
		Collection<String> subBndFiles = model.getSubBndFiles();
		final boolean enableSubs;

		// If -sub is unset, ask if it should be set to *.bnd
		if(subBndFiles == null || subBndFiles.isEmpty()) {
		    BndPreferences prefs = new BndPreferences();
			String enableSubsPref = prefs.getEnableSubBundles();

			if(MessageDialogWithToggle.ALWAYS.equals(enableSubsPref)) {
				enableSubs = true;
			} else if(MessageDialogWithToggle.NEVER.equals(enableSubsPref)) {
				enableSubs = false;
			} else {
				// Null, or any other value, implies "prompt"
				MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoCancelQuestion(getShell(), Messages.EmptyBndFileWizard_titleSubBundlesNotEnabled, Messages.EmptyBndFileWizard_questionSubBundlesNotEnabled, Messages.EmptyBndFileWizard_selectAsDefault, false, null, null);
				final int returnCode = dialog.getReturnCode();
				if(returnCode == IDialogConstants.CANCEL_ID) {
					return false;
				}
				enableSubs = returnCode == IDialogConstants.YES_ID;

				// Persist the selection if the toggle is on
				if(dialog.getToggleState()) {
					enableSubsPref = (returnCode == IDialogConstants.YES_ID) ? MessageDialogWithToggle.ALWAYS : MessageDialogWithToggle.NEVER;
					prefs.setEnableSubBundles(enableSubsPref);
				}
			}
		} else {
			enableSubs = false;
		}

		// Actually do it!
		if(enableSubs) {
			model.setSubBndFiles(Arrays.asList(new String[] { "*.bnd" })); //$NON-NLS-1$
			model.saveChangesTo(projectDoc);

			FileUtils.writeFully(projectDoc, projectFile, true);
		}

		return true;
	}
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

}
