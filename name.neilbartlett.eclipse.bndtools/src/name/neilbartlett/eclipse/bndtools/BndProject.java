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
package name.neilbartlett.eclipse.bndtools;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import name.neilbartlett.eclipse.bndtools.builder.BndFileModel;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

// @ThreadSafe
public class BndProject {

	public static final long NEVER = -1;
	
	// @GuardedBy("projectMap")
	private static final ConcurrentHashMap<IPath, BndProject> projectMap = new ConcurrentHashMap<IPath, BndProject>();
	
	public static BndProject create(IProject project) {
		projectMap.putIfAbsent(project.getFullPath(), new BndProject(project));
		return projectMap.get(project.getFullPath());
	}
	
	private final IProject project;
	
	// @GuardedBy("fileModelMap")
	private final Map<IPath, BndFileModel> fileModelMap = new HashMap<IPath, BndFileModel>();
	
	// @GuardedBy("this")
	private long lastBuilt = NEVER;

	private final Set<IResource> exportDirs = new HashSet<IResource>();

	// Prevent direct instantiation
	private BndProject(IProject project) {
		this.project = project;
	}
	public IProject getProject() {
		return project;
	}
	public long getLastBuildTime() {
		return lastBuilt;
	}
	public void markBuilt() {
		this.lastBuilt = System.currentTimeMillis();
	}
	public void clearAll() {
		synchronized(fileModelMap) {
			fileModelMap.clear();
		}
		lastBuilt = NEVER;
	}
	/**
	 * Gets a {@link BndFileModel} instance for the specified bnd file path,
	 * creates a instance if one does not exist. Never returns null.
	 */
	public BndFileModel getFileModel(IPath bndFilePath) {
		BndFileModel result;
		synchronized (fileModelMap) {
			result = fileModelMap.get(bndFilePath);
			if(result == null) {
				result = new BndFileModel(bndFilePath);
				fileModelMap.put(bndFilePath, result);
			}
		}
		return result;
	}
	public Collection<BndFileModel> getAllFileModels() {
		return Collections.unmodifiableCollection(fileModelMap.values());
	}
	public BndFileModel removeFileModel(IPath fullPath) {
		synchronized(fileModelMap) {
			return fileModelMap.get(fullPath);
		}
	}
	public Set<IResource> getExportDirs() {
		return exportDirs;
	}
	public void setExportDirs(Collection<? extends IResource> exportDirs) {
		this.exportDirs.clear();
		this.exportDirs.addAll(exportDirs);
	}
}
