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
package name.neilbartlett.eclipse.bndtools;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;

import aQute.bnd.plugin.popup.actions.MakeBundle;

public class MakeBundleWithRefreshAction extends MakeBundle implements IEditorActionDelegate {
	
	private IWorkbenchPart targetPart;

	@Override
	public void run(IAction action) {
		// What should we refresh??
		IStructuredSelection selection = StructuredSelection.EMPTY;
		if(targetPart instanceof IEditorPart) {
			IEditorInput input = ((IEditorPart) targetPart).getEditorInput();
			if(input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				selection = new StructuredSelection(file);
			}
		} else {
			ISelection sel = targetPart.getSite().getSelectionProvider().getSelection();
			if(sel instanceof IStructuredSelection) {
				selection = (IStructuredSelection) sel;
			}
		}
		selectionChanged(action, selection);
		super.run(action);
		
		final List<IResource> resources = new ArrayList<IResource>(selection.size());
		for(Iterator<?> iter = selection.iterator(); iter.hasNext(); ) {
			Object next = iter.next();
			if(next instanceof IResource)
				resources.add((IResource) next);
		}
		if(!resources.isEmpty()) {
			final IWorkspace workspace = resources.get(0).getWorkspace();
			final IWorkspaceRunnable operation = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					SubMonitor progress = SubMonitor.convert(monitor, resources.size());
					MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "One or more errors occurred while refreshing resources", null);
					for (IResource resource : resources) {
						try {
							resource.getParent().refreshLocal(1, progress.newChild(1));
						} catch (CoreException e) {
							status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, e.getMessage(), e));
						}
					}
					if(!status.isOK()) {
						ErrorDialog.openError(targetPart.getSite().getShell(), "Error", null, status);
					}
				}
			};
			IRunnableWithProgress task = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						workspace.run(operation, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			try {
				targetPart.getSite().getWorkbenchWindow().run(false, false, task);
			} catch (InvocationTargetException e) {
				CoreException ce = (CoreException) e.getCause();
				ErrorDialog.openError(targetPart.getSite().getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error refreshing resources", ce));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		// Process it
		IRunnableContext context = targetPart.getSite().getWorkbenchWindow();
		try {
			context.run(false, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				}
			});
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		super.setActivePart(action, targetPart);
		this.targetPart = targetPart;
	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		setActivePart(action, targetEditor);
		
		if(targetEditor == null) {
			selectionChanged(action, new StructuredSelection());
		} else {
			IEditorInput input = targetEditor.getEditorInput();
		}
	}
}
