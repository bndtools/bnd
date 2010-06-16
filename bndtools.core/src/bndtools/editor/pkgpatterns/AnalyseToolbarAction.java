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


import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.PartConstants;
import bndtools.Plugin;
import bndtools.model.importanalysis.ExportPackage;
import bndtools.model.importanalysis.ImportPackage;
import bndtools.model.importanalysis.RequiredBundle;
import bndtools.tasks.AnalyseBundleResolutionJob;
import bndtools.utils.EditorUtils;
import bndtools.views.ImportsExportsView;

public class AnalyseToolbarAction extends Action {

	private final IFormPage formPage;

	public AnalyseToolbarAction(IFormPage formPage) {
		super("analyse");
		this.formPage = formPage;
	}
	@Override
    public void run() {
		IFile file = ResourceUtil.getFile(formPage.getEditorInput());
		final IWorkbenchPage workbenchPage = formPage.getEditorSite().getPage();

		try {
			workbenchPage.showView(PartConstants.VIEW_ID_IMPORTSEXPORTS);
			FormEditor editor = formPage.getEditor();
			if(EditorUtils.saveEditorIfDirty(editor, "Analyse Imports", "The editor content must be saved before continuing.")) {
				final AnalyseBundleResolutionJob job = new AnalyseBundleResolutionJob("Analyse Imports", new File[] { file.getLocation().toFile() });
				job.addJobChangeListener(new JobChangeAdapter() {
				    @Override
				    public void done(IJobChangeEvent event) {
				        if(job.getResult().isOK())
				            showResults(workbenchPage, job.getResultFileArray(), job.getImportResults(), job.getExportResults(), job.getRequiredBundles());
				    }
                });
				job.schedule();
			}
		} catch (PartInitException e) {
			ErrorDialog.openError(workbenchPage.getWorkbenchWindow().getShell(), "Analyse Packages", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error opening Imports/Exports view", e));
		}
	};

    void showResults(final IWorkbenchPage page, final File[] files, final List<ImportPackage> imports, final List<ExportPackage> exports, final List<RequiredBundle> requiredBundles) {
        Display display = page.getWorkbenchWindow().getShell().getDisplay();
        display.asyncExec(new Runnable() {
            public void run() {
                IViewReference viewRef = page.findViewReference(PartConstants.VIEW_ID_IMPORTSEXPORTS);
                if(viewRef != null) {
                    ImportsExportsView view = (ImportsExportsView) viewRef.getView(false);
                    if(view != null) {
                        view.setInput(files, imports, exports, requiredBundles);
                    }
                }
            }
        });
    }

	@Override
	public ImageDescriptor getImageDescriptor() {
		return AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/cog_go.png");
	}
}
