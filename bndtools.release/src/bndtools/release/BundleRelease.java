/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release;

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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

import bndtools.diff.ClassInfo;
import bndtools.diff.FieldInfo;
import bndtools.diff.JarDiff;
import bndtools.diff.MethodInfo;
import bndtools.diff.PackageInfo;
import bndtools.release.nl.Messages;

public class BundleRelease {

	public final static String VERSION_STRING = "(\\d+)(\\.(\\d+)(\\.(\\d+)(\\.([-_\\da-zA-Z]+))?)?)?";
	public final static Pattern VERSION = Pattern.compile(VERSION_STRING);

	private List<JarDiff> diffs;

	private Composite container;

	protected TreeViewer treeViewer;

	protected TreeContentProvider treeProvider = new TreeContentProvider();

	public BundleRelease() {
	}

	public BundleRelease(List<JarDiff> diffs) {
		this.diffs = diffs;
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		container.setLayout(gridLayout);
		container.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
				true, true));

		Composite comp = new Composite(container, SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		comp.setLayout(gridLayout);
		comp.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
				true));

		createTreeViewer(comp);
		createButtons(container, comp);
		createInput();
	}

	private void createTreeViewer(Composite container) {

		treeViewer = new TreeViewer(container, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER | SWT.FULL_SELECTION);
		GridData gd = new GridData(GridData.FILL, GridData.FILL, true, true);
		// gd.grabExcessHorizontalSpace = true;
		gd.widthHint = 800;
		gd.heightHint = 550;
		treeViewer.getTree().setLayoutData(gd);
		treeViewer.getTree().setHeaderVisible(true);

		TreeViewerColumn treeColumn = new TreeViewerColumn(treeViewer, SWT.NONE);
		treeColumn.getColumn().setText(Messages.symbNamePackage);
		treeColumn.getColumn().setWidth(450);
		treeColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof JarDiff) {
					return ((JarDiff) element).getSymbolicName();
				}
				if (element instanceof PackageInfo) {
					return ((PackageInfo) element).getPackageName();
				}
				if (element instanceof ClassInfo) {
					String name = ((ClassInfo) element).getName();
					int idx = name.lastIndexOf('/');
					if (idx > -1) {
						name = name.substring(idx + 1);
					}
					return name;
				}
				if (element instanceof MethodInfo) {
					return ((MethodInfo) element).getName() + ((MethodInfo) element).getDesc();
				}
				if (element instanceof FieldInfo) {
					return ((FieldInfo) element).getName()+ "   (" + ((FieldInfo) element).getDesc() + ")";
				}
				return "";
			}

			public Image getImage(Object element) {

				if (element instanceof JarDiff) {
					return Activator.getDefault().getImageRegistry().get("bundle");
				}
				if (element instanceof PackageInfo) {
					PackageInfo pi = (PackageInfo) element;
					String baseImageKey = "package";
					if (pi.isExported()) {
						 baseImageKey = "package_export";
					}
					if (pi.isImported()) {
						 baseImageKey = "package_import";
					}
					if (pi.isImported() && pi.isExported()) {
						baseImageKey = "package_import_export";
					}
					String overlayKey = null;
					switch (pi.getChangeCode()) {
					case PackageInfo.CHANGE_CODE_NEW : {
						switch (pi.getSeverity()) {
						case JarDiff.PKG_SEVERITY_MAJOR : {
							overlayKey = "major_add";
							break;
						}
						case JarDiff.PKG_SEVERITY_MINOR : {
							overlayKey = "minor_add";
							break;
						}
						default:
							break;
						}
						break;
					}
					case PackageInfo.CHANGE_CODE_REMOVED : {
						switch (pi.getSeverity()) {
						case JarDiff.PKG_SEVERITY_MAJOR : {
							overlayKey = "major_remove";
							break;
						}
						case JarDiff.PKG_SEVERITY_MINOR : {
							overlayKey = "minor_remove";
							break;
						}
						case JarDiff.PKG_SEVERITY_MICRO : {
							overlayKey = "micro_remove";
							break;
						}
						default:
							break;
						}
						break;
					}
					case PackageInfo.CHANGE_CODE_MODIFIED : {
						switch (pi.getSeverity()) {
						case JarDiff.PKG_SEVERITY_MAJOR : {
							overlayKey = "major_modify";
							break;
						}
						case JarDiff.PKG_SEVERITY_MINOR : {
							overlayKey = "minor_modify";
							break;
						}
						default:
							break;
						}
						break;
					}
					case PackageInfo.CHANGE_CODE_VERSION_MISSING : {
						overlayKey = "micro_modify";
						break;
					}
					}
					if (overlayKey != null) {
						return Activator.getDefault().getImageRegistry().get(baseImageKey + "_" + overlayKey);
					}
					return Activator.getDefault().getImageRegistry().get(baseImageKey);
				}

				if (element instanceof ClassInfo) {
					ClassInfo ci = (ClassInfo) element;
					PackageInfo pi = ci.getPackageInfo();
					String baseImageKey = "class";
					String overlayKey = null;
					switch (ci.getChangeCode()) {
					case ClassInfo.CHANGE_CODE_NEW : {
						switch (pi.getSeverity()) {
						case JarDiff.PKG_SEVERITY_MAJOR : {
							overlayKey = "major_add";
							break;
						}
						case JarDiff.PKG_SEVERITY_MINOR : {
							overlayKey = "minor_add";
							break;
						}
						default:
							break;
						}
						break;
					}
					case ClassInfo.CHANGE_CODE_REMOVED : {
						switch (pi.getSeverity()) {
						case JarDiff.PKG_SEVERITY_MAJOR : {
							overlayKey = "major_remove";
							break;
						}
						case JarDiff.PKG_SEVERITY_MINOR : {
							overlayKey = "minor_remove";
							break;
						}
						default:
							break;
						}
						break;
					}
					case ClassInfo.CHANGE_CODE_MODIFIED : {
						switch (pi.getSeverity()) {
						case JarDiff.PKG_SEVERITY_MAJOR : {
							overlayKey = "major_modify";
							break;
						}
						case JarDiff.PKG_SEVERITY_MINOR : {
							overlayKey = "minor_modify";
							break;
						}
						default:
							break;
						}
						break;
					}
					}
					if (overlayKey != null) {
						return Activator.getDefault().getImageRegistry().get(baseImageKey + "_" + overlayKey);
					}
					return Activator.getDefault().getImageRegistry().get(baseImageKey);
				}

				if (element instanceof MethodInfo) {
					MethodInfo ci = (MethodInfo) element;
					PackageInfo pi = ci.getPackageInfo();
					
					String baseImageKey = "method";
					if (ci.isStatic()) {
						baseImageKey = "static_method";
					}
					String overlayKey = null;
					switch (ci.getChangeCode()) {
					case MethodInfo.CHANGE_NEW : {
						switch (pi.getSeverity()) {
						case JarDiff.PKG_SEVERITY_MAJOR : {
							overlayKey = "major_add";
							break;
						}
						case JarDiff.PKG_SEVERITY_MINOR : {
							overlayKey = "minor_add";
							break;
						}
						default:
							break;
						}
						break;
					}
					case MethodInfo.CHANGE_REMOVED : {
						switch (pi.getSeverity()) {
						case JarDiff.PKG_SEVERITY_MAJOR : {
							overlayKey = "major_remove";
							break;
						}
						case JarDiff.PKG_SEVERITY_MINOR : {
							overlayKey = "minor_remove";
							break;
						}
						default:
							break;
						}
						break;
					}
					}
					if (overlayKey != null) {
						return Activator.getDefault().getImageRegistry().get(baseImageKey + "_" + overlayKey);
					}
					return Activator.getDefault().getImageRegistry().get(baseImageKey);
				}
				if (element instanceof FieldInfo) {
					FieldInfo ci = (FieldInfo) element;
					PackageInfo pi = ci.getPackageInfo();
					
					String baseImageKey = "field";
					if (ci.isStatic()) {
						baseImageKey = "static_field";
					}
					String overlayKey = null;
					switch (ci.getChangeCode()) {
					case FieldInfo.CHANGE_NEW : {
						switch (pi.getSeverity()) {
						case JarDiff.PKG_SEVERITY_MAJOR : {
							overlayKey = "major_add";
							break;
						}
						case JarDiff.PKG_SEVERITY_MINOR : {
							overlayKey = "minor_add";
							break;
						}
						default:
							break;
						}
						break;
					}
					case FieldInfo.CHANGE_REMOVED : {
						switch (pi.getSeverity()) {
						case JarDiff.PKG_SEVERITY_MAJOR : {
							overlayKey = "major_remove";
							break;
						}
						case JarDiff.PKG_SEVERITY_MINOR : {
							overlayKey = "minor_remove";
							break;
						}
						default:
							break;
						}
						break;
					}
					}
					if (overlayKey != null) {
						return Activator.getDefault().getImageRegistry().get(baseImageKey + "_" + overlayKey);
					}
					return Activator.getDefault().getImageRegistry().get(baseImageKey);
				}
				return null;
			}
		});

		TreeViewerColumn currentVersion = new TreeViewerColumn(treeViewer,
				SWT.NONE);
		currentVersion.getColumn().setText(Messages.version);
		currentVersion.getColumn().setWidth(80);
		currentVersion.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof JarDiff) {
					return ((JarDiff) element).getCurrentVersion();
				}
				if (element instanceof PackageInfo) {
					return ((PackageInfo) element).getCurrentVersion();
				}
				if (element instanceof ClassInfo) {
					return "";
				}
				if (element instanceof MethodInfo) {
					return "";
				}
				if (element instanceof FieldInfo) {
					return "";
				}
				return "";
			}

			public Image getImage(Object element) {
				return null;
			}
		});

		TreeViewerColumn suggestedVersion = new TreeViewerColumn(treeViewer,
				SWT.NONE);
		suggestedVersion.getColumn().setText(Messages.newVersion);
		suggestedVersion.getColumn().setWidth(80);
		suggestedVersion.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof JarDiff) {
					return ((JarDiff) element).getSuggestedVersion();
				}
				if (element instanceof PackageInfo) {
					return ((PackageInfo) element).getSuggestedVersion();
				}
				if (element instanceof ClassInfo) {
					return "";
				}
				if (element instanceof MethodInfo) {
					return "";
				}
				if (element instanceof FieldInfo) {
					return "";
				}
				return "";
			}

			public Image getImage(Object element) {
				return null;
			}
		});
		suggestedVersion.setEditingSupport(new InlineComboEditingSupport(treeViewer));

		
		
		TreeViewerColumn versionRange = new TreeViewerColumn(treeViewer,
				SWT.NONE);
		versionRange.getColumn().setText(Messages.versionRange);
		versionRange.getColumn().setWidth(100);
		versionRange.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof JarDiff) {
					return "";
				}
				if (element instanceof PackageInfo) {
					return ((PackageInfo) element).getVersionRange() == null ? "" : ((PackageInfo) element).getVersionRange();
				}
				if (element instanceof ClassInfo) {
					return "";
				}
				if (element instanceof MethodInfo) {
					return "";
				}
				if (element instanceof FieldInfo) {
					return "";
				}
				return "";
			}

			public Image getImage(Object element) {
				return null;
			}
		});

		TreeViewerColumn suggestedVersionRange = new TreeViewerColumn(treeViewer,
				SWT.NONE);
		suggestedVersionRange.getColumn().setText(Messages.newRange);
		suggestedVersionRange.getColumn().setWidth(100);
		suggestedVersionRange.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof JarDiff) {
					return "";
				}
				if (element instanceof PackageInfo) {
					return ((PackageInfo) element).getSuggestedVersionRange() == null ? "" : ((PackageInfo) element).getSuggestedVersionRange();
				}
				if (element instanceof ClassInfo) {
					return "";
				}
				if (element instanceof MethodInfo) {
					return "";
				}
				if (element instanceof FieldInfo) {
					return "";
				}
				return "";
			}

			public Image getImage(Object element) {
				return null;
			}
		});
		

		treeViewer.setContentProvider(treeProvider);
		treeViewer.setAutoExpandLevel(2);
	}

	private void createButtons(Composite container, Composite comp) {

		final Button showAll = new Button(comp, SWT.CHECK);
		showAll.setText(Messages.showAllPackages);
		showAll.setFont(container.getFont());
		GridData data = new GridData();
		data.grabExcessHorizontalSpace = true;
		data.widthHint = 824;
		data.horizontalIndent = -1;
		showAll.setLayoutData(data);
		showAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				Widget widget = event.widget;
				if (widget == showAll) {
					treeProvider.setShowAll(!treeProvider.isShowAll());
					treeViewer.setSelection(null, false);
					treeViewer.refresh();
				}
			}
		});
	}

	private void createInput() {
		if (diffs != null) { 
			treeViewer.setInput(diffs);
		}
	}

	public void setInput(List<JarDiff> diffs) {
		if (diffs == null) {
			treeViewer.setInput(Collections.emptyList());
			return;
		}
		treeViewer.setInput(diffs);
	}

	
	public Control getControl() {
		return container;
	}

	public void dispose() {
		container.dispose();
		container = null;
	}

	static class InlineComboEditingSupport extends EditingSupport {

		protected ComboBoxCellEditor editor;
		
		public InlineComboEditingSupport(ColumnViewer viewer) {
			super(viewer);
			this.editor = new ComboBoxCellEditor(
					(Composite) viewer.getControl(),
					new String[] {},
					SWT.READ_ONLY);
			
			Control control = editor.getControl();
			((CCombo) control).addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					editor.deactivate();
				}
				
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
		}

		protected boolean canEdit(Object element) {
			if (element instanceof JarDiff) {
				return true;
			}
			if (element instanceof PackageInfo) {
				PackageInfo pi = (PackageInfo) element;
				if (pi.isImported() && !pi.isExported()) {
					return false;
				}
				if (pi.getChangeCode() == PackageInfo.CHANGE_CODE_REMOVED) {
					return false;
				}
				if (pi.getChangeCode() == PackageInfo.CHANGE_CODE_VERSION_MISSING) {
					return true;
				}
				return pi.getSuggestedVersion() != null;
			}
			return false;
		}

		protected CellEditor getCellEditor(Object element) {
			
//			String suggestedVersion = null;
			
			Set<String> versions = new TreeSet<String>();
			if (element instanceof JarDiff) {
				JarDiff diff = (JarDiff) element;
				if (diff.getCurrentVersion() != null) {
					versions.add(diff.getCurrentVersion());
				}
				
				for (String suggestedVersion : diff.getSuggestedVersions()) {
					versions.add(suggestedVersion);
				}
				
			} else {
				PackageInfo pi = (PackageInfo) element;
	
				if (pi.getCurrentVersion() != null) {
					versions.add(pi.getCurrentVersion());
				}
				for (String suggestedVersion : pi.getSuggestedVersions()) {
					versions.add(suggestedVersion);
				}
				if (pi.getJarDiff().getCurrentVersion() != null) {
					versions.add(pi.getJarDiff().getCurrentVersion());
				}
				if (pi.getJarDiff().getSuggestedVersion() != null) {
					versions.add(pi.getJarDiff().getSuggestedVersion());
				}
				for (String suggestedVersion : pi.getJarDiff().getSuggestedVersions()) {
					versions.add(suggestedVersion);
				}
			}
			
//			String[] items = versions.toArray(new String[versions.size()]);
//			int idx = 0;
//			for (int i = 0; i < items.length; i++) {
//				if (items[i].equals(suggestedVersion)) {
//					idx = i;
//					break;
//				}
//			}
			editor.setItems(versions.toArray(new String[versions.size()]));
//			editor.setValue(idx);
			return editor;
		}

		protected Object getValue(Object element) {
			return null;
			//Not needed
		}

		protected void setValue(Object element, Object value) {
			//Not needed
		}

		protected void initializeCellEditorValue(CellEditor cellEditor, ViewerCell cell) {
			
			String selectedVersion;
			if (cell.getElement() instanceof JarDiff) {
				selectedVersion = ((JarDiff) cell.getElement()).getSelectedVersion();
			} else {
				PackageInfo pi = (PackageInfo) cell.getElement();
				selectedVersion = pi.getSelectedVersion();
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

		protected void saveCellEditorValue(CellEditor cellEditor, ViewerCell cell) {
			int idx = ((Integer)cellEditor.getValue()).intValue();
			String[] items = ((ComboBoxCellEditor)cellEditor).getItems();
			
			String selectedVersion = items[idx];
			cell.setText(selectedVersion);

			if (cell.getElement() instanceof JarDiff) {
				((JarDiff)cell.getElement()).setSelectedVersion(selectedVersion);
				return;
			}
			PackageInfo modelElement = (PackageInfo) cell.getElement();
			modelElement.setSelectedVersion(selectedVersion);
		}
	}
	
}
