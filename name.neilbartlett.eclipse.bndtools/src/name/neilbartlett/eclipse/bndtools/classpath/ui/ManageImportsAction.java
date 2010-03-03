/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools.classpath.ui;

import name.neilbartlett.eclipse.bndtools.classpath.WorkspaceRepositoryClasspathContainerInitializer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class ManageImportsAction implements IObjectActionDelegate {

	private ISelection selection;
	private IWorkbenchPart targetPart;

	public void run(IAction action) {
		if(selection.isEmpty() || !(selection instanceof IStructuredSelection))
			return;
		
		Object element = ((IStructuredSelection) selection).getFirstElement();
		try {
			if(element instanceof IProject) {
				execute((IProject) element);
			} else if(element instanceof IAdaptable) {
				IProject project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
				if(project != null)
					execute(project);
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void execute(IProject project) throws JavaModelException {
		IJavaProject javaProject = JavaCore.create(project);
		
		IClasspathEntry repoClasspathEntry = null;
		IClasspathEntry[] classpath = javaProject.getRawClasspath();
		for (IClasspathEntry entry : classpath) {
			if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && WorkspaceRepositoryClasspathContainerInitializer.CONTAINER_ID.equals(entry.getPath().segment(0))) {
				repoClasspathEntry = entry;
				break;
			}
		}
		
		if(repoClasspathEntry != null) {
			ClassPathContainer containerView = new ClassPathContainer(javaProject, repoClasspathEntry);
			PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(targetPart.getSite().getShell(), containerView, null, null, null);
			dialog.open();
		}
	}
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}
}
