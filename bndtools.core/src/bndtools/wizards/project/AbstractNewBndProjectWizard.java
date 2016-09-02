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
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.bndtools.api.BndProjectResource;
import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.ProjectPaths;
import org.bndtools.build.api.BuildErrorDetailsHandler;
import org.bndtools.headless.build.manager.api.HeadlessBuildManager;
import org.bndtools.utils.copy.ResourceCopier;
import org.bndtools.utils.javaproject.JavaProjectUtils;
import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
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
import bndtools.central.Central;
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

    @SuppressWarnings({
            "static-method", "unused"
    })
    protected BndEditModel generateBndModel(IProgressMonitor monitor) {
        try {
            return new BndEditModel(Central.getWorkspace());
        } catch (Exception e) {
            logger.logInfo("Unable to create BndEditModel with Workspace, defaulting to without Workspace", e);
            return new BndEditModel();
        }
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
        headlessBuildManager.setup(enabledPlugins, false, project.getProject().getLocation().toFile(), true, enabledIgnorePlugins, new LinkedList<String>());

        /* refresh the project; files were created outside of Eclipse API */
        project.getProject().refreshLocal(IResource.DEPTH_INFINITE, progress);

        project.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, progress);
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

    @SuppressWarnings("unused")
    protected void generateProjectContent(IProject project, IProgressMonitor monitor, Map<String,String> templateParams) throws IOException {
        // this implementation does nothing.
    }

    protected abstract Map<String,String> getProjectTemplateParams();

    @Override
    public boolean performFinish() {
        boolean result = super.performFinish();
        if (result) {
            final IJavaProject javaProj = (IJavaProject) getCreatedElement();
            final IProject project = javaProj.getProject();
            final Map<String,String> templateParams = getProjectTemplateParams();

            try {
                // Run using the progress bar from the wizard dialog
                getContainer().run(false, false, new IRunnableWithProgress() {
                    @Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        try {
                            // Make changes to the project
                            final IWorkspaceRunnable op = new IWorkspaceRunnable() {
                                @Override
                                public void run(IProgressMonitor monitor) throws CoreException {
                                    try {
                                        generateProjectContent(project, monitor, templateParams);
                                    } catch (IOException e) {
                                        throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error generating project content from template.", e));
                                    }
                                }
                            };
                            javaProj.getProject().getWorkspace().run(op, monitor);
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

            // get bnd.bnd file
            IFile bndFile = javaProj.getProject().getFile(Project.BNDFILE);

            // check to see if we need to add marker about missing workspace
            try {
                if (!Central.hasWorkspaceDirectory()) {
                    IResource markerTarget = bndFile;
                    if (markerTarget == null || markerTarget.getType() != IResource.FILE || !markerTarget.exists())
                        markerTarget = project;
                    IMarker marker = markerTarget.createMarker(BndtoolsConstants.MARKER_BND_MISSING_WORKSPACE);
                    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                    marker.setAttribute(IMarker.MESSAGE, "Missing Bnd Workspace. Create a new workspace with the 'New Bnd OSGi Workspace' wizard.");
                    marker.setAttribute(BuildErrorDetailsHandler.PROP_HAS_RESOLUTIONS, true);
                    marker.setAttribute("$bndType", BndtoolsConstants.MARKER_BND_MISSING_WORKSPACE);
                }
            } catch (Exception e1) {
                // ignore exceptions, this is best effort to help new users
            }

            // Open the bnd.bnd file in the editor
            try {
                if (bndFile.exists())
                    IDE.openEditor(getWorkbench().getActiveWorkbenchWindow().getActivePage(), bndFile);
            } catch (PartInitException e) {
                ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to open project descriptor file {0} in the editor.", bndFile.getFullPath().toString()), e));
            }
        }
        return result;
    }
}
