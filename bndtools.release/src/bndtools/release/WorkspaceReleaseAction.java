/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import aQute.bnd.osgi.Constants;
import bndtools.release.nl.Messages;

public class WorkspaceReleaseAction implements IObjectActionDelegate {

    private Set<IProject> projects = Collections.emptySet();

    @Override
    public void run(IAction action) {

        if (projects.size() > 0) {
            if (ReleaseHelper.getReleaseRepositories().length == 0) {
                Activator.message(Messages.noReleaseRepos);
                return;
            }

            if (!PlatformUI.getWorkbench()
                .saveAllEditors(true)) {
                return;
            }
            WorkspaceAnalyserJob job = new WorkspaceAnalyserJob(projects);
            job.setRule(ResourcesPlugin.getWorkspace()
                .getRoot());
            job.schedule();
        }
    }

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        projects = Collections.emptySet();
        if (selection != null && (selection instanceof StructuredSelection)) {
            StructuredSelection ss = (StructuredSelection) selection;
            projects = new HashSet<IProject>();
            for (Iterator<?> itr = ss.iterator(); itr.hasNext();) {
                Object selected = itr.next();
                if (selected instanceof IProject) {
                    projects.add((IProject) selected);
                } else if (selected instanceof IWorkingSet) {
                    IWorkingSet workingSet = (IWorkingSet) selected;
                    for (IAdaptable adaptable : workingSet.getElements()) {
                        IProject project = adaptable.getAdapter(IProject.class);
                        if (project != null && !projects.contains(project)) {
                            projects.add(project);
                        }
                    }
                } else if (selected instanceof IFile) {
                    IFile bndFile = (IFile) selected;
                    if (bndFile.getName()
                        .endsWith(Constants.DEFAULT_BND_EXTENSION)) {
                        if (!projects.contains(bndFile.getProject()))
                            projects.add(bndFile.getProject());
                    }
                }
            }
        }
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }
}
