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

import name.neilbartlett.eclipse.bndtools.classpath.WorkspaceRepositoryClasspathContainerInitializer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;

public class NewBndProjectWizardPageOne extends NewJavaProjectWizardPageOne {
	
	private final NewBndProjectWizardFrameworkPage frameworkPage;

	NewBndProjectWizardPageOne(NewBndProjectWizardFrameworkPage frameworkPage) {
		this.frameworkPage = frameworkPage;
		setTitle("Create a Bnd OSGi Project");
		setDescription("Create a Bnd OSGi Project in the workspace or an external location.");
	}
	
	@Override
	public IClasspathEntry[] getDefaultClasspathEntries() {
		IClasspathEntry[] entries = super.getDefaultClasspathEntries();
		List<IClasspathEntry> result = new ArrayList<IClasspathEntry>(entries.length + 2);
		result.addAll(Arrays.asList(entries));
		
		// Add the framework
		IClasspathEntry frameworkEntry = frameworkPage.getFrameworkClasspathEntry();
		if(frameworkEntry != null) {
			result.add(frameworkEntry);
		}
		
		// Add the workspace bundle repository classpath container
		IPath bundleRepoContainerPath = new Path(WorkspaceRepositoryClasspathContainerInitializer.CONTAINER_ID);
		result.add(JavaCore.newContainerEntry(bundleRepoContainerPath, false));
		
		return (IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);
	}
}
