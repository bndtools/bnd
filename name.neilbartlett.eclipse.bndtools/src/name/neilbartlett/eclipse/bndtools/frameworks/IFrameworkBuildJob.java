package name.neilbartlett.eclipse.bndtools.frameworks;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IFrameworkBuildJob {
	void buildForLaunch(IProject project, IFrameworkInstance frameworkInstance, IProgressMonitor monitor) throws CoreException;
}
