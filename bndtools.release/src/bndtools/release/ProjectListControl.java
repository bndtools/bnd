package bndtools.release;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class ProjectListControl {

	private Table projects;
	private String[] releaseRepos;
	private final SelectionListener selectionListener;
	
	public ProjectListControl(SelectionListener selectionListener, String[] releaseRepos) {
		this.selectionListener = selectionListener;
		this.releaseRepos = releaseRepos;
	}

	public void createControl(Composite parent) {
		createTable(parent);
	}
	
	private void createTable(Composite parent) {
		
		projects = new Table (parent, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CHECK | SWT.FULL_SELECTION);
		projects.setLinesVisible (true);
		projects.setHeaderVisible (true);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 300;
		projects.setLayoutData (gridData);
		projects.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				selectionListener.widgetSelected(e);
				handleTableSelection(e);
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				selectionListener.widgetDefaultSelected(e);
			}
		});

		// Project
		TableColumn tableCol = new TableColumn(projects, SWT.NONE);
		tableCol.setText("Project");
		tableCol.setWidth(200);

		// Repository
		tableCol = new TableColumn(projects, SWT.NONE);
		tableCol.setText("Repository");
		tableCol.setWidth(100);

		// Number of Bundles
		tableCol = new TableColumn(projects, SWT.NONE);
		tableCol.setText("Bundles");
		tableCol.setAlignment(SWT.RIGHT);
		tableCol.setWidth(50);
	}

	protected void handleTableSelection(SelectionEvent e) {

	}
	
	public void addItemToTable(ProjectDiff projectDiff) {
		TableItem ti = new TableItem(projects, SWT.NONE, projects.getItemCount());
		ti.setChecked(projectDiff.isRelease());
		ti.setText(projectDiff.getProject().getName());

		ti.setData(projectDiff);
		
		TableEditor tEditor = new TableEditor(projects);
		CCombo combo = new CCombo(projects, SWT.NONE);
		combo.setItems(releaseRepos);
		
		if (projectDiff.getDefaultReleaseRepository() != null) {
			int index = combo.indexOf(projectDiff.getDefaultReleaseRepository());
			combo.select(index);
		} else {
			if (releaseRepos.length > 0) {
				int index = combo.indexOf(releaseRepos[0]);
				combo.select(index);
			}
		}
		
		tEditor.grabHorizontal = true;
		tEditor.setEditor(combo, ti, 1);

		int bundles = -1;
		try {
			bundles = projectDiff.getProject().getSubBuilders().size();
		} catch (Exception e) {
		}
		ti.setText(2, String.valueOf(bundles));
	}
	
	public Table getTable() {
		return projects;
	}
	
	public void setSelected(int index) {
		projects.select(index);
	}
}
