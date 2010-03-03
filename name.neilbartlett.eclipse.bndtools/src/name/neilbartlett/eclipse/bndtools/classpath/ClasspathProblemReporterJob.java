/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools.classpath;

import java.util.LinkedList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.builder.BndIncrementalBuilder;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CategorizedProblem;

class ClasspathProblemReporterJob extends WorkspaceJob {
	
	private final List<ResolutionProblem> problems = new LinkedList<ResolutionProblem>();
	private final IJavaProject javaProject;
	
	ClasspathProblemReporterJob(IJavaProject javaProject) {
		super("classpathProblemReporter");
		this.javaProject = javaProject;
	}
	
	public void addResolutionProblem(ResolutionProblem problem) {
		problems.add(problem);
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) {
		IProject project = javaProject.getProject();
		if(project.isOpen()) {
			try {
				project.deleteMarkers(BndIncrementalBuilder.MARKER_BND_CLASSPATH_PROBLEM, true, IResource.DEPTH_INFINITE);
			} catch (CoreException e) {
				Plugin.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error deleting Bnd Classpath Problem markers", e));
			}
			
			for (ResolutionProblem problem : problems) {
				try {
					createMissingDependencyMarker(problem);
				} catch (CoreException e) {
					Plugin.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error creating Bnd Classpath Problem marker", e));
				}
			}
		}
		return Status.OK_STATUS;
	}

	void createMissingDependencyMarker(ResolutionProblem problem) throws CoreException {
		BundleDependency dependency = problem.getDependency();
		StringBuilder messageBuilder = new StringBuilder();
		
		messageBuilder.append("Could not resolve bundle: ");
		messageBuilder.append(problem.getMessage());
		
		boolean cycle = false;
		
		List<RejectedExportCandidate> candidates = problem.getRejectedCandidates();
		for (RejectedExportCandidate candidate : candidates) {
			messageBuilder.append("\nRejected: ").append(candidate.getExportCandidate().getPath());
			messageBuilder.append(", Reason: ").append(candidate.getReason());
			cycle |= candidate.isCycle();
		}
		
		IMarker marker = javaProject.getProject().createMarker(BndIncrementalBuilder.MARKER_BND_CLASSPATH_PROBLEM);
		marker.setAttributes(
			new String[] {
				IMarker.MESSAGE,
				IMarker.SEVERITY,
				IMarker.LOCATION,
				IJavaModelMarker.CYCLE_DETECTED,
				IJavaModelMarker.CLASSPATH_FILE_FORMAT,
				IJavaModelMarker.ID,
				IJavaModelMarker.CATEGORY_ID,
			},
			new Object[] {
				messageBuilder.toString(),
				IMarker.SEVERITY_ERROR,
				"Build Path",
				cycle ? "true" : "false",//$NON-NLS-1$ //$NON-NLS-2$
				"false",//$NON-NLS-1$
				IJavaModelStatusConstants.CP_CONTAINER_PATH_UNBOUND,
				new Integer(CategorizedProblem.CAT_BUILDPATH),
			}
		);
	}
}
