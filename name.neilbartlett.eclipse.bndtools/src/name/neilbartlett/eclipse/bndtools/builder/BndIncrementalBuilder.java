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
package name.neilbartlett.eclipse.bndtools.builder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.utils.ResourceDeltaAccumulator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Project;
import aQute.bnd.plugin.Activator;
import aQute.bnd.plugin.Central;
import aQute.lib.osgi.Builder;

public class BndIncrementalBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = Plugin.PLUGIN_ID + ".bndbuilder";
	public static final String MARKER_BND_PROBLEM = Plugin.PLUGIN_ID + ".bndproblem";
	public static final String MARKER_BND_CLASSPATH_PROBLEM = Plugin.PLUGIN_ID + ".bnd_classpath_problem";

	private static final String BND_SUFFIX = ".bnd";
	
	private static final long NEVER = -1;
	
	private final Map<String, Long> projectLastBuildTimes = new HashMap<String, Long>();
	private final Map<String, Collection<IPath>> projectBndFiles = new HashMap<String, Collection<IPath>>();
	
	@Override protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor)
			throws CoreException {
		IProject project = getProject();
		
		ensureBndBndExists(project);

		if (getLastBuildTime(project) == -1 || kind == FULL_BUILD) {
			rebuildBndProject(project, monitor);
		} else {
			IResourceDelta delta = getDelta(project);
			if(delta == null)
				rebuildBndProject(project, monitor);
			else
				incrementalRebuild(delta, project, monitor);
		}
		setLastBuildTime(project, System.currentTimeMillis());
		return new IProject[]{ project.getWorkspace().getRoot().getProject(Project.BNDCNF)};
	}
	private void setLastBuildTime(IProject project, long time) {
		projectLastBuildTimes.put(project.getName(), time);
	}
	private long getLastBuildTime(IProject project) {
		Long time = projectLastBuildTimes.get(project.getName());
		return time != null ? time.longValue() : NEVER;
	}
	Collection<IPath> enumerateBndFiles(IProject project) throws CoreException {
		final Collection<IPath> paths = new LinkedList<IPath>();
		project.accept(new IResourceProxyVisitor() {
			public boolean visit(IResourceProxy proxy) throws CoreException {
				if(proxy.getType() == IResource.FOLDER || proxy.getType() == IResource.PROJECT)
					return true;
				
				String name = proxy.getName();
				if(name.toLowerCase().endsWith(BND_SUFFIX)) {
					IPath path = proxy.requestFullPath();
					paths.add(path);
				}
				return false;
			}
		}, 0);
		return paths;
	}
	void ensureBndBndExists(IProject project) throws CoreException {
		IFile bndFile = project.getFile(Project.BNDFILE);
		if(!bndFile.exists()) {
			bndFile.create(new ByteArrayInputStream(new byte[0]), 0, null);
		}
	}
	@Override protected void clean(IProgressMonitor monitor)
			throws CoreException {
		// Clear markers
		getProject().deleteMarkers(MARKER_BND_PROBLEM, true,
				IResource.DEPTH_INFINITE);

		// Delete target files
		Project model = Activator.getDefault().getCentral().getModel(JavaCore.create(getProject()));
		try {
			model.clean();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error cleaning project outputs.", e));
		}
	}
	void incrementalRebuild(IResourceDelta delta, IProject project, IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor);
		Project model = Activator.getDefault().getCentral().getModel(JavaCore.create(project));
		// model.refresh();
		
		try {
			List<File> affectedFiles = new ArrayList<File>();
			ResourceDeltaAccumulator visitor = new ResourceDeltaAccumulator(IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED, affectedFiles);
			delta.accept(visitor);
			
			progress.setWorkRemaining(affectedFiles.size() + 10);
			
			boolean rebuild = false;
			
			// Check if any affected file is a bnd file
			for (File file : affectedFiles) {
				if(file.getName().toLowerCase().endsWith(BND_SUFFIX)) {
					rebuild = true;
					break;
				}
			}
			if(!rebuild) {
				// Check if any of the affected files are members of bundles built by a sub builder 
				Collection<Builder> builders = model.getSubBuilders();
				for (Builder builder : builders) {
					File buildPropsFile = builder.getPropertiesFile();
					if(affectedFiles.contains(buildPropsFile)) {
						rebuild = true;
						break;
					} else if(builder.isInScope(affectedFiles)) {
						rebuild = true;
						break;
					}
					progress.worked(1);
				}
			}
			progress.setWorkRemaining(10);
			
			if(rebuild)
				rebuildBndProject(project, monitor);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		model.refresh();
	}
	void rebuildBndProject(IProject project, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		Project model = Activator.getDefault().getCentral().getModel(JavaCore.create(project));
		model.refresh();
		
		// Get or create the build model for this bnd file
		IFile bndFile = project.getFile(Project.BNDFILE);

		// Clear markers
		if (bndFile.exists()) {
			bndFile.deleteMarkers(MARKER_BND_PROBLEM, true, IResource.DEPTH_INFINITE);
		}
		
		// Build
		try { 
			File files[] = model.build();
			if (files != null)
				for (File f : files) {
					Central.refresh(Central.toPath(model, f));
				}
			progress.worked(1);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error building project.", e));
		}

		// Report errors
		List<String> errors = new ArrayList<String>(model.getErrors());
		for (String errorMessage : errors) {
			IMarker marker = bndFile.createMarker(MARKER_BND_PROBLEM);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			marker.setAttribute(IMarker.MESSAGE, errorMessage);
			marker.setAttribute(IMarker.LINE_NUMBER, 1);
			model.clear();
		}
	}
}
