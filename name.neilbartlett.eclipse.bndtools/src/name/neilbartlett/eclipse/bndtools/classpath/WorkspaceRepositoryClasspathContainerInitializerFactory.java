package name.neilbartlett.eclipse.bndtools.classpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;

public class WorkspaceRepositoryClasspathContainerInitializerFactory implements IExecutableExtensionFactory {
	
	public Object create() throws CoreException {
		return WorkspaceRepositoryClasspathContainerInitializer.getInstance();
	}

}
