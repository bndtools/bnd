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
package name.neilbartlett.eclipse.bndtools.editor;

import java.io.IOException;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.editor.imports.ImportsPage;
import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;

public class BndEditor extends FormEditor {
	
	private BndEditModel model = null;
	private BndSourceEditorPage sourcePage;
	
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
			OverviewFormPage detailsPage = new OverviewFormPage(this, "detailsPage", "Details");
			addPage(detailsPage);

			ImportsPage importsPage = new ImportsPage(this, "importsPage", "Imports");
			addPage(importsPage);

			sourcePage = new BndSourceEditorPage("bndSourcePage", this);
			int sourcePageIndex = addPage(sourcePage, getEditorInput());
			setPageText(sourcePageIndex, "Source");
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		
		if(!IStorageEditorInput.class.isInstance(input)) {
			throw new PartInitException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Invalid editor input type.", null));
		}
		
		try {
			IStorageEditorInput storageInput = (IStorageEditorInput) input;
			IStorage storage = storageInput.getStorage();
			setPartName(storage.getName());
			
			model = new BndEditModel();
			model.loadFrom(storage.getContents());
		} catch (CoreException e) {
			throw new PartInitException("Error accessing editor input.", e);
		} catch (IOException e) {
			throw new PartInitException("Error reading editor input.", e);
		}
	}

	public BndEditModel getBndModel() {
		return this.model;
	}
}
