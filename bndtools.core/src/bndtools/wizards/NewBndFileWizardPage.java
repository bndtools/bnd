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
package bndtools.wizards;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import bndtools.Plugin;
import bndtools.builder.BndProjectNature;

import aQute.bnd.build.Project;

public class NewBndFileWizardPage extends WizardNewFileCreationPage {

	public NewBndFileWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle("New Bnd OSGi Bundle Descriptor");
	}
	
	@Override
	protected String getNewFileLabel() {
		return "Bnd File:";
	}
	
	@Override
	protected boolean validatePage() {
		boolean valid = super.validatePage();
		if(!valid)
			return valid;
		
		String warning = null;
		String error = null;
		
		String fileName = getFileName();
		IPath containerPath = getContainerFullPath();
		IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath);
		IProject project = container.getProject();
		
		if(Project.BNDFILE.equalsIgnoreCase(fileName)) {
			error = "This file name is reserved.";
		}
		
		if(container.getType() != IResource.PROJECT) {
			warning = "Bnd bundle descriptors should be placed at the top level of a project. Non-top-level files must be manually managed using the source editor.";
		}
		
		try {
			if(!project.hasNature(BndProjectNature.NATURE_ID)) {
				warning = "The selected project is not a Bnd OSGi project. Bundle descriptors will only be built as bundles if located in a Bnd OSGi project.";
			}
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "An error occurred while checking if the selected project is a Bnd OSGi project.", e));
		}
		
		setMessage(warning, IMessageProvider.WARNING);
		setErrorMessage(error);
		return error == null;
	}
}
