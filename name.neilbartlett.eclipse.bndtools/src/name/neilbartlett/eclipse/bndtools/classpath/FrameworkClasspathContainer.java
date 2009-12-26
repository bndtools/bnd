package name.neilbartlett.eclipse.bndtools.classpath;

import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

class FrameworkClasspathContainer implements IClasspathContainer {
	
	private static final IClasspathEntry[] EMPTY_ENTRIES = new IClasspathEntry[0];
	
	private final IPath path;
	private final IFrameworkInstance frameworkInstance;
	private final IClasspathEntry annotationsEntry;

	public FrameworkClasspathContainer(IPath path, IFrameworkInstance frameworkInstance) {
		this(path, frameworkInstance, null);
	}
	
	IFrameworkInstance getFrameworkInstance() {
		return frameworkInstance;
	}
	
	FrameworkClasspathContainer(IPath path, IFrameworkInstance frameworkInstance, IPath annotationsPath) {
		this.path = path;
		this.frameworkInstance = frameworkInstance;
		
		if(annotationsPath != null) {
			annotationsEntry = JavaCore.newLibraryEntry(annotationsPath, null, null, new IAccessRule[0], new IClasspathAttribute[0], false);
		} else {
			annotationsEntry = null;
		}
	}

	public IClasspathEntry[] getClasspathEntries() {
		IClasspathEntry[] entries = EMPTY_ENTRIES;
		
		if(frameworkInstance != null && frameworkInstance.getStatus().isOK())
			entries = frameworkInstance.getClasspathEntries();
		
		if(annotationsEntry != null) {
			IClasspathEntry[] copy = new IClasspathEntry[entries.length + 1];
			System.arraycopy(entries, 0, copy, 0, entries.length);
			copy[entries.length] = annotationsEntry;
			entries = copy;
		}
		
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
