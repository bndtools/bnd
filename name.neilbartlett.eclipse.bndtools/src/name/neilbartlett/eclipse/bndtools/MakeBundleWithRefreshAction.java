/*******************************************************************************
 * Copyright (c) 2009 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.FileEditorInput;

import aQute.bnd.plugin.popup.actions.MakeBundle;

public class MakeBundleWithRefreshAction extends MakeBundle implements IEditorActionDelegate {
	
	IStructuredSelection selection;
	private IWorkbenchPart targetPart;

	@Override
	public void run(IAction action) {
		super.run(action);

		MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "One or more errors occurred while refreshing resources", null);
		//ProgressMonitorDialog monitor = new ProgressMonitorDialog(targetPart.getSite().getShell());
		for(Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
			IResource resource = (IResource) iter.next();
			try {
				resource.getParent().refreshLocal(1, null);
			} catch (CoreException e) {
				status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, e.getMessage(), e));
			}
		}
		if(!status.isOK()) {
			ErrorDialog.openError(targetPart.getSite().getShell(), "Error", null, status);
		}
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		if(selection instanceof IStructuredSelection) {
			super.selectionChanged(action, selection);
			this.selection = (IStructuredSelection) selection;
		}
	}
	
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		super.setActivePart(action, targetPart);
		this.targetPart = targetPart;
	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		setActivePart(action, targetEditor);
		
		IEditorInput input = targetEditor.getEditorInput();
		if(input instanceof FileEditorInput) {
			IFile file = ((FileEditorInput) input).getFile();
			selectionChanged(action, new StructuredSelection(file));
		}
	}
}
