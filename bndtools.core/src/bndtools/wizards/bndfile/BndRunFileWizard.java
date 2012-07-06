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
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IConfigurationElement;
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
import org.osgi.framework.Bundle;

import bndtools.Plugin;
import bndtools.utils.BundleUtils;

public class BndRunFileWizard extends Wizard implements INewWizard {

    protected IStructuredSelection selection;
    protected IWorkbench workbench;

    protected WizardNewFileCreationPage mainPage;
    private final LaunchTemplateSelectionPage templatePage = new LaunchTemplateSelectionPage();

    @Override
    public void addPages() {
        mainPage = new WizardNewFileCreationPage("newFilePage", selection) {
            @Override
            protected InputStream getInitialContents() {
                return getTemplateContents();
            }
        };
        mainPage.setTitle("New Bnd Run Descriptor");
        mainPage.setFileExtension("bndrun"); //$NON-NLS-1$
        mainPage.setAllowExistingResources(false);

        addPage(mainPage);
        addPage(templatePage);
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

    private InputStream getTemplateContents() throws IllegalArgumentException {
        IConfigurationElement configElem = templatePage.getSelectedElement();
        String bsn = configElem.getContributor().getName();

        String path = configElem.getAttribute("path");
        if (path == null)
            throw new IllegalArgumentException("Missing 'path' attribute.");

        Bundle bundle = BundleUtils.findBundle(Plugin.getDefault().getBundleContext(), bsn, null);
        if (bundle == null)
            throw new IllegalArgumentException(String.format("Cannot find bundle %s.", bsn));

        try {
            URL entry = bundle.getEntry(path);
            return entry != null ? entry.openStream() : null;
        } catch (IOException e) {
            Plugin.getDefault().getLogger().logError(String.format("Unable to open template entry: %s in bundle %s", path, bsn), e);
            return null;
        }
    }
}