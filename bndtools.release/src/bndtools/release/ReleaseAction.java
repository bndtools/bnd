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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;

public class ReleaseAction implements IObjectActionDelegate {

	private IFile[] locations;
	private IWorkbenchPart targetPart;

	public void run(IAction action) {

		if (locations != null) {
			if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
				return;
			}
			
			for (int i = 0; i < locations.length; i++) {
				File mf = locations[i].getLocation().toFile();
				if (mf.getName().equals(Project.BNDFILE)) {
					try {
						Project project = Workspace.getProject(mf
								.getParentFile());
						Workspace ws = Workspace.getWorkspace(mf
								.getParentFile().getParentFile());
						List<RepositoryPlugin> repos = ws
								.getPlugins(RepositoryPlugin.class);

						ReleaseDialogJob job = new ReleaseDialogJob(project, repos);
						job.schedule();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		locations = getLocations(selection);
	}

	@SuppressWarnings("unchecked")
	IFile[] getLocations(ISelection selection) {
		if (selection != null && (selection instanceof StructuredSelection)) {
			StructuredSelection ss = (StructuredSelection) selection;
			IFile[] result = new IFile[ss.size()];
			int n = 0;
			for (Iterator<IFile> i = ss.iterator(); i.hasNext();) {
				result[n++] = i.next();
			}
			return result;
		}
		return null;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

}
