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
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
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
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class JARContentTreePart extends AbstractFormPart {

	private final IManagedForm managedForm;

	private final Tree tree;
	private final TreeViewer viewer;


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
		viewer.setContentProvider(new JARTreeContentProvider());
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
	public void refresh() {
	    super.refresh();
	    Object input = getManagedForm().getInput();
	    viewer.setInput(input);
	}

	@Override
    public boolean setFormInput(Object input) {
		viewer.setInput(input);
		return false;
	}

    private static class JARTreeLabelProvider extends StyledCellLabelProvider {

        private final Image folderImg = AbstractUIPlugin.imageDescriptorFromPlugin(Constants.PLUGIN_ID, "/icons/fldr_obj.gif").createImage();
        private final Image fileImg = AbstractUIPlugin.imageDescriptorFromPlugin(Constants.PLUGIN_ID, "/icons/file_obj.gif").createImage();

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
                if(entry != null) {
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

		Map<String, ZipTreeNode> entryMap;

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
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			entryMap = new LinkedHashMap<String, ZipTreeNode>();
			if(newInput instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) newInput).getFile();
				try {
					File ioFile = new File(file.getLocationURI());
					JarFile jarFile = new JarFile(ioFile);

					Enumeration<JarEntry> entries = jarFile.entries();
					while (entries.hasMoreElements()) {
						ZipTreeNode.addEntry(entryMap, entries.nextElement());
					}
					jarFile.close();
				} catch (IOException e) {
					Status status = new Status(IStatus.ERROR, Constants.PLUGIN_ID, 0, "I/O error reading JAR file contents", e);
					ErrorDialog.openError(managedForm.getForm().getShell(), "Error", null, status);
				}
			}
		}
	}
}

