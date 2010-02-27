package name.neilbartlett.eclipse.bndtools.classpath;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

class WorkspaceRepositoryClasspathContainer implements
		IClasspathContainer {
	
	private final IPath containerPath;
	private final IJavaProject javaProject;
	private final Collection<BundleDependency> dependencies;
	private final Map<BundleDependency, ExportedBundle> bindings;

	private AtomicReference<IClasspathEntry[]> entriesRef = new AtomicReference<IClasspathEntry[]>(null);

	WorkspaceRepositoryClasspathContainer(IPath containerPath, IJavaProject javaProject, Collection<BundleDependency> dependencies, Map<BundleDependency,ExportedBundle> bindings) {
		this.containerPath = containerPath;
		this.javaProject = javaProject;
		this.dependencies = dependencies;
		this.bindings = bindings;
	}
	public IClasspathEntry[] getClasspathEntries() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		
		IClasspathEntry[] result = entriesRef.get();
		if(result != null) return result;
		
		result = new IClasspathEntry[bindings.size()];
		int i = 0;
		for(Iterator<ExportedBundle> iter = bindings.values().iterator(); iter.hasNext(); i++) {
			ExportedBundle bundle = iter.next();
			
			IPath bndFilePath = bundle.getSourceBndFilePath();
			IPath srcProjectPath = null;
			if(bndFilePath != null) {
				IResource bndFile = root.findMember(bndFilePath);
				if(bndFile != null) {
					srcProjectPath = bndFile.getProject().getFullPath();
				}
			}
			
			result[i] = JavaCore.newLibraryEntry(bundle.getPath(), srcProjectPath, null, false);
		}
		entriesRef.compareAndSet(null, result);
		return entriesRef.get();
	}
	public String getDescription() {
		return "Workspace Bundle Repository";
	}
	public int getKind() {
		return K_APPLICATION;
	}
	public IPath getPath() {
		return containerPath;
	}
	public Collection<BundleDependency> getDependencies() {
		return Collections.unmodifiableCollection(dependencies);
	}
	public ExportedBundle getBinding(BundleDependency dependency) {
		return bindings.get(dependency);
	}
	public Map<BundleDependency, ExportedBundle> getAllBindings() {
		return Collections.unmodifiableMap(bindings);
	}
	public boolean isBoundToPath(IPath path) {
		for (ExportedBundle bundle : bindings.values()) {
			if(bundle.getPath().equals(path)) {
				return true;
			}
		}
		return false;
	}
	public IJavaProject getJavaProject() {
		return javaProject;
	}
}