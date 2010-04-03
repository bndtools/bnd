package aQute.bnd.classpath;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import aQute.bnd.build.Project;

public class BndContainer implements IClasspathContainer {

	final Project project;
	final IJavaProject javaProject;
	final IClasspathEntry[] entries;

	BndContainer(Project project, IJavaProject javaProject, IClasspathEntry[] entries) {
		this.project = project;
		this.javaProject = javaProject;
		this.entries = entries;
	}

	public IClasspathEntry[] getClasspathEntries() {
		return entries;
	}

	public String getDescription() {
		return Messages.BndContainer_ContainerName;
	}

	public int getKind() {
		return IClasspathContainer.K_APPLICATION;
	}

	public IPath getPath() {
		return BndContainerInitializer.ID;
	}

	public Project getModel() {
		return project;
	}
}