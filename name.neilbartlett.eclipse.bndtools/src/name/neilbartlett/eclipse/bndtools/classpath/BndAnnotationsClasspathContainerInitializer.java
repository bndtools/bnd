package name.neilbartlett.eclipse.bndtools.classpath;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class BndAnnotationsClasspathContainerInitializer extends
		ClasspathContainerInitializer {
	
	public static final String FRAMEWORK_CONTAINER_ID = "name.neilbartlett.eclipse.bndtools.FRAMEWORK_CONTAINER";

	@Override
	public void initialize(IPath containerPath, IJavaProject project)
			throws CoreException {
		if(isValidFrameworkContainerPath(containerPath)) {
			FrameworkClasspathContainer container = fromContainerPath(containerPath);
			JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project}, new IClasspathContainer[] { container } , null);
		}
	}

	static FrameworkClasspathContainer fromContainerPath(IPath containerPath) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Not implemented", null));
	}

	static boolean isValidFrameworkContainerPath(IPath path) {
		return path != null && path.segmentCount() == 2 && FRAMEWORK_CONTAINER_ID.equals(path.segment(0));
	}

	
}
