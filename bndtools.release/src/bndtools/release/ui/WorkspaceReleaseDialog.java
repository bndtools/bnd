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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

import bndtools.release.ProjectDiff;
import bndtools.release.ProjectListControl;
import bndtools.release.ReleaseHelper;
import bndtools.release.nl.Messages;

public class WorkspaceReleaseDialog extends Dialog implements SelectionListener {

    private static final int UPDATE_RELEASE_BUTTON = IDialogConstants.CLIENT_ID + 1;
    private static final int UPDATE_BUTTON = IDialogConstants.CLIENT_ID + 3;
    private static final int CANCEL_BUTTON = IDialogConstants.CLIENT_ID + 2;

	private List<ProjectDiff> projectDiffs;
	private ProjectListControl projectListControl;
	private BundleTree bundleRelease;

	private boolean updateOnly = false;

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

		bundleRelease = new BundleTree(composite);

		projectListControl.setInput(projectDiffs);
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

   @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, UPDATE_RELEASE_BUTTON, Messages.updateVersionsAndRelease, true);
        createButton(parent, UPDATE_BUTTON, Messages.updateVersions, false);
        createButton(parent, CANCEL_BUTTON, IDialogConstants.CANCEL_LABEL, false);
    }

   @Override
   protected void buttonPressed(int buttonId) {
       if (CANCEL_BUTTON == buttonId) {
           cancelPressed();
           return;
       }

       if (UPDATE_BUTTON == buttonId) {
           updateOnly = true;
       }
       super.okPressed();
   }


	public void widgetDefaultSelected(SelectionEvent e) {
	}

    public boolean isUpdateOnly() {
        return updateOnly;
    }
}
