package name.neilbartlett.eclipse.bndtools.classpath;

import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

class FrameworkClasspathContainer implements IClasspathContainer {
	
	private final IPath path;
	private final IClasspathEntry[] entries;
	private final IFrameworkInstance frameworkInstance;

	FrameworkClasspathContainer(IPath path, IClasspathEntry[] entries, IFrameworkInstance frameworkInstance) {
		this.path = path;
		this.entries = entries;
		this.frameworkInstance = frameworkInstance;
	}

	public IClasspathEntry[] getClasspathEntries() {
		return entries;
	}

	public String getDescription() {
		return String.format("OSGi Framework (%s)", frameworkInstance.getDisplayString());
	}

	public int getKind() {
		return IClasspathContainer.K_APPLICATION;
	}

	public IPath getPath() {
		return path;
	}
}
