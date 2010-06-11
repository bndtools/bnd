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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;

import bndtools.Plugin;

public class BndRunFileWizard extends Wizard implements INewWizard {

    protected IStructuredSelection selection;
    protected IWorkbench workbench;

    protected WizardNewFileCreationPage mainPage;
    private FrameworkConsoleSelectionWizardPage fwkConsolePge;

    @Override
    public void addPages() {
        mainPage = new WizardNewFileCreationPage("newFilePage", selection) {
            @Override
            protected InputStream getInitialContents() {
                return generateContents();
            };
        };
        mainPage.setTitle("New Bnd Run Descriptor");
        mainPage.setFileExtension("bndrun"); //$NON-NLS-1$
        mainPage.setAllowExistingResources(false);

        fwkConsolePge = new FrameworkConsoleSelectionWizardPage("fwkConsolePage");

        addPage(mainPage);
        addPage(fwkConsolePge);
    }

    @Override
    public boolean performFinish() {
        IFile file = mainPage.createNewFile();
        if (file == null) {
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
            ErrorDialog.openError(getShell(), "New Bnd Run File", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error opening editor", e));
        }

        return true;
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;
    }

    private InputStream generateContents() {
        StringBuilder builder = new StringBuilder();

        String framework = fwkConsolePge.getFramework();
        builder.append("-runfw: ").append(framework).append('\n');

        // Handle the console in different ways for Equinox and Felix
        if (fwkConsolePge.getConsole()) {
            if ("org.eclipse.osgi".equals(framework)) {
                builder.append("-runproperties: osgi.console=");
            } else {
                builder.append("-runbundles: org.apache.felix.shell,\\\n");
                builder.append("\torg.apache.felix.shell.tui");
            }
        }

        return new ByteArrayInputStream(builder.toString().getBytes());
    }
}