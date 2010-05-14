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


import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import bndtools.editor.components.ComponentsPage;
import bndtools.editor.contents.BundleContentPage;
import bndtools.editor.imports.ImportPatternsPage;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.model.ExportedPackage;
import bndtools.editor.model.ImportPattern;
import bndtools.editor.model.ServiceComponent;

public class BndEditorContentOutlinePage extends ContentOutlinePage {

	private final BndEditModel model;
	private final BndEditor editor;

	public BndEditorContentOutlinePage(BndEditor editor, BndEditModel model) {
		this.editor = editor;
		this.model = model;
	}
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);

		TreeViewer viewer = getTreeViewer();
		viewer.setAutoExpandLevel(2);
		viewer.setContentProvider(new BndEditorContentOutlineProvider(viewer));
		viewer.setLabelProvider(new BndEditorContentOutlineLabelProvider());

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				Object element = selection.getFirstElement();

				if(element instanceof String) {
					editor.setActivePage((String) element);
				} else if(element instanceof ServiceComponent) {
					ComponentsPage componentsPage = (ComponentsPage) editor.setActivePage(BndEditor.COMPONENTS_PAGE);
					if(componentsPage != null) {
						componentsPage.setSelectedComponent((ServiceComponent) element);
					}
				} else if(element instanceof ExportedPackage) {
                    BundleContentPage contentsPage = (BundleContentPage) editor.setActivePage(BndEditor.CONTENT_PAGE);
                    if (contentsPage != null) {
                        contentsPage.setSelectedExport((ExportedPackage) element);
                    }
				} else if(element instanceof PrivatePkg) {
                    BundleContentPage contentsPage = (BundleContentPage) editor.setActivePage(BndEditor.CONTENT_PAGE);
                    if (contentsPage != null) {
                        contentsPage.setSelectedPrivatePkg(((PrivatePkg) element).pkg);
                    }
				} else if(element instanceof ImportPattern) {
					ImportPatternsPage importsPage = (ImportPatternsPage) editor.setActivePage(BndEditor.IMPORTS_PAGE);
					if(importsPage != null) {
						importsPage.setSelectedImport((ImportPattern) element);
					}
				}
			}
		});

		viewer.setInput(model);
	}
}
