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
import java.util.Iterator;
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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.preferences.BndPreferences;

public class ToggleNatureAction implements IObjectActionDelegate {

    private ISelection selection;

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
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
                        toggleNature(JavaCore.create(project));
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
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface. action.IAction,
     * org.eclipse.ui.IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {}

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
    private static void toggleNature(IJavaProject project) {
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

            for (int i = 0; i < natures.length; ++i) {
                if (BndtoolsConstants.NATURE_ID.equals(natures[i])) {
                    // Remove the nature
                    String[] newNatures = new String[natures.length - 1];
                    System.arraycopy(natures, 0, newNatures, 0, i);
                    System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
                    description.setNatureIds(newNatures);
                    iProject.setDescription(description, null);

                    /* Remove the headless build files */
                    headlessBuildManager.setup(enabledPlugins, false, iProject.getLocation().toFile(), false, enabledIgnorePlugins);

                    /* refresh the project; files were created outside of Eclipse API */
                    iProject.refreshLocal(IResource.DEPTH_INFINITE, null);

                    return;
                }
            }

            /* Add the headless build files */
            headlessBuildManager.setup(enabledPlugins, false, iProject.getLocation().toFile(), true, enabledIgnorePlugins);

            // Add the nature
            ensureBndBndExists(iProject);
            String[] newNatures = new String[natures.length + 1];
            System.arraycopy(natures, 0, newNatures, 0, natures.length);
            newNatures[natures.length] = BndtoolsConstants.NATURE_ID;
            description.setNatureIds(newNatures);
            iProject.setDescription(description, null);

            /* refresh the project; files were created outside of Eclipse API */
            iProject.refreshLocal(IResource.DEPTH_INFINITE, null);

            return;
        } catch (CoreException e) {}
    }
}