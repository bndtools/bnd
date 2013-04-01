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
package bndtools.jareditor.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class JARContentTreePart extends AbstractFormPart {

    protected final IManagedForm managedForm;

    private final Tree tree;
    private final TreeViewer viewer;
    private final JARTreeContentProvider contentProvider = new JARTreeContentProvider();

    private String[] selectedPath = null;

    public JARContentTreePart(Composite parent, IManagedForm managedForm) {
        this.managedForm = managedForm;

        FormToolkit toolkit = managedForm.getToolkit();
        Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);

        section.setText("Content Tree");
        tree = toolkit.createTree(section, SWT.FULL_SELECTION | SWT.SINGLE);
        tree.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
        section.setClient(tree);
        toolkit.paintBordersFor(section);

        viewer = new TreeViewer(tree);
        viewer.setContentProvider(contentProvider);
        viewer.setLabelProvider(new JARTreeLabelProvider());

        managedForm.addPart(this);
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                JARContentTreePart.this.managedForm.fireSelectionChanged(JARContentTreePart.this, event.getSelection());
            }
        });

        parent.setLayout(new GridLayout());
        section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);
    }

    @Override
    public boolean isStale() {
        // Claim to always be stale, so we always get refresh events.
        return true;
    }

    @Override
    public void refresh() {
        super.refresh();
        Object input = getManagedForm().getInput();
        viewer.setInput(input);
        refreshSelectedPath();
    }

    private void refreshSelectedPath() {
        if (selectedPath != null) {
            TreePath treePath = contentProvider.findPath(selectedPath);
            if (treePath != null)
                viewer.setSelection(new TreeSelection(treePath), true);
            else
                viewer.setSelection(TreeSelection.EMPTY);
        }
    }

    @Override
    public boolean setFormInput(Object input) {
        viewer.setInput(input);
        return false;
    }

    void setSelectedPath(String[] path) {
        selectedPath = path;
        if (viewer != null && viewer.getInput() != null)
            refreshSelectedPath();
    }

    String[] getSelectedPath() {
        String[] result;
        if (viewer.getSelection().isEmpty())
            result = null;
        else {
            TreeSelection selection = (TreeSelection) viewer.getSelection();
            TreePath treePath = selection.getPaths()[0];
            result = new String[treePath.getSegmentCount()];
            for (int i = 0; i < result.length; i++)
                result[i] = treePath.getSegment(i).toString();
        }
        return result;
    }

    private static class JARTreeLabelProvider extends StyledCellLabelProvider {

        private final Image folderImg = AbstractUIPlugin.imageDescriptorFromPlugin(PluginConstants.PLUGIN_ID, "/icons/fldr_obj.gif").createImage();
        private final Image fileImg = AbstractUIPlugin.imageDescriptorFromPlugin(PluginConstants.PLUGIN_ID, "/icons/file_obj.gif").createImage();

        public JARTreeLabelProvider() {
            super();
        }

        @Override
        public void update(ViewerCell cell) {
            ZipTreeNode node = (ZipTreeNode) cell.getElement();

            String name = node.toString();

            StyledString label = new StyledString(name);

            if (name.endsWith("/")) {
                cell.setImage(folderImg);
            } else {
                cell.setImage(fileImg);
                ZipEntry entry = node.getZipEntry();
                if (entry != null) {
                    label.append(String.format(" [sz: %,d; crc: %d]", entry.getSize(), entry.getCrc()), StyledString.QUALIFIER_STYLER);
                }
            }

            cell.setText(label.getString());
            cell.setStyleRanges(label.getStyleRanges());
        }

        @Override
        public void dispose() {
            super.dispose();
            folderImg.dispose();
            fileImg.dispose();
        }
    }

    private class JARTreeContentProvider implements ITreeContentProvider {

        Map<String,ZipTreeNode> entryMap;

        public JARTreeContentProvider() {
            super();
        }

        public Object[] getChildren(Object parentElement) {
            ZipTreeNode parentNode = (ZipTreeNode) parentElement;
            return parentNode.getChildren().toArray();
        }

        public Object getParent(Object element) {
            return ((ZipTreeNode) element).getParent();
        }

        public boolean hasChildren(Object element) {
            return ((ZipTreeNode) element).hasChildren();
        }

        public Object[] getElements(Object inputElement) {
            return entryMap.values().toArray();
        }

        public void dispose() {}

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            entryMap = new TreeMap<String,ZipTreeNode>();
            URI uri = null;
            if (newInput instanceof IFileEditorInput) {
                uri = ((IFileEditorInput) newInput).getFile().getLocationURI();
            } else if (newInput instanceof IURIEditorInput) {
                uri = ((IURIEditorInput) newInput).getURI();
            }

            if (uri != null) {
                JarFile jarFile = null;
                try {
                    File ioFile = new File(uri);
                    jarFile = new JarFile(ioFile);

                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipTreeNode.addEntry(entryMap, entries.nextElement());
                    }
                } catch (IOException e) {
                    Status status = new Status(IStatus.ERROR, PluginConstants.PLUGIN_ID, 0, "I/O error reading JAR file contents", e);
                    ErrorDialog.openError(managedForm.getForm().getShell(), "Error", null, status);
                } finally {
                    try {
                        if (jarFile != null) {
                            jarFile.close();
                        }
                    } catch (IOException e) {}
                }
            }
        }

        public TreePath findPath(String[] path) {
            if (path == null || path.length == 0)
                return null;

            TreePath result = TreePath.EMPTY;
            ZipTreeNode current = entryMap.get(path[0]);
            if (current == null)
                return null;
            result = result.createChildPath(current);

            segments: for (int i = 1; i < path.length; i++) {
                Collection<ZipTreeNode> children = current.getChildren();
                for (ZipTreeNode child : children) {
                    if (path[i].equals(child.toString())) {
                        current = child;
                        result = result.createChildPath(child);
                        continue segments;
                    }
                }
                return null;
            }

            return result;
        }
    }
}
