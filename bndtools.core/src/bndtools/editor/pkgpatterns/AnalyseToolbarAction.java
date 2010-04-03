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
package bndtools.editor.pkgpatterns;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.utils.EditorUtils;
import bndtools.views.impexp.AnalyseImportsJob;
import bndtools.views.impexp.ImportsExportsView;

public class AnalyseToolbarAction extends Action {
	
	private final IFormPage formPage;

	public AnalyseToolbarAction(IFormPage formPage) {
		super("analyse");
		this.formPage = formPage;
	}
	public void run() {
		IFile file = ResourceUtil.getFile(formPage.getEditorInput());
		final IWorkbenchPage workbenchPage = formPage.getEditorSite().getPage();
		
		try {
			workbenchPage.showView(ImportsExportsView.VIEW_ID);
			FormEditor editor = formPage.getEditor();
			if(EditorUtils.saveEditorIfDirty(editor, "Analyse Imports", "The editor content must be saved before continuing.")) {
				AnalyseImportsJob job = new AnalyseImportsJob("Analyse Imports", new IFile[] { file }, workbenchPage);
				job.schedule();
			}
		} catch (PartInitException e) {
			ErrorDialog.openError(workbenchPage.getWorkbenchWindow().getShell(), "Analyse Packages", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error opening Imports/Exports view", e));
		}
	};
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/cog_go.png");
	}
}
