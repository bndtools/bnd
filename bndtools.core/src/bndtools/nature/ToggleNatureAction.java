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
package bndtools.nature;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.headless.build.manager.api.HeadlessBuildManager;
import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.preferences.BndPreferences;

public class ToggleNatureAction implements IObjectActionDelegate {

    private ISelection selection;
    private IWorkbenchPart targetPart;

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(IAction action) {
        if (selection instanceof IStructuredSelection) {
            for (Iterator< ? > it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
                Object element = it.next();
                IProject project = null;
                if (element instanceof IProject) {
                    project = (IProject) element;
                } else if (element instanceof IAdaptable) {
                    project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
                }
                if (project != null) {
                    boolean isJavaProject = false;
                    try {
                        isJavaProject = project.hasNature(JavaCore.NATURE_ID);
                    } catch (CoreException e) {
                        /* swallow */
                    }
                    if (isJavaProject) {
                        IStatus status = toggleNature(JavaCore.create(project));
                        if (!status.isOK())
                            ErrorDialog.openError(targetPart.getSite().getShell(), "Toggle Bnd Nature", null, status);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action .IAction,
     * org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface. action.IAction,
     * org.eclipse.ui.IWorkbenchPart)
     */
    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        this.targetPart = targetPart;
    }

    private static void ensureBndBndExists(IProject project) throws CoreException {
        IFile bndfile = project.getFile(Project.BNDFILE);
        if (!bndfile.exists())
            bndfile.create(new ByteArrayInputStream(new byte[0]), false, null);
    }

    /**
     * Toggles sample nature on a project
     *
     * @param project
     *            to have sample nature added or removed
     */
    private static IStatus toggleNature(IJavaProject project) {
        try {
            /* Version control ignores */
            VersionControlIgnoresManager versionControlIgnoresManager = Plugin.getDefault().getVersionControlIgnoresManager();
            Set<String> enabledIgnorePlugins = new BndPreferences().getVersionControlIgnoresPluginsEnabled(versionControlIgnoresManager, project, null);

            /* Headless build files */
            HeadlessBuildManager headlessBuildManager = Plugin.getDefault().getHeadlessBuildManager();
            Set<String> enabledPlugins = new BndPreferences().getHeadlessBuildPluginsEnabled(headlessBuildManager, null);

            IProject iProject = project.getProject();
            IProjectDescription description = iProject.getDescription();
            String[] natures = description.getNatureIds();

            List<String> headlessBuildWarnings = new LinkedList<>();

            for (int i = 0; i < natures.length; ++i) {
                if (BndtoolsConstants.NATURE_ID.equals(natures[i])) {
                    // Remove the nature
                    String[] newNatures = new String[natures.length - 1];
                    System.arraycopy(natures, 0, newNatures, 0, i);
                    System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
                    description.setNatureIds(newNatures);
                    iProject.setDescription(description, null);

                    /* Remove the headless build files */
                    headlessBuildManager.setup(enabledPlugins, false, iProject.getLocation().toFile(), false, enabledIgnorePlugins, headlessBuildWarnings);

                    /* refresh the project; files were created outside of Eclipse API */
                    iProject.refreshLocal(IResource.DEPTH_INFINITE, null);

                    return createStatus("Obsolete build files may remain in the project. Please review the messages below.", Collections.<String> emptyList(), headlessBuildWarnings);
                }
            }

            /* Add the headless build files */
            headlessBuildManager.setup(enabledPlugins, false, iProject.getLocation().toFile(), true, enabledIgnorePlugins, headlessBuildWarnings);

            // Add the nature
            ensureBndBndExists(iProject);
            String[] newNatures = new String[natures.length + 1];
            System.arraycopy(natures, 0, newNatures, 0, natures.length);
            newNatures[natures.length] = BndtoolsConstants.NATURE_ID;
            description.setNatureIds(newNatures);
            iProject.setDescription(description, null);

            /* refresh the project; files were created outside of Eclipse API */
            iProject.refreshLocal(IResource.DEPTH_INFINITE, null);

            return createStatus("Some build files could not be generated. Please review the messages below.", Collections.<String> emptyList(), headlessBuildWarnings);
        } catch (CoreException e) {
            return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error occurred while toggling bnd project nature", e);
        }
    }

    private static IStatus createStatus(String message, List<String> errors, List<String> warnings) {
        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, message, null);

        for (String error : errors)
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, error, null));
        for (String warning : warnings)
            status.add(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, warning, null));

        return status;
    }
}