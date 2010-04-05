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
package bndtools.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;


import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import bndtools.editor.model.BndEditModel;
import bndtools.editor.model.ExportedPackage;
import bndtools.editor.model.ImportPattern;
import bndtools.editor.model.ServiceComponent;

import aQute.lib.osgi.Constants;

public class BndEditorContentOutlineProvider implements ITreeContentProvider, PropertyChangeListener {
	
	BndEditModel model;
	private final TreeViewer viewer;
	
	public BndEditorContentOutlineProvider(TreeViewer viewer) {
		this.viewer = viewer;
	}
	public Object[] getElements(Object inputElement) {
		Object[] result;
		if(model.isProjectFile()) {
			result = new String[] { BndEditor.OVERVIEW_PAGE, BndEditor.PROJECT_PAGE, BndEditor.COMPONENTS_PAGE, BndEditor.EXPORTS_PAGE, BndEditor.IMPORTS_PAGE, BndEditor.SOURCE_PAGE };
		} else {
			result = new String[] { BndEditor.OVERVIEW_PAGE, BndEditor.COMPONENTS_PAGE, BndEditor.EXPORTS_PAGE, BndEditor.IMPORTS_PAGE, BndEditor.SOURCE_PAGE };
		}
		return result;
	}
	public void dispose() {
		if(model != null)
			model.removePropertyChangeListener(this);
	}
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if(model != null)
			model.removePropertyChangeListener(this);
		
		model = (BndEditModel) newInput;
		if(model != null)
			model.addPropertyChangeListener(this);
	}
	public Object[] getChildren(Object parentElement) {
		Object[] result = new Object[0];
		
		if(parentElement instanceof String) {
			if(BndEditor.COMPONENTS_PAGE.equals(parentElement)) {
				Collection<ServiceComponent> components = model.getServiceComponents();
				if(components != null)
					result = (ServiceComponent[]) components.toArray(new ServiceComponent[components.size()]);
			} else if(BndEditor.EXPORTS_PAGE.equals(parentElement)) {
				List<ExportedPackage> exports = model.getExportedPackages();
				if(exports != null)
					result = exports.toArray(new Object[exports.size()]);
			} else if(BndEditor.IMPORTS_PAGE.equals(parentElement)) {
				List<ImportPattern> imports = model.getImportPatterns();
				if(imports != null)
					result = imports.toArray(new Object[imports.size()]);
			}
		}
		
		return result;
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		if(element instanceof String) {
			if(BndEditor.COMPONENTS_PAGE.equals(element)) {
				Collection<ServiceComponent> components = model.getServiceComponents();
				return components != null && !components.isEmpty();
			}
			if(BndEditor.EXPORTS_PAGE.equals(element)) {
				List<ExportedPackage> exports = model.getExportedPackages();
				return exports != null && !exports.isEmpty();
			}
			if(BndEditor.IMPORTS_PAGE.equals(element)) {
				List<ImportPattern> imports = model.getImportPatterns();
				return imports != null && !imports.isEmpty();
			}
		}
		return false;
	}
	public void propertyChange(PropertyChangeEvent evt) {
		if(Constants.SERVICE_COMPONENT.equals(evt.getPropertyName())) {
			viewer.refresh(BndEditor.COMPONENTS_PAGE);
			viewer.expandToLevel(BndEditor.COMPONENTS_PAGE, 1);
		} else if(Constants.EXPORT_PACKAGE.equals(evt.getPropertyName())) {
			viewer.refresh(BndEditor.EXPORTS_PAGE);
			viewer.expandToLevel(BndEditor.EXPORTS_PAGE, 1);
		} else if(Constants.IMPORT_PACKAGE.equals(evt.getPropertyName())) {
			viewer.refresh(BndEditor.IMPORTS_PAGE);
			viewer.expandToLevel(BndEditor.IMPORTS_PAGE, 1);
		}
	}
}
