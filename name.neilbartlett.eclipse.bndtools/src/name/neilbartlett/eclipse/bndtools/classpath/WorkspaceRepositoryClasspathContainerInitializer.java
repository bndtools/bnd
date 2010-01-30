package name.neilbartlett.eclipse.bndtools.classpath;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class WorkspaceRepositoryClasspathContainerInitializer extends
		ClasspathContainerInitializer {
	
	public static final String CONTAINER_ID = Plugin.PLUGIN_ID + ".WORKSPACE_REPOSITORY";
	
	private static final AtomicReference<WorkspaceRepositoryClasspathContainerInitializer> instanceRef
		= new AtomicReference<WorkspaceRepositoryClasspathContainerInitializer>(null);
	
	public static final WorkspaceRepositoryClasspathContainerInitializer getInstance() {
		WorkspaceRepositoryClasspathContainerInitializer instance;

		instance = instanceRef.get();
		if(instance == null) {
			instanceRef.compareAndSet(null, new WorkspaceRepositoryClasspathContainerInitializer());
			instance = instanceRef.get();
		}
		return instanceRef.get();
	}

	private final Map<String, WorkspaceRepositoryClasspathContainer> projectContainerMap = new HashMap<String, WorkspaceRepositoryClasspathContainer>();
	private final Map<String, Set<String>> reverseDependsMap = new HashMap<String, Set<String>>();
	
	// Prevent instantiation.
	private WorkspaceRepositoryClasspathContainerInitializer() {
	}
	@Override
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		if(containerPath.segmentCount() == 2) {
			// Construct the new container
			String dependListStr = containerPath.segment(1);
			StringTokenizer tokenizer = new StringTokenizer(dependListStr, ",");
			Set<String> newDepends = new TreeSet<String>();
			while(tokenizer.hasMoreTokens()) {
				String name = tokenizer.nextToken().trim();
				newDepends.add(name);
			}
			WorkspaceRepositoryClasspathContainer newContainer = new WorkspaceRepositoryClasspathContainer(containerPath, project, newDepends.toArray(new String[newDepends.size()]));
			
			// Find the old container and remove any old dependencies from the reverse dependency map
			WorkspaceRepositoryClasspathContainer oldContainer = projectContainerMap.put(project.getProject().getName(), newContainer);
			if(oldContainer != null) {
				String[] oldDepends = oldContainer.getDepends();
				for (String projectName : oldDepends) {
					if(!newDepends.contains(projectName)) {
						Set<String> set = reverseDependsMap.get(projectName);
						if(set != null) {
							set.remove(project.getProject().getName());
						}
					}
				}
			}
			
			// Add new dependencies to the reverse dependency map
			for (String projectName : newDepends) {
				Set<String> set = reverseDependsMap.get(projectName);
				if(set == null) {
					set = new HashSet<String>();
					reverseDependsMap.put(projectName, set);
				}
				set.add(project.getProject().getName());
			}
			
			// Rebind the container path for the project
			JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, new IClasspathContainer[] { newContainer }, null);
		}
	}
	
	public void bndFilesChanged(IProject project, List<IFile> deletedFiles, List<IFile> addedOrChangedFiles, IProgressMonitor monitor) throws JavaModelException {
		if((deletedFiles != null && !deletedFiles.isEmpty()) || (addedOrChangedFiles != null && !addedOrChangedFiles.isEmpty())) {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			Set<String> dependees = reverseDependsMap.get(project.getName());
			if(dependees == null)
				return;
			
			SubMonitor progress = SubMonitor.convert(monitor, dependees.size());
			
			for (String projectName : dependees) {
				IProject dependerProject = root.getProject(projectName);
				IJavaProject javaProject = JavaCore.create(dependerProject);
				
				WorkspaceRepositoryClasspathContainer container = projectContainerMap.get(projectName);
				WorkspaceRepositoryClasspathContainer newContainer = new WorkspaceRepositoryClasspathContainer(container.getPath(), javaProject, container.getDepends());
				
				JavaCore.setClasspathContainer(newContainer.getPath(), new IJavaProject[] { javaProject }, new IClasspathContainer[] { newContainer }, progress.newChild(1));
			}
		}
	}
}