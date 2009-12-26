package name.neilbartlett.eclipse.bndtools.classpath;

import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

class FrameworkClasspathContainer implements IClasspathContainer {
	
	private static final IClasspathEntry[] EMPTY_ENTRIES = new IClasspathEntry[0];
	private final IPath path;
	private final IFrameworkInstance frameworkInstance;

	FrameworkClasspathContainer(IPath path, IFrameworkInstance frameworkInstance) {
		this.path = path;
		this.frameworkInstance = frameworkInstance;
	}

	public IClasspathEntry[] getClasspathEntries() {
		IClasspathEntry[] entries = EMPTY_ENTRIES;
		
		if(frameworkInstance != null && frameworkInstance.getStatus().isOK())
			entries = frameworkInstance.getClasspathEntries();
		
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
