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

import java.io.InputStream;

import org.bndtools.api.BndtoolsConstants;
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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import aQute.bnd.build.Project;
import bndtools.Plugin;

public class NewBndFileWizardPage extends WizardNewFileCreationPage {

    private InputStream initialContents = null;

    public NewBndFileWizardPage(String pageName, IStructuredSelection selection) {
        super(pageName, selection != null ? selection : StructuredSelection.EMPTY);
        setTitle(Messages.NewBndFileWizardPage_title);
    }

    @Override
    protected String getNewFileLabel() {
        return Messages.NewBndFileWizardPage_labelBndFile;
    }

    @Override
    protected boolean validatePage() {
        boolean valid = super.validatePage();
        if (!valid)
            return valid;

        String warning = null;
        String error = null;

        String fileName = getFileName();
        IPath containerPath = getContainerFullPath();
        IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath);
        IProject project = container.getProject();

        try {
            if (project.hasNature(BndtoolsConstants.NATURE_ID)) {
                if (Project.BNDFILE.equalsIgnoreCase(fileName)) {
                    error = Messages.NewBndFileWizardPage_errorReservedFilename;
                }
            }
        } catch (CoreException e) {
            ErrorDialog.openError(getShell(), Messages.NewBndFileWizardPage_titleError, null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.NewBndFileWizardPage_errorCheckingBndNature, e));
        }

        if (container.getType() != IResource.PROJECT) {
            warning = Messages.NewBndFileWizardPage_warningNotTopLevel;
        }

        try {
            if (!project.hasNature(BndtoolsConstants.NATURE_ID)) {
                warning = Messages.NewBndFileWizardPage_warningNonBndProject;
            }
        } catch (CoreException e) {
            ErrorDialog.openError(getShell(), Messages.NewBndFileWizardPage_titleError, null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.NewBndFileWizardPage_errorCheckingBndNature, e));
        }

        setMessage(warning, IMessageProvider.WARNING);
        setErrorMessage(error);
        return error == null;
    }

    @Override
    protected InputStream getInitialContents() {
        return initialContents;
    }

    void setInitialContents(InputStream initialContents) {
        this.initialContents = initialContents;
    }
}
