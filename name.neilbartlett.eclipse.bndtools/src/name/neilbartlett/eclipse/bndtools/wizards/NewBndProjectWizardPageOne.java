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
package name.neilbartlett.eclipse.bndtools.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import aQute.bnd.classpath.BndContainerInitializer;

public class NewBndProjectWizardPageOne extends NewJavaProjectWizardPageOne {
	
	private static final String PATH_TEST_SRC = "test";
	private static final String PATH_TEST_BIN = "bin_test";

	NewBndProjectWizardPageOne() {
		setTitle("Create a Bnd OSGi Project");
		setDescription("Create a Bnd OSGi Project in the workspace or an external location.");
	}

	@Override
	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 * This has been cut and pasted from the superclass because we wish to customize the contents of the page.
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		final Composite composite= new Composite(parent, SWT.NULL);
		composite.setFont(parent.getFont());
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		// create UI elements
		Control nameControl= createNameControl(composite);
		nameControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control locationControl= createLocationControl(composite);
		locationControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control jreControl= createJRESelectionControl(composite);
		jreControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

//		Control layoutControl= createProjectLayoutControl(composite);
//		layoutControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control workingSetControl= createWorkingSetControl(composite);
		workingSetControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control infoControl= createInfoControl(composite);
		infoControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		setControl(composite);
	}
	
	@Override
	public IClasspathEntry[] getDefaultClasspathEntries() {
		IClasspathEntry[] entries = super.getDefaultClasspathEntries();
		List<IClasspathEntry> result = new ArrayList<IClasspathEntry>(entries.length + 2);
		result.addAll(Arrays.asList(entries));
		
		// Add the Bnd classpath container entry
		IPath bndContainerPath = BndContainerInitializer.ID;
		IClasspathEntry bndContainerEntry = JavaCore.newContainerEntry(bndContainerPath, false);
		result.add(bndContainerEntry);
		
		return (IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);
	}
	
	@Override
	public IClasspathEntry[] getSourceClasspathEntries() {
		IPath projectPath = new Path(getProjectName()).makeAbsolute();
		
		IClasspathEntry[] entries = super.getSourceClasspathEntries();
		IClasspathEntry[] newEntries = new IClasspathEntry[entries.length + 1];
		System.arraycopy(entries, 0, newEntries, 0, entries.length);
		
		newEntries[entries.length] = JavaCore.newSourceEntry(projectPath.append(PATH_TEST_SRC), null, projectPath.append(PATH_TEST_BIN));
		
		return newEntries;
	}
}
