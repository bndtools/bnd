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
package name.neilbartlett.eclipse.bndtools.editor;

import java.io.IOException;

import name.neilbartlett.eclipse.bndtools.editor.components.ComponentsPage;
import name.neilbartlett.eclipse.bndtools.editor.exports.ExportPatternsPage;
import name.neilbartlett.eclipse.bndtools.editor.imports.ImportPatternsPage;
import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class BndEditor extends FormEditor implements IResourceChangeListener {
	
	private final BndEditModel model = new BndEditModel();
	private final BndSourceEditorPage sourcePage = new BndSourceEditorPage("bndSourcePage", this);;
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		if(sourcePage.isActive() && sourcePage.isDirty()) {
			sourcePage.commit(true);
		} else {
			commitPages(true);
			sourcePage.refresh();
		}
		sourcePage.doSave(monitor);
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	protected void handlePropertyChange(int propertyId) {
		super.handlePropertyChange(propertyId);
	}

	@Override
	protected void addPages() {
		try {
			OverviewFormPage detailsPage = new OverviewFormPage(this, model, "detailsPage", "Overview");
			addPage(detailsPage);

			ComponentsPage componentsPage = new ComponentsPage(this, model, "componentsPage", "Components");
			addPage(componentsPage);
			
			ExportPatternsPage exportsPage = new ExportPatternsPage(this, model, "exportsPage", "Exports");
			addPage(exportsPage);
			
			ImportPatternsPage importsPage = new ImportPatternsPage(this, model, "importsPage", "Imports");
			addPage(importsPage);
			
			int sourcePageIndex = addPage(sourcePage, getEditorInput());
			setPageText(sourcePageIndex, "Source");
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		sourcePage.init(site, input);
		setPartName(input.getName());
		
		IDocumentProvider docProvider = sourcePage.getDocumentProvider();
		IDocument document = docProvider.getDocument(input);
		try {
			model.loadFrom(document);
		} catch (IOException e) {
			throw new PartInitException("Error reading editor input.", e);
		}
		
		
		IResource resource = ResourceUtil.getResource(input);
		if(resource != null) {
			resource.getWorkspace().addResourceChangeListener(this);
		}
	}
	@Override
	public void dispose() {
		IResource resource = ResourceUtil.getResource(getEditorInput());
		
		super.dispose();
		
		if(resource != null) {
			resource.getWorkspace().removeResourceChangeListener(this);
		}
	}

	public BndEditModel getBndModel() {
		return this.model;
	}

	public void resourceChanged(IResourceChangeEvent event) {
		IResource myResource = ResourceUtil.getResource(getEditorInput());
		
		IResourceDelta delta = event.getDelta();
		delta = delta.findMember(myResource.getFullPath());
		if(delta == null)
			return;
		
		if(delta.getKind() == IResourceDelta.REMOVED) {
			close(false);
		}
	}
}