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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Widget;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.version.Version;
import bndtools.release.Activator;
import bndtools.release.api.ReleaseOption;
import bndtools.release.nl.Messages;

public class BundleTree extends Composite {

	public final static String VERSION_STRING = "(\\d+)(\\.(\\d+)(\\.(\\d+)(\\.([-_\\da-zA-Z]+))?)?)?"; //$NON-NLS-1$
	public final static Pattern VERSION = Pattern.compile(VERSION_STRING);

	protected SashForm sashForm;
	protected Button showAll;
	protected Combo options;

	protected TreeViewer infoViewer;
	protected Composite infoViewerComposite;

	protected TreeViewer bundleTreeViewer;
	protected Composite bundleTreeViewerComposite;

	protected TreeContentProvider bundleTreeViewerProvider = new TreeContentProvider();
	protected InfoContentProvider infoTreeViewerProvider = new InfoContentProvider();

	private Map<Object, Version> initialSuggested;

	public BundleTree(Composite composite) {
		this(composite, SWT.NONE);
	}

	public BundleTree(Composite composite, int style) {
		super(composite, style);
		createControl();
	}

	public void createControl() {

	    setLayout(createGridLayout());
	    setLayoutData(createFillGridData());

		sashForm = new SashForm(this, SWT.VERTICAL);
		sashForm.setLayout(createGridLayout());
        sashForm.setLayoutData(createFillGridData());
		sashForm.setSashWidth(10);

		createInfoViewer(sashForm);

		Composite composite = new Composite(sashForm, SWT.NONE);
		composite.setLayout(createGridLayout());
		composite.setLayoutData(createFillGridData());

		createBundleTreeViewer(composite);

		createButtons(composite);

		sashForm.setWeights(new int[] { 30, 70 });
	}

	private void createInfoViewer(Composite container) {

		infoViewerComposite = new Composite(container, SWT.NONE);
		infoViewerComposite.setLayoutData(createFillGridData());

		TreeColumnLayout layout = new TreeColumnLayout();
		infoViewerComposite.setLayout(layout);

		infoViewer = new TreeViewer(infoViewerComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		infoViewer.setUseHashlookup(true);
		infoViewer.getTree().setHeaderVisible(true);

		TreeViewerColumn treeViewerColumn = new TreeViewerColumn(infoViewer, SWT.NONE);
        TreeColumn treeColumn = treeViewerColumn.getColumn();
        layout.setColumnData(treeColumn, new ColumnWeightData(450, 180, true));
        treeColumn.setText(Messages.bundleAndPackageName);
        treeViewerColumn.setLabelProvider(new InfoLabelProvider());

        treeViewerColumn = new TreeViewerColumn(infoViewer, SWT.NONE);
        treeColumn = treeViewerColumn.getColumn();
        layout.setColumnData(treeColumn, new ColumnWeightData(80, 80, true));
        treeColumn.setText(Messages.version2);
        treeViewerColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof Baseline) {
                    return ((Baseline) element).getOlderVersion().getWithoutQualifier().toString();
                }
                if (element instanceof Info) {
                    return ((Info) element).olderVersion.toString();
                }
                return ""; //$NON-NLS-1$
            }
        });

        treeViewerColumn = new TreeViewerColumn(infoViewer, SWT.NONE);
        treeColumn = treeViewerColumn.getColumn();
        layout.setColumnData(treeColumn, new ColumnWeightData(80, 80, true));
        treeColumn.setText(Messages.newVersion);
        treeViewerColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof Baseline) {
                    return ((Baseline) element).getSuggestedVersion().toString();
                }
                if (element instanceof Info) {
                    return ((Info) element).suggestedVersion != null ? ((Info) element).suggestedVersion.toString() : ""; //$NON-NLS-1$
                }
                return ""; //$NON-NLS-1$
            }
        });
        treeViewerColumn.setEditingSupport(new InlineComboEditingSupport(infoViewer));

		infoViewer.setContentProvider(infoTreeViewerProvider);
		infoViewer.setAutoExpandLevel(2);

	}

	private void createBundleTreeViewer(Composite container) {

		bundleTreeViewerComposite = new Composite(container, SWT.NONE);
		bundleTreeViewerComposite.setLayoutData(createFillGridData());

		TreeColumnLayout layout = new TreeColumnLayout();
		bundleTreeViewerComposite.setLayout(layout);

		bundleTreeViewer = new TreeViewer(bundleTreeViewerComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		bundleTreeViewer.setUseHashlookup(true);
		bundleTreeViewer.getTree().setHeaderVisible(true);

		TreeViewerColumn treeViewerColumn = new TreeViewerColumn(bundleTreeViewer, SWT.NONE);
		TreeColumn treeColumn = treeViewerColumn.getColumn();
		layout.setColumnData(treeColumn, new ColumnWeightData(100, 340, true));
		treeColumn.setText(Messages.symbNameResources);
		treeViewerColumn.setLabelProvider(new TreeLabelProvider());

		bundleTreeViewer.setContentProvider(bundleTreeViewerProvider);
		bundleTreeViewer.setAutoExpandLevel(3);
	}

	private void createButtons(Composite parent) {

	    GridLayout gridLayout = createGridLayout();
	    gridLayout.numColumns = 2;

	    Composite composite = new Composite(parent, SWT.NONE);
	    composite.setLayout(gridLayout);
	    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	    gridData.grabExcessHorizontalSpace = true;
	    composite.setLayoutData(gridData);

		showAll = new Button(composite, SWT.CHECK);
		showAll.setText(Messages.showAllPackages);
		showAll.setFont(getFont());
		showAll.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent event) {
				Widget widget = event.widget;
				if (widget == showAll) {

					bundleTreeViewerProvider.setShowAll(!bundleTreeViewerProvider.isShowAll());
					infoTreeViewerProvider.setShowAll(!infoTreeViewerProvider.isShowAll());

					if (bundleTreeViewer.getInput() != null || infoViewer.getInput() != null) {
    					bundleTreeViewer.setSelection(null, false);
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
			}
		});

		Composite dropdown = new Composite(composite, SWT.NONE);
		gridLayout = createGridLayout();
		gridLayout.numColumns = 2;
		dropdown.setLayout(gridLayout);
        gridData = createFillGridData();
        gridData.grabExcessVerticalSpace = false;
        gridData.horizontalAlignment = SWT.RIGHT;
        gridData.grabExcessHorizontalSpace = true;
	    dropdown.setLayoutData(gridData);

	    Label label = new Label(dropdown, SWT.NONE);
	    label.setText(Messages.releaseOption);

		options = new Combo(dropdown, SWT.DROP_DOWN | SWT.READ_ONLY);
		String items[] = { Messages.updateVersionsAndRelease, Messages.updateVersions, Messages.release };
		options.setItems(items);
		options.add(Messages.comboSelectText, 0);
		options.select(0);

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

    protected Version getInitialSuggestedVersion(Object obj) {
        if (initialSuggested == null) {
            initialSuggested = new HashMap<Object,Version>();
        }

        Version version = initialSuggested.get(obj);
        if (version != null) {
            return version;
        }

        if (obj instanceof Info) {
            version = ((Info) obj).suggestedVersion;
        } else {
            version = ((Baseline) obj).getSuggestedVersion();
        }
        initialSuggested.put(obj, version);
        return version;
    }

	class InlineComboEditingSupport extends EditingSupport {

		protected ComboBoxCellEditor editor;

		public InlineComboEditingSupport(ColumnViewer viewer) {
			super(viewer);
			this.editor = new ComboBoxCellEditor((Composite) viewer.getControl(), new String[] {});

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
                versions.add(getInitialSuggestedVersion(info).toString());
                if (getInitialSuggestedVersion(info).compareTo(Version.ONE) < 0) {
                    versions.add(Version.ONE.toString());
                }
            }
			if (element instanceof Info) {
				Info info = (Info) element;
				versions.add(getInitialSuggestedVersion(info).toString());
                if (getInitialSuggestedVersion(info).compareTo(Version.ONE) < 0) {
                    versions.add(Version.ONE.toString());
                }
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

			 String selectedVersion = ""; //$NON-NLS-1$
			 if (cell.getElement() instanceof Baseline) {
			     selectedVersion = ((Baseline) cell.getElement()).getSuggestedVersion().toString();
			 } else if (cell.getElement() instanceof Info) {
                 selectedVersion = ((Info) cell.getElement()).suggestedVersion.toString();
			 }

			 String[] items = ((ComboBoxCellEditor)cellEditor).getItems();
			 int idx = -1;
			 for (int i = 0; i < items.length; i++) {
			     if (items[i].equals(selectedVersion)) {
			         idx = i;
			         break;
			     }
			 }
			 if (idx > -1)
			     cellEditor.setValue(idx);
			 cell.setText(selectedVersion);
		}

		@Override
        protected void saveCellEditorValue(CellEditor cellEditor, ViewerCell cell) {
			int idx = ((Integer) cellEditor.getValue()).intValue();
			String[] items = ((ComboBoxCellEditor) cellEditor).getItems();

			String selectedVersion;
			if (idx > -1) {
			    selectedVersion = items[idx];
			} else {
			    selectedVersion = ((CCombo) cellEditor.getControl()).getText();
			}

			Version version;
			try {
			    version = Version.parseVersion(selectedVersion);
			} catch (IllegalArgumentException e) {
			    Activator.message(String.format(Messages.versionInvalid, selectedVersion));
			    return;
			}

			cell.setText(selectedVersion);

            if (cell.getElement() instanceof Baseline) {
                ((Baseline)cell.getElement()).setSuggestedVersion(version);
            } else if (cell.getElement() instanceof Info) {
			    ((Info)cell.getElement()).suggestedVersion = version;
			}
		}
	}

	public ReleaseOption getReleaseOption() {
	    return ReleaseOption.parse(options.getText());
	}
    private static GridLayout createGridLayout() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        return gridLayout;
    }

    private static GridData createFillGridData() {
        return new GridData(GridData.FILL, GridData.FILL, true, true);
    }
}
