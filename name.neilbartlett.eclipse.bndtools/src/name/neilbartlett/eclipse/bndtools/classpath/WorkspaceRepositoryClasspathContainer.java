package name.neilbartlett.eclipse.bndtools.classpath;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.BndProject;
import name.neilbartlett.eclipse.bndtools.builder.BndFileModel;

import org.eclipse.core.resources.IProject;
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
	private final String[] depends;

	WorkspaceRepositoryClasspathContainer(IPath containerPath, IJavaProject javaProject, String[] depends) {
		this.containerPath = containerPath;
		this.javaProject = javaProject;
		this.depends = depends;
	}
	String[] getDepends() {
		return this.depends;
	}
	public IClasspathEntry[] getClasspathEntries() {
		List<IClasspathEntry> result = new LinkedList<IClasspathEntry>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (String projectName : depends) {
			IProject project = root.getProject(projectName);
			if(project.exists() && project.isOpen()) {
				BndProject bndProject = BndProject.create(project);

				Collection<BndFileModel> fileModels = bndProject.getAllFileModels();
				for (BndFileModel fileModel : fileModels) {
					IPath targetPath = fileModel.getTargetPath();

					IClasspathEntry newEntry = JavaCore.newLibraryEntry(targetPath, null, null, false);
					result.add(newEntry);
				}
			}
		}
		return (IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);
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

}
