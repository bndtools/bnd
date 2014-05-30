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
package bndtools.wizards.project;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

import org.bndtools.api.BndProjectResource;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.ProjectPaths;
import org.bndtools.headless.build.manager.api.HeadlessBuildManager;
import org.bndtools.utils.copy.ResourceCopier;
import org.bndtools.utils.javaproject.JavaProjectUtils;
import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.properties.Document;
import bndtools.Plugin;
import bndtools.editor.model.BndProject;
import bndtools.preferences.BndPreferences;

abstract class AbstractNewBndProjectWizard extends JavaProjectWizard {
    private static final ILogger logger = Logger.getLogger(AbstractNewBndProjectWizard.class);

    protected final NewBndProjectWizardPageOne pageOne;
    protected final NewJavaProjectWizardPageTwo pageTwo;

    AbstractNewBndProjectWizard(NewBndProjectWizardPageOne pageOne, NewJavaProjectWizardPageTwo pageTwo) {
        super(pageOne, pageTwo);
        setWindowTitle("New Bnd OSGi Project");
        setNeedsProgressMonitor(true);

        this.pageOne = pageOne;
        this.pageTwo = pageTwo;
    }

    @Override
    public void addPages() {
        addPage(pageOne);
        addPage(pageTwo);
    }

    /**
     * Generate the new Bnd model for the project. This implementation simply returns an empty Bnd model.
     *
     * @param monitor
     */
    @SuppressWarnings({
            "static-method", "unused"
    })
    protected BndEditModel generateBndModel(IProgressMonitor monitor) {
        return new BndEditModel();
    }

    /**
     * Allows for an IProjectTemplate to modify the new Bnd project
     *
     * @param monitor
     */
    @SuppressWarnings({
            "static-method", "unused"
    })
    protected BndProject generateBndProject(IProject project, IProgressMonitor monitor) {
        return new BndProject(project);
    }

    /**
     * Modify the newly generated Java project; this method is executed from within a workspace operation so is free to
     * make workspace resource modifications.
     *
     * @throws CoreException
     */
    protected void processGeneratedProject(ProjectPaths projectPaths, BndEditModel bndModel, IJavaProject project, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 3);

        Document document = new Document("");
        bndModel.saveChangesTo(document);
        progress.worked(1);

        ByteArrayInputStream bndInput;
        try {
            bndInput = new ByteArrayInputStream(document.get().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return;
        }
        IFile bndBndFile = project.getProject().getFile(Project.BNDFILE);
        if (bndBndFile.exists()) {
            bndBndFile.setContents(bndInput, false, false, progress.newChild(1));
        }

        BndProject proj = generateBndProject(project.getProject(), progress.newChild(1));

        progress.setWorkRemaining(proj.getResources().size());
        for (Map.Entry<String,BndProjectResource> resource : proj.getResources().entrySet()) {
            importResource(project.getProject(), resource.getKey(), resource.getValue(), progress.newChild(1));
        }

        if (!bndBndFile.exists()) {
            bndBndFile.create(bndInput, false, progress.newChild(1));
        }

        /* Version control ignores */
        VersionControlIgnoresManager versionControlIgnoresManager = Plugin.getDefault().getVersionControlIgnoresManager();
        Set<String> enabledIgnorePlugins = new BndPreferences().getVersionControlIgnoresPluginsEnabled(versionControlIgnoresManager, project, null);
        Map<String,String> sourceOutputLocations = JavaProjectUtils.getSourceOutputLocations(project);
        versionControlIgnoresManager.createProjectIgnores(enabledIgnorePlugins, project.getProject().getLocation().toFile(), sourceOutputLocations, projectPaths.getTargetDir());

        /* Headless build files */
        HeadlessBuildManager headlessBuildManager = Plugin.getDefault().getHeadlessBuildManager();
        Set<String> enabledPlugins = new BndPreferences().getHeadlessBuildPluginsEnabled(headlessBuildManager, null);
        headlessBuildManager.setup(enabledPlugins, false, project.getProject().getLocation().toFile(), true, enabledIgnorePlugins);

        /* refresh the project; files were created outside of Eclipse API */
        project.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    protected static IFile importResource(IProject project, String fullPath, BndProjectResource bndProjectResource, IProgressMonitor monitor) throws CoreException {
        URL url = bndProjectResource.getUrl();
        Map<String,String> replaceRegularExpressions = bndProjectResource.getReplaceRegularExpressions();
        IFile dst = project.getFile(fullPath);

        try {
            return ResourceCopier.copy(url, dst, replaceRegularExpressions, monitor);
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, e.getMessage(), e));
        }
    }

    @Override
    public boolean performFinish() {
        boolean result = super.performFinish();
        if (result) {
            final IJavaProject javaProj = (IJavaProject) getCreatedElement();
            try {
                // Run using the progress bar from the wizard dialog
                getContainer().run(false, false, new IRunnableWithProgress() {
                    @Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        try {
                            SubMonitor progress = SubMonitor.convert(monitor, 3);

                            // Generate the Bnd model
                            final BndEditModel bndModel = generateBndModel(progress.newChild(1));

                            // Make changes to the project
                            final IWorkspaceRunnable op = new IWorkspaceRunnable() {
                                @Override
                                public void run(IProgressMonitor monitor) throws CoreException {
                                    processGeneratedProject(ProjectPaths.get(pageOne.getProjectLayout()), bndModel, javaProj, monitor);
                                }
                            };
                            javaProj.getProject().getWorkspace().run(op, progress.newChild(2));
                        } catch (CoreException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
                result = true;
            } catch (InvocationTargetException e) {
                logger.logError("Could not initialise the project", e);
                ErrorDialog.openError(getShell(), "Error", "", new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error creating Bnd project descriptor file ({0}).", Project.BNDFILE), e.getTargetException()));
                result = false;
            } catch (InterruptedException e) {
                // Shouldn't happen
            }

            // Open the bnd.bnd file in the editor
            IFile bndFile = javaProj.getProject().getFile(Project.BNDFILE);
            try {
                IDE.openEditor(getWorkbench().getActiveWorkbenchWindow().getActivePage(), bndFile);
            } catch (PartInitException e) {
                ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to open project descriptor file {0} in the editor.", bndFile.getFullPath().toString()), e));
            }
        }
        return result;
    }
}
