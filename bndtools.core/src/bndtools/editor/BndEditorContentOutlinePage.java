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

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.ImportPattern;
import bndtools.editor.pages.BundleContentPage;
import bndtools.editor.pages.WorkspacePage;

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

                if (element instanceof String) {
                    if (BndEditorContentOutlineProvider.EXPORTS.equals(element)) {
                        editor.setActivePage(BndEditor.CONTENT_PAGE);
                    } else if (BndEditorContentOutlineProvider.IMPORT_PATTERNS.equals(element)) {
                        editor.setActivePage(BndEditor.CONTENT_PAGE);
                    } else if (BndEditorContentOutlineProvider.PRIVATE_PKGS.equals(element)) {
                        editor.setActivePage(BndEditor.CONTENT_PAGE);
                    } else if (BndEditorContentOutlineProvider.PLUGINS.equals(element)) {
                        editor.setActivePage(BndEditor.WORKSPACE_PAGE);
                    } else {
                        editor.setActivePage((String) element);
                    }
                } else if (element instanceof ExportedPackage) {
                    BundleContentPage contentsPage = (BundleContentPage) editor.setActivePage(BndEditor.CONTENT_PAGE);
                    if (contentsPage != null) {
                        contentsPage.setSelectedExport((ExportedPackage) element);
                    }
                } else if (element instanceof PrivatePkg) {
                    BundleContentPage contentsPage = (BundleContentPage) editor.setActivePage(BndEditor.CONTENT_PAGE);
                    if (contentsPage != null) {
                        contentsPage.setSelectedPrivatePkg(((PrivatePkg) element).pkg);
                    }
                } else if (element instanceof ImportPattern) {
                    BundleContentPage contentsPage = (BundleContentPage) editor.setActivePage(BndEditor.CONTENT_PAGE);
                    if (contentsPage != null) {
                        contentsPage.setSelectedImport((ImportPattern) element);
                    }
                } else if (element instanceof PluginClause) {
                    WorkspacePage workspacePage = (WorkspacePage) editor.setActivePage(BndEditor.WORKSPACE_PAGE);
                    if (workspacePage != null)
                        workspacePage.setSelectedPlugin(((PluginClause) element).header);
                }
            }
        });

        viewer.setInput(model);
    }
}
