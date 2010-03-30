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
import name.neilbartlett.eclipse.bndtools.editor.project.ProjectPage;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import aQute.bnd.build.Project;

public class BndEditor extends FormEditor implements IResourceChangeListener {
	
	static final String OVERVIEW_PAGE = "__overview_page";
	static final String PROJECT_PAGE = "__project_page";
	static final String COMPONENTS_PAGE = "__components_page";
	static final String EXPORTS_PAGE = "__exports_page";
	static final String IMPORTS_PAGE = "__imports_page";
	static final String SOURCE_PAGE = "__source_page";
	
	private final BndEditModel model = new BndEditModel();
	private final BndSourceEditorPage sourcePage = new BndSourceEditorPage(SOURCE_PAGE, this);
	
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
			OverviewFormPage detailsPage = new OverviewFormPage(this, model, OVERVIEW_PAGE, "Overview");
			addPage(detailsPage);
			
			String inputName = getEditorInput().getName();
			if(Project.BNDFILE.equals(inputName)) {
				ProjectPage projectPage = new ProjectPage(this, model, PROJECT_PAGE, "Project");
				addPage(projectPage);
			}

			ComponentsPage componentsPage = new ComponentsPage(this, model, COMPONENTS_PAGE, "Components");
			addPage(componentsPage);
			
			ExportPatternsPage exportsPage = new ExportPatternsPage(this, model, EXPORTS_PAGE, "Exports");
			addPage(exportsPage);
			
			ImportPatternsPage importsPage = new ImportPatternsPage(this, model, IMPORTS_PAGE, "Imports");
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
		
		String name = input.getName();
		if(Project.BNDFILE.equals(name)) {
			IResource resource = ResourceUtil.getResource(input);
			if(resource != null)
				name = resource.getProject().getName();
		}
		setPartName(name);
		
		IDocumentProvider docProvider = sourcePage.getDocumentProvider();
		IDocument document = docProvider.getDocument(input);
		try {
			model.loadFrom(document);
			model.setProjectFile(Project.BNDFILE.equals(input.getName()));
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
		IPath fullPath = myResource.getFullPath();
		delta = delta.findMember(fullPath);
		if(delta == null)
			return;
		
		if(delta.getKind() == IResourceDelta.REMOVED) {
			close(false);
		}
	}
	
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if(IContentOutlinePage.class == adapter) {
			return new BndEditorContentOutlinePage(this, model);
		}
		return super.getAdapter(adapter);
	}
}