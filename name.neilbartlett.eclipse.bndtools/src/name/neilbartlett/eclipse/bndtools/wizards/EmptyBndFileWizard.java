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
package name.neilbartlett.eclipse.bndtools.wizards;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.utils.FileUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
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

public class EmptyBndFileWizard extends Wizard implements INewWizard {
	
	private IStructuredSelection selection;
	private NewBndFileWizardPage mainPage;
	private IWorkbench workbench;

	@Override
	public void addPages() {
		super.addPages();
		
		mainPage = new NewBndFileWizardPage("newFilePage1", selection); //$NON-NLS-1$
		mainPage.setFileExtension("bnd");
		mainPage.setAllowExistingResources(false);
		
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		IFile file = mainPage.createNewFile();
		if (file == null) {
			return false;
		}
		
		// Add to the bnd.bnd descriptor
		try {
			addToBndBnd(file);
		} catch (Exception e) {
			ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error adding new bundle to project.", e));
			return false;
		}

		// Open editor on new file.
		IWorkbenchWindow dw = workbench.getActiveWorkbenchWindow();
		try {
			if (dw != null) {
				IWorkbenchPage page = dw.getActivePage();
				if (page != null) {
					IDE.openEditor(page, file, true);
				}
			}
		} catch (PartInitException e) {
			ErrorDialog.openError(getShell(), "New Bnd File", null, new Status(
					IStatus.ERROR, Plugin.PLUGIN_ID, 0,
					"Error opening editor.", e));
		}

		return true;
	}
	void addToBndBnd(IFile file) throws CoreException, IOException {
		IPath relativePath = file.getProjectRelativePath();
		
		IFile projectFile = file.getProject().getFile(Project.BNDFILE);
		IDocument projectDoc = FileUtils.readFully(projectFile);
		if(projectDoc == null)
			projectDoc = new Document();
		
		BndEditModel model = new BndEditModel();
		model.loadFrom(projectDoc);
		Collection<IPath> subBndFiles = model.getSubBndFiles();
		if(subBndFiles == null)
			subBndFiles = new LinkedList<IPath>();
		
		if(!subBndFiles.contains(relativePath)) {
			subBndFiles.add(relativePath);
			model.setSubBndFiles(subBndFiles);
			model.saveChangesTo(projectDoc);
			
			FileUtils.writeFully(projectDoc, projectFile, true);
		}
	}
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

}
