package name.neilbartlett.eclipse.bndtools;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import name.neilbartlett.eclipse.bndtools.builder.BndFileModel;

import org.eclipse.core.resources.IProject;
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
}
