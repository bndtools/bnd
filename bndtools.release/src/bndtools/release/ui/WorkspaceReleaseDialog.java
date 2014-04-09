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

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

import bndtools.release.Activator;
import bndtools.release.ProjectDiff;
import bndtools.release.ReleaseHelper;
import bndtools.release.ReleaseHelper.MacroInfo;
import bndtools.release.api.ReleaseOption;
import bndtools.release.nl.Messages;

public class WorkspaceReleaseDialog extends Dialog implements SelectionListener {

	private List<ProjectDiff> projectDiffs;
	private ProjectListControl projectListControl;
	private BundleTree bundleRelease;
	protected SashForm sashForm;

	private final boolean showMessage;
    private ReleaseOption releaseOption;

	public WorkspaceReleaseDialog(Shell parentShell, List<ProjectDiff> projectDiffs, boolean showMessage) {
		super(parentShell);
		super.setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
		this.projectDiffs = projectDiffs;
		this.showMessage = showMessage;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		GridData gridData = createFillGridData();

		/* take 80% of the application window or 800x500 if that's bigger */
		Point size = getParentShell().getSize();
		gridData.widthHint = Math.max(Double.valueOf(size.x * 0.8).intValue(), 800);
		gridData.heightHint = Math.max(Double.valueOf(size.y * 0.8).intValue(), 500);

	    sashForm = new SashForm(composite, SWT.HORIZONTAL);
	    sashForm.setLayout(createGridLayout());
	    sashForm.setLayoutData(gridData);
	    sashForm.setSashWidth(10);

	    Composite left = new Composite(sashForm, SWT.NONE);
	    left.setLayout(createGridLayout());
	    left.setLayoutData(createFillGridData());

        Composite right = new Composite(sashForm, SWT.NONE);
        right.setLayout(createGridLayout());
        right.setLayoutData(createFillGridData());

		String[] items = ReleaseHelper.getReleaseRepositories();

		projectListControl = new ProjectListControl(this, items);
		projectListControl.createControl(left);

		bundleRelease = new BundleTree(right);

		projectListControl.setInput(projectDiffs);

        sashForm.setWeights(new int[] { 40, 60 });

		return sashForm;
	}

	public void setSelected(int index) {

		ProjectDiff projectDiff = null;
		if (projectDiffs.size() > 0) {
			projectListControl.setSelected(index);
			projectDiff = projectDiffs.get(index);
		}

		if (projectDiff != null) {
			bundleRelease.setInput(projectDiff.getBaselines());
			bundleRelease.setVisible(true);
		} else {
			bundleRelease.setVisible(false);
		}

	}

	public void widgetSelected(SelectionEvent e) {

        if (e.item == null || e.item.isDisposed()) {
            return;
        }

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

			bundleRelease.setInput(projectDiff.getBaselines());
			bundleRelease.setVisible(true);
		} else {
			bundleRelease.setVisible(false);
		}

	}

    public void widgetDefaultSelected(SelectionEvent e) {
	}

    public ReleaseOption getReleaseOption() {
        return releaseOption;
    }

    public boolean isShowMessage() {
        return showMessage;
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

    @Override
    protected void okPressed() {
        if (bundleRelease.getReleaseOption() == null) {
            for (ProjectDiff diff : projectDiffs) {
                if (diff.isRelease()) {
                    Activator.message(Messages.releaseOptionMustBeSpecified);
                    return;
                }
            }
        }

        // Find bundles with macros as Bundle-Version:
        List<MacroInfo> bsns = ReleaseHelper.getBsnsWithBundleVersionMacro(projectDiffs);
        if (bsns.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(Messages.macrosWillBeOverwritten1);
            sb.append("\n\n");
            for (MacroInfo info : bsns) {
                sb.append("\t");
                sb.append(info.projectDiff.getProject().getName());
                if (!info.projectDiff.getProject().getName().equals(info.bsn)) {
                    sb.append(" (");
                    sb.append(info.bsn);
                    sb.append(")");
                }
                sb.append("    ");
                sb.append(info.macro);
                sb.append("\n");
            }
            sb.append("\n");
            sb.append(Messages.macrosWillBeOverwritten2);

            if (!Activator.confirmationMessage(sb.toString())) {
                return;
            }
        }

        this.releaseOption = bundleRelease.getReleaseOption();
        super.okPressed();
    }

    @Override
    public boolean close() {
        if (projectListControl != null) {
            projectListControl.dispose();
        }
        return super.close();
    }
}
