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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
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

    private static class WrappingException extends RuntimeException {
        private final Exception e;

        public WrappingException(Exception e) {
            this.e = e;
        }

        public Exception getWrapped() {
            return e;
        }
    }

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
        } catch (WrappingException e) {
            ErrorDialog.openError(getShell(), "New Bnd Run File", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error generating file", e.getWrapped()));
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
                } catch (Exception e) {
                    throw new WrappingException(e);
                }
            }
        };
        mainPage.setTitle("New Bnd Run Descriptor");
        mainPage.setFileExtension("bndrun"); //$NON-NLS-1$
        mainPage.setAllowExistingResources(false);

        templatePage = new RepoTemplateSelectionWizardPage("runTemplateSelection", "bndrun", workbench);
        templatePage.setTitle("Select Run Descriptor Template");
    }

    private String baseName(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        String base = lastDot >= 0 ? fileName.substring(0, lastDot) : fileName;
        int lastSlash = base.lastIndexOf('/');
        base = lastSlash >= 0 ? base.substring(lastSlash + 1) : base;
        return base;
    }

    private InputStream getTemplateContents(String fileName) throws Exception {
        // Load properties
        Map<String,List<Object>> params = new HashMap<>();
        params.put("fileName", Collections.<Object> singletonList(fileName));
        params.put("fileBaseName", Collections.<Object> singletonList(baseName(fileName)));

        IPath containerPath = mainPage.getContainerFullPath();
        if (containerPath != null) {
            IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath);
            if (container != null) {
                String projectName = container.getProject().getName();
                params.put("projectName", Collections.<Object> singletonList(projectName));
            }
        }

        // Run the template processor
        Template template = templatePage.getTemplate();
        StringTemplateEngine templateEngine = new StringTemplateEngine();
        ResourceMap inputs = template.getInputSources();
        ResourceMap outputs;
        outputs = templateEngine.generateOutputs(inputs, params);
        Resource output = outputs.get(fileName);

        if (output == null)
            throw new IllegalArgumentException("File not found in template outputs: " + fileName);

        // Pull the generated content
        return output.getContent();
    }
}