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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import aQute.bnd.build.Project;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.diff.JarDiff;
import bndtools.release.nl.Messages;

public class BundleReleaseDialog extends Dialog {

	private BundleRelease release;
	private Project project;
	private List<JarDiff> diffs;
	private Combo combo;
	
	public BundleReleaseDialog(Shell parentShell, Project project, List<JarDiff> compare) {
		super(parentShell);
		super.setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
		this.project = project;
		release = new BundleRelease(compare);
		this.diffs = compare;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		Composite c2 = new Composite(composite, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 5;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 10;
		c2.setLayout(gridLayout);
		c2.setLayoutData(new GridData(SWT.HORIZONTAL, SWT.VERTICAL, true, true));
		
		Label label = new Label(c2, SWT.NONE);
		label.setText(Messages.releaseToRepo);
		
		String[] items = getRepositories();
		String defaultRepo = project.getProperty("-releaserepo");
		int idx = 0;
		for (int i = 0; i < items.length; i++) {
			if (defaultRepo != null) {
				if (items[i].equals(defaultRepo)) {
					idx = i;
					break;
				}
			}
		}
		
		combo = new Combo (c2, SWT.READ_ONLY);
		//combo.setLayout(gridLayout);
		combo.setItems (items);
		combo.setSize (200, 200);
		combo.setText(items[idx]);
		
		ScrolledComposite scrolled = new ScrolledComposite(composite, SWT.H_SCROLL | SWT.V_SCROLL);

		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 5;
		gridLayout.marginWidth = 10;
		gridLayout.marginHeight = 10;

		scrolled.setLayout(gridLayout);
		scrolled.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		release.createControl(scrolled);

		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		scrolled.setContent(release.getControl());
		scrolled.setMinSize(500, 500);
		scrolled.layout(true);

		return composite;
	}
	
	private String[] getRepositories() {
		List<RepositoryPlugin> repos = project.getWorkspace().getPlugins(RepositoryPlugin.class);
		Set<String> ret = new TreeSet<String>();
		for (RepositoryPlugin repo : repos) {
			if (repo.canWrite()) {
				if (repo.getName() != null) {
					ret.add(repo.getName());
				} else {
					ret.add(repo.toString());
				}
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	@Override
	protected void okPressed() {
		String repository = combo.getText();
		
		ReleaseJob job = new ReleaseJob(project, diffs, repository);
		job.schedule();
		
		super.okPressed();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.releaseDialogTitle);
	}
}
