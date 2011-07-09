package bndtools.classpath;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;


public class BndContainer implements IClasspathContainer {

	final IJavaProject javaProject;
	final IClasspathEntry[] entries;
    private final String description;

	public BndContainer(IJavaProject javaProject, IClasspathEntry[] entries, String description) {
		this.javaProject = javaProject;
		this.entries = entries;
        this.description = description;
	}

	public IClasspathEntry[] getClasspathEntries() {
		return entries;
	}

	public String getDescription() {
		return description != null ? description : Messages.BndContainer_ContainerName;
	}

	public int getKind() {
		return IClasspathContainer.K_APPLICATION;
	}

	public IPath getPath() {
		return BndContainerInitializer.PATH_ID;
	}
}