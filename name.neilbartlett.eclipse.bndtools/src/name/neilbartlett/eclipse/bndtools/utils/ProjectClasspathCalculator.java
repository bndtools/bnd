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
package name.neilbartlett.eclipse.bndtools.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

public class ProjectClasspathCalculator extends AbstractClasspathCalculator {
	
	final IJavaProject javaProject;
	
	final List<IPath> classpathLocations;
	final List<IPath> sourceLocations;

	public ProjectClasspathCalculator(IJavaProject javaProject) throws JavaModelException {
		this.javaProject = javaProject;
		
		this.classpathLocations = new ArrayList<IPath>();
		this.sourceLocations = new ArrayList<IPath>(3);
		
		calculateClasspaths();
	}
	private void calculateClasspaths() throws JavaModelException {
		classpathLocations.add(javaProject.getOutputLocation().makeRelative());
		IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
		for (IClasspathEntry entry : classpathEntries) {
			switch (entry.getEntryKind()) {
			case IClasspathEntry.CPE_SOURCE:
				sourceLocations.add(entry.getPath().makeRelative());
				IPath outputLocation = entry.getOutputLocation();
				if(outputLocation != null)
					classpathLocations.add(outputLocation.makeRelative());
				break;
			case IClasspathEntry.CPE_LIBRARY:
				IPath path = entry.getPath();
				if(javaProject.getProject().getFullPath().isPrefixOf(path)) {
					classpathLocations.add(path.makeRelative());
				} else {
					classpathLocations.add(path);
				}
				break;
			default:
				break;
			}
		}
	}
	public List<IPath> classpathAsWorkspacePaths() {
		return Collections.unmodifiableList(classpathLocations);
	}
	public List<File> classpathAsFiles() {
		return pathsToFiles(javaProject.getProject().getWorkspace().getRoot(), classpathLocations);
	}
	public List<IPath> sourcepathAsWorkspacePaths() {
		return Collections.unmodifiableList(sourceLocations);
	}
	public List<File> sourcepathAsFiles() {
		return pathsToFiles(javaProject.getProject().getWorkspace().getRoot(), sourceLocations);
	}
}
