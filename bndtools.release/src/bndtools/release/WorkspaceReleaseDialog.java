package bndtools.release;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

public class WorkspaceReleaseDialog extends Dialog implements SelectionListener {

	private List<ProjectDiff> projectDiffs;
	private ProjectListControl projectListControl;
	private BundleRelease bundleRelease;
	
	public WorkspaceReleaseDialog(Shell parentShell, List<ProjectDiff> projectDiffs) {
		super(parentShell);
		super.setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
		this.projectDiffs = projectDiffs;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 5;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		composite.setLayout(gridLayout);
		
		String[] items = ReleaseHelper.getReleaseRepositories();

		projectListControl = new ProjectListControl(this, items);
		projectListControl.createControl(composite);
		
		bundleRelease = new BundleRelease();
		bundleRelease.createControl(composite);

		for (ProjectDiff projectDiff : projectDiffs) {
			projectListControl.addItemToTable(projectDiff);
		}
		setSelected(0);
		
		return composite;
	}

	public void setSelected(int index) {
		
		ProjectDiff projectDiff = null;
		if (projectDiffs.size() > 0) {
			projectListControl.setSelected(index);
			projectDiff = projectDiffs.get(index);
		}
		
		if (projectDiff != null) {
			bundleRelease.setInput(projectDiff.getJarDiffs());
			bundleRelease.getControl().setVisible(true);
		} else {
			bundleRelease.getControl().setVisible(false);
		}

	}

	public void widgetSelected(SelectionEvent e) {
	
		ProjectDiff projectDiff = (ProjectDiff) ((TableItem) e.item).getData();
		if (projectDiff != null) {
			if (e.detail > 0) {
				int checkedIndex = -1;
				TableItem[] items = projectListControl.getTable().getItems();
				for (int i = 0; i < items.length; i++) {
					if (items[i] == e.item) {
						checkedIndex = i;
						break;
					}
				}
				TableItem ti = projectListControl.getTable().getItem(checkedIndex);
				boolean checked = ti.getChecked();
				projectDiff.setRelease(checked);
				
				if (checkedIndex > -1) {
					projectListControl.getTable().deselectAll();
					projectListControl.setSelected(checkedIndex);
				}
			}
			
			bundleRelease.setInput(projectDiff.getJarDiffs());
			bundleRelease.getControl().setVisible(true);
		} else {
			bundleRelease.getControl().setVisible(false);
		}
		
	}
	
	public void widgetDefaultSelected(SelectionEvent e) {
	}
}
