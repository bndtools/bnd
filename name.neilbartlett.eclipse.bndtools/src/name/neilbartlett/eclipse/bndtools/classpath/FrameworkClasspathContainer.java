package name.neilbartlett.eclipse.bndtools.classpath;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

class FrameworkClasspathContainer implements IClasspathContainer {
	
	private final IPath path;
	private final IClasspathEntry[] entries;

	FrameworkClasspathContainer(IPath path, IClasspathEntry[] entries) {
		this.path = path;
		this.entries = entries;
	}

	public IClasspathEntry[] getClasspathEntries() {
		return entries;
	}

	public String getDescription() {
		return "OSGi Framework Core and Compendium";
	}

	public int getKind() {
		return IClasspathContainer.K_APPLICATION;
	}

	public IPath getPath() {
		return path;
	}
}
