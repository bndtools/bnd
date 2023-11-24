package bndtools.facades.jdt;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.annotation.versioning.ConsumerType;

import bndtools.facades.util.EclipseBinder;

@ConsumerType
public class ClasspathContainerInitializerFacade extends ClasspathContainerInitializer implements IExecutableExtension
{
	final EclipseBinder<ClasspathContainerInitializer> binder = new EclipseBinder<>(
		ClasspathContainerInitializer.class,
		this);

	@Override
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		binder.get()
			.initialize(containerPath, project);

	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
		throws CoreException {
		binder.setInitializationData(config, propertyName, data);
	}

}
