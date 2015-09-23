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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.core.ui.wizards.shared.RepoTemplateSelectionWizardPage;
import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.Template;
import org.bndtools.templating.engine.StringTemplateEngine;
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
    private static final ILogger logger = Logger.getLogger(BndRunFileWizard.class);

    protected RepoTemplateSelectionWizardPage templatePage;

    protected IStructuredSelection selection;
    protected IWorkbench workbench;

    protected WizardNewFileCreationPage mainPage;

    @Override
    public void addPages() {
        addPage(mainPage);
        addPage(templatePage);
    }

    @Override
    public boolean performFinish() {
        try {
            IFile file = mainPage.createNewFile();
            if (file == null) {
                return false;
            }

            // Open editor on new file.
            IWorkbenchWindow dw = workbench.getActiveWorkbenchWindow();
            if (dw != null) {
                IWorkbenchPage page = dw.getActivePage();
                if (page != null) {
                    IDE.openEditor(page, file, true);
                }
            }
            return true;
        } catch (PartInitException e) {
            ErrorDialog.openError(getShell(), "New Bnd Run File", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error opening editor", e));
            return true;
        } catch (RuntimeException e) {
            ErrorDialog.openError(getShell(), "New Bnd Run File", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error generating file", e));
            return false;
        }
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;

        mainPage = new WizardNewFileCreationPage("newFilePage", selection) {
            @Override
            protected InputStream getInitialContents() {
                try {
                    return getTemplateContents(getFileName());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        mainPage.setTitle("New Bnd Run Descriptor");
        mainPage.setFileExtension("bndrun"); //$NON-NLS-1$
        mainPage.setAllowExistingResources(false);

        templatePage = new RepoTemplateSelectionWizardPage("runTemplateSelection", "bndrun", workbench);
        templatePage.setTitle("Select Run Descriptor Template");
    }

    private InputStream getTemplateContents(String fileName) throws IOException {
        Template template = templatePage.getTemplate();
        StringTemplateEngine templateEngine = new StringTemplateEngine();

        Map<String,List<Object>> params = new HashMap<>();
        params.put("fileName", Collections.<Object> singletonList(fileName));
        ResourceMap inputs = template.getInputSources();
        ResourceMap outputs;
        try {
            outputs = templateEngine.generateOutputs(inputs, params);
        } catch (Exception e) {
            throw new IOException("Error generating template outputs", e);
        }
        Resource output = outputs.get(fileName);

        if (output == null)
            throw new IllegalArgumentException("File not found in template outputs: " + fileName);

        return output.getContent();
    }
}