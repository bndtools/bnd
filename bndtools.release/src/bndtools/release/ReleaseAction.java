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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import aQute.bnd.build.Project;
import bndtools.central.Central;
import bndtools.release.nl.Messages;

public class ReleaseAction implements IObjectActionDelegate {
//	@SuppressWarnings("unused")
//	private IWorkbenchPart targetPart;

	private Map<Project, List<File>> bndFiles;

	public void run(IAction action) {

		if (bndFiles != null) {
            if (ReleaseHelper.getReleaseRepositories().length == 0) {
                Activator.message(Messages.noReleaseRepos);
                return;
            }

			if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
				return;
			}

			for (Map.Entry<Project, List<File>> me : bndFiles.entrySet()) {

				Project project;
				try {
					project = me.getKey();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				ReleaseDialogJob job;
				if (isBndBndSelected(me.getValue())) {
					job = new ReleaseDialogJob(project, null);
				} else {
					job = new ReleaseDialogJob(project, me.getValue());
				}
				job.schedule();
			}
		}
	}

	private static boolean isBndBndSelected(List<File> files) {
		for (File file : files) {
			if (Project.BNDFILE.equals(file.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		IFile[] locations = getLocations(selection);
		bndFiles = new LinkedHashMap<Project, List<File>>();
		for (IFile iFile : locations) {
			File file = iFile.getLocation().toFile();
			Project project;
			try {
			    IProject iProject = iFile.getProject();
				project = Central.getWorkspace().getProject(iProject.getName());
				// .bnd files exists in cnf that are unreleasable
				if (project == null || project.isCnf()) {
				    continue;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			List<File> projectFiles = bndFiles.get(project);
			if (projectFiles == null) {
				projectFiles = new ArrayList<File>();
				bndFiles.put(project, projectFiles);
			}
			projectFiles.add(file);
		}
	}

	static
	IFile[] getLocations(ISelection selection) {
		if (selection != null && (selection instanceof StructuredSelection)) {
			StructuredSelection ss = (StructuredSelection) selection;
			IFile[] result = new IFile[ss.size()];
			int n = 0;
			for (@SuppressWarnings("unchecked") Iterator<IFile> i = ss.iterator(); i.hasNext();) {
				result[n++] = i.next();
			}
			return result;
		}
		return null;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// this.targetPart = targetPart;
	}

}
