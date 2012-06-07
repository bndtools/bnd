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
package bndtools.builder;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class AddBndProjectNatureAction implements IObjectActionDelegate {

	private ISelection selection;
	@SuppressWarnings("unused")
    private IWorkbenchPart targetPart;

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

	public void run(IAction action) {
		if(selection instanceof IStructuredSelection) {
			Iterator<?> iter = ((IStructuredSelection) selection).iterator();
			while(iter.hasNext()) {
				Object element = iter.next();
				@SuppressWarnings("unused")
                IProject project = null;
				
				if(element instanceof IProject)
					project = null;
				else if(element instanceof IAdaptable)
					project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
				
				
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

}
