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
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
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
		calculateClasspathsForProject(javaProject, false);
	}
	private void calculateClasspathsForProject(IJavaProject currentProject, boolean exportedOnly) throws JavaModelException {
		classpathLocations.add(currentProject.getOutputLocation().makeRelative());
		
		LinkedList<IClasspathEntry[]> stack = new LinkedList<IClasspathEntry[]>();
		stack.add(currentProject.getRawClasspath());
		
		while(!stack.isEmpty()) {
			IClasspathEntry[] batch = stack.remove(0);
			for (IClasspathEntry entry : batch) {
				if(exportedOnly && !entry.isExported())
					continue;
				
				switch (entry.getEntryKind()) {
				case IClasspathEntry.CPE_SOURCE:
					sourceLocations.add(entry.getPath().makeRelative());
					IPath outputLocation = entry.getOutputLocation();
					if(outputLocation != null)
						classpathLocations.add(outputLocation.makeRelative());
					break;
				case IClasspathEntry.CPE_LIBRARY:
					IPath path = entry.getPath();
					IResource member = currentProject.getProject().getWorkspace().getRoot().findMember(path);
					if(member != null) {
						classpathLocations.add(path.makeRelative());
					} else {
						classpathLocations.add(path);
					}
					break;
				case IClasspathEntry.CPE_VARIABLE:
					IClasspathEntry resolvedEntry = JavaCore.getResolvedClasspathEntry(entry);
					stack.add(0, new IClasspathEntry[] { resolvedEntry });
					break;
				case IClasspathEntry.CPE_CONTAINER:
					IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), currentProject);
					IClasspathEntry[] containerEntries = container.getClasspathEntries();
					stack.add(0, containerEntries);
					break;
				case IClasspathEntry.CPE_PROJECT:
					IPath projectPath = entry.getPath();
					IProject referencedProject = javaProject.getProject().getWorkspace().getRoot().getProject(projectPath.segment(0));
					calculateClasspathsForProject(JavaCore.create(referencedProject), true);
					break;
				default:
					break;
				}
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
