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
package bndtools.release;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import bndtools.release.nl.Messages;

public class ProjectListControl {

	private Table projects;
	private String[] releaseRepos;
	private TableViewer tableViewer;
	private final SelectionListener selectionListener;
	private List<ProjectDiff> projectDiffs;

    private static final String PROJECT_COLUMN = "project";
    private static final String REPOSITORY_COLUMN = "repository";
    private static final String BUNDLES_COLUMN = "bundles";

    // Set column names
    private static final String[] columnNames = new String[] {
            PROJECT_COLUMN,
            REPOSITORY_COLUMN,
            BUNDLES_COLUMN
            };

	public ProjectListControl(SelectionListener selectionListener, String[] releaseRepos) {
		this.selectionListener = selectionListener;
		this.releaseRepos = releaseRepos;
	}

	public void createControl(Composite parent) {
		createTable(parent);
		createTableViewer();
	}

	private void createTable(Composite parent) {

	    projects = new Table (parent, SWT.CHECK | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
		projects.setLinesVisible (true);
		projects.setHeaderVisible (true);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 300;
		projects.setLayoutData (gridData);
		projects.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				selectionListener.widgetSelected(e);
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				selectionListener.widgetDefaultSelected(e);
			}
		});

		// Project
		TableColumn tableCol = new TableColumn(projects, SWT.LEFT, 0);
		tableCol.setText(Messages.project1);
		tableCol.setWidth(200);

		// Repository
		tableCol = new TableColumn(projects, SWT.LEFT, 1);
		tableCol.setText(Messages.repository);
		tableCol.setWidth(100);

		// Number of Bundles
		tableCol = new TableColumn(projects, SWT.CENTER, 2);
		tableCol.setText(Messages.bundles);
		tableCol.setWidth(50);
	}

    private void createTableViewer() {

        tableViewer = new TableViewer(projects);
        tableViewer.setUseHashlookup(true);

        tableViewer.setColumnProperties(columnNames);

        // Create the cell editors
        CellEditor[] editors = new CellEditor[columnNames.length];

        // Column 1 : Project
        TextCellEditor textEditor = new TextCellEditor(projects);
        ((Text) textEditor.getControl()).setEditable(false);
        editors[0] = textEditor;

        // Column 2 : Repository (Combo Box)
        editors[1] = new ComboBoxCellEditor(projects, releaseRepos, SWT.READ_ONLY);

        // Column 3 : Number of bundles
        textEditor = new TextCellEditor(projects);
        ((Text) textEditor.getControl()).setEditable(false);
        editors[2] = textEditor;

        // Assign the cell editors to the viewer
        tableViewer.setCellEditors(editors);
        tableViewer.setCellModifier(new TableCellModifier());
        tableViewer.setContentProvider(new ContentProvider());
        tableViewer.setLabelProvider(new TableLabelProvider());
    }


    public void setInput(List<ProjectDiff> projectDiffs) {
        this.projectDiffs = projectDiffs;
        tableViewer.setInput(projectDiffs);
        for (TableItem tableItem : tableViewer.getTable().getItems()) {
            tableItem.setChecked(true);
        }
    }

	public Table getTable() {
		return projects;
	}

	public void setSelected(int index) {
		projects.select(index);
	}

	private class ContentProvider implements IStructuredContentProvider {

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

        }

        public Object[] getElements(Object parent) {
            return projectDiffs.toArray();
        }
	}

	private class TableLabelProvider extends LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            String text = "";
            ProjectDiff diff = (ProjectDiff) element;
            switch (columnIndex) {
            case 0:
                text = diff.getProject().getName();
                break;
            case 1:
                text = diff.getReleaseRepository();
                if (text == null) {
                    text = diff.getDefaultReleaseRepository();
                }
                break;
            case 2:
              int bundles = -1;
              try {
                  bundles = diff.getProject().getSubBuilders().size();
              } catch (Exception e) {
                  /* ignore */
              }
              text = String.valueOf(bundles);
              break;
            default:
                break;
            }
            return text;
        }
	}

	private class TableCellModifier implements ICellModifier {

	    private List<String> names = Arrays.asList(columnNames);

        public boolean canModify(Object element, String property) {
            // Find the index of the column
            int columnIndex = names.indexOf(property);
            if (columnIndex == 1)
                return true;
            return false;
        }

        public Object getValue(Object element, String property) {
            int columnIndex = names.indexOf(property);
            Object result = null;
            ProjectDiff diff = (ProjectDiff) element;
            switch (columnIndex) {
            case 0:
                result = diff.getProject().getName();
                break;
            case 1:
                String stringValue = diff.getReleaseRepository();
                if (stringValue == null)
                    stringValue = diff.getDefaultReleaseRepository();
                int i = releaseRepos.length - 1;
                while (!stringValue.equals(releaseRepos[i]) && i > 0)
                    --i;
                result = new Integer(i);
                break;
            case 2:
              int bundles = -1;
              try {
                  bundles = diff.getProject().getSubBuilders().size();
              } catch (Exception e) {
                  /* ignore */
              }
              result = String.valueOf(bundles);
              break;
            default:
                break;
            }
            return result;
        }

        public void modify(Object element, String property, Object value) {
            // Find the index of the column
            int columnIndex = names.indexOf(property);

            TableItem item = (TableItem) element;
            ProjectDiff diff = (ProjectDiff) item.getData();
            String valueString;

            switch (columnIndex) {
                case 1 : // Repository
                    if (((Integer) value).intValue() < 0)
                        break;
                    valueString = releaseRepos[((Integer) value).intValue()];
                    if (diff.getReleaseRepository() == null || !diff.getReleaseRepository().equals(valueString)) {
                        diff.setReleaseRepository(valueString);
                    }
                    item.setText(1, valueString);
                    break;
                default :
                }
        }
	}
}
