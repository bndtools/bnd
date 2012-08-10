/*******************************************************************************
 * Copyright (c) 2012 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release.ui;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.version.Version;
import bndtools.release.nl.Messages;

public class BundleTree extends Composite {

	public final static String VERSION_STRING = "(\\d+)(\\.(\\d+)(\\.(\\d+)(\\.([-_\\da-zA-Z]+))?)?)?";
	public final static Pattern VERSION = Pattern.compile(VERSION_STRING);

	protected SashForm sashForm;
	protected Button showAll;

	protected TreeViewer infoViewer;
	protected Composite infoViewerComposite;

	protected TreeViewer bundleTreeViewer;
	protected Composite bundleTreeViewerComposite;

	protected TreeContentProvider bundleTreeViewerProvider = new TreeContentProvider();
	protected InfoContentProvider infoTreeViewerProvider = new InfoContentProvider();

	public BundleTree(Composite composite) {
		this(composite, SWT.NONE);
	}

	public BundleTree(Composite composite, int style) {
		super(composite, style);
		createControl();
	}

	public void createControl() {

		sashForm = new SashForm(this, SWT.VERTICAL); 
		sashForm.setLayout(new FillLayout());
		sashForm.setSashWidth(10);

		createInfoViewer(sashForm);
		createBundleTreeViewer(sashForm);

		showAll = createButtons(this);

		setLayout(new BundleTreeLayout());

		sashForm.setWeights(new int[] { 30, 70 });

	}

	private void createInfoViewer(Composite container) {

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;

		infoViewerComposite = new Composite(container, SWT.NONE);
		infoViewerComposite.setLayout(gridLayout);

		infoViewer = new TreeViewer(infoViewerComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		infoViewer.setUseHashlookup(true);

		GridData gd = new GridData(GridData.FILL, GridData.FILL, true, true);

		infoViewer.getTree().setLayoutData(gd);
		infoViewer.getTree().setHeaderVisible(true);

		TreeViewerColumn treeColumn = new TreeViewerColumn(infoViewer, SWT.NONE);
		treeColumn.getColumn().setText(Messages.bundleAndPackageName);
		treeColumn.getColumn().setWidth(450);
		treeColumn.setLabelProvider(new InfoLabelProvider());

		TreeViewerColumn currentVersion = new TreeViewerColumn(infoViewer, SWT.NONE);
		currentVersion.getColumn().setText(Messages.version);
		currentVersion.getColumn().setWidth(80);
		currentVersion.setLabelProvider(new ColumnLabelProvider() {
			@Override
            public String getText(Object element) {
                if (element instanceof Baseline) {
                    return ((Baseline) element).getOlderVersion().toString();
                }
				if (element instanceof Info) {
					return ((Info) element).olderVersion.toString();
				}
				return "";
			}
		});
		TreeViewerColumn suggestedVersion = new TreeViewerColumn(infoViewer, SWT.NONE);
		suggestedVersion.getColumn().setText(Messages.newVersion);
		suggestedVersion.getColumn().setWidth(80);
		suggestedVersion.setLabelProvider(new ColumnLabelProvider() {
			@Override
            public String getText(Object element) {
                if (element instanceof Baseline) {
                    return ((Baseline) element).getSuggestedVersion().toString();
                }
				if (element instanceof Info) {
					return ((Info) element).suggestedVersion.toString();
				}
				return "";
			}
		});
		suggestedVersion.setEditingSupport(new InlineComboEditingSupport(infoViewer));

		infoViewer.setContentProvider(infoTreeViewerProvider);
		infoViewer.setAutoExpandLevel(2);

	}

	private void createBundleTreeViewer(Composite container) {

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;

		bundleTreeViewerComposite = new Composite(container, SWT.NONE);
		bundleTreeViewerComposite.setLayout(gridLayout);

		bundleTreeViewer = new TreeViewer(bundleTreeViewerComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		bundleTreeViewer.setUseHashlookup(true);

		GridData gd = new GridData(GridData.FILL, GridData.FILL, true, true);

		bundleTreeViewer.getTree().setLayoutData(gd);
		bundleTreeViewer.getTree().setHeaderVisible(true);

		TreeViewerColumn treeColumn = new TreeViewerColumn(bundleTreeViewer, SWT.NONE);
		treeColumn.getColumn().setText(Messages.symbNameResources);
		treeColumn.getColumn().setWidth(610);
		treeColumn.setLabelProvider(new TreeLabelProvider());

		bundleTreeViewer.setContentProvider(bundleTreeViewerProvider);
		bundleTreeViewer.setAutoExpandLevel(3);
	}

	private Button createButtons(Composite comp) {

		final Button showAll = new Button(comp, SWT.CHECK);
		showAll.setText(Messages.showAllPackages);
		showAll.setFont(getFont());

		GridData data = new GridData();
		data.grabExcessHorizontalSpace = true;
		data.horizontalIndent = -1;
		showAll.setLayoutData(data);
		showAll.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent event) {
				Widget widget = event.widget;
				if (widget == showAll) {

					bundleTreeViewerProvider.setShowAll(!bundleTreeViewerProvider.isShowAll());
					bundleTreeViewer.setSelection(null, false);
					infoTreeViewerProvider.setShowAll(!infoTreeViewerProvider.isShowAll());
					infoViewer.setSelection(null, false);

					boolean showInfoView = showInfoView(bundleTreeViewer.getInput());
					if (showInfoView) {
						sashForm.setMaximizedControl(null);
					} else {
						sashForm.setMaximizedControl(bundleTreeViewerComposite);
					}
					bundleTreeViewer.refresh();
					infoViewer.refresh();
					sashForm.redraw();
				}
			}
		});
		return showAll;
	}

	public void setInput(Object input) {

	    boolean showInfoView = showInfoView(input);
		if (showInfoView) {
			sashForm.setMaximizedControl(null);
		} else {
			sashForm.setMaximizedControl(bundleTreeViewerComposite);
		}

		if (input == null) {
			bundleTreeViewer.setInput(Collections.emptyList());
			infoViewer.setInput(Collections.emptyList());
		} else {
            bundleTreeViewer.getTree().setRedraw(false);
            bundleTreeViewer.setSelection(null, false);
            bundleTreeViewer.setInput(input);
    		bundleTreeViewer.getTree().setRedraw(true);
            infoViewer.getTree().setRedraw(false);
            infoViewer.setSelection(null, false);
    		infoViewer.setInput(input);
            infoViewer.getTree().setRedraw(true);
		}
		sashForm.redraw();
	}

	private static boolean showInfoView(Object input) {
	    if (input instanceof List && ((List<?>)input).size() > 0) {
	        Object obj = ((List<?>)input).get(0);
	        if (obj instanceof Baseline) {
	            return true;
	        }
	    }
		return false;
	}

	static class InlineComboEditingSupport extends EditingSupport {

		protected ComboBoxCellEditor editor;

		public InlineComboEditingSupport(ColumnViewer viewer) {
			super(viewer);
			this.editor = new ComboBoxCellEditor((Composite) viewer.getControl(), new String[] {}, SWT.READ_ONLY);

			Control control = editor.getControl();
			((CCombo) control).addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					editor.deactivate();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
		}

		@Override
        protected boolean canEdit(Object element) {
			if (element instanceof Info) {
				return true;
			}
            if (element instanceof Baseline) {
                return true;
            }
			return false;
		}

		@Override
        protected CellEditor getCellEditor(Object element) {

			Set<String> versions = new TreeSet<String>();
            if (element instanceof Baseline) {
                Baseline info = (Baseline) element;
                versions.add(info.getSuggestedVersion().toString());
            }
			if (element instanceof Info) {
				Info info = (Info) element;
				versions.add(info.suggestedVersion.toString());
			}

			editor.setItems(versions.toArray(new String[versions.size()]));
			return editor;
		}

		@Override
        protected Object getValue(Object element) {
			return null;
			// Not needed
		}

		@Override
        protected void setValue(Object element, Object value) {
			// Not needed
		}

		@Override
        protected void initializeCellEditorValue(CellEditor cellEditor, ViewerCell cell) {

			 String selectedVersion = "";
			 if (cell.getElement() instanceof Baseline) {
			     selectedVersion = ((Baseline) cell.getElement()).getSuggestedVersion().toString();
			 } else if (cell.getElement() instanceof Info) {
                 selectedVersion = ((Info) cell.getElement()).suggestedVersion.toString();
			 }

			 String[] items = ((ComboBoxCellEditor)cellEditor).getItems();
			 int idx = 0;
			 for (int i = 0; i < items.length; i++) {
			     if (items[i].equals(selectedVersion)) {
			         idx = i;
			         break;
			     }
			 }

			 cellEditor.setValue(idx);
			 cell.setText(selectedVersion);
		}

		@Override
        protected void saveCellEditorValue(CellEditor cellEditor, ViewerCell cell) {
			int idx = ((Integer) cellEditor.getValue()).intValue();
			String[] items = ((ComboBoxCellEditor) cellEditor).getItems();

			String selectedVersion = items[idx];
			cell.setText(selectedVersion);

            if (cell.getElement() instanceof Baseline) {
                ((Baseline)cell.getElement()).setSuggestedVersion(Version.parseVersion(selectedVersion));
            } else if (cell.getElement() instanceof Info) {
			    ((Info)cell.getElement()).suggestedVersion = Version.parseVersion(selectedVersion);
			}
		}
	}
}
