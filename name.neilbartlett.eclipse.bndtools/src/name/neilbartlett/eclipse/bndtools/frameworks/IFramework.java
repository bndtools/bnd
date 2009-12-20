package name.neilbartlett.eclipse.bndtools.frameworks;

import java.io.File;
import java.lang.annotation.AnnotationFormatError;

import org.eclipse.core.runtime.CoreException;

/**
 * Represents an instance of an OSGi framework
 * 
 * @author Neil Bartlett
 */
public interface IFramework {

	/**
	 * Create a framework instance from the selected resource.
	 * 
	 * @param resource
	 *            A resource (file or directory) that contains a framework of
	 *            this type. Only resources that have been validated with the
	 *            {@link #validateFrameworkResource(File)} method will be passed
	 *            to this method.
	 * @return A {@link IFrameworkInstance} object representing a specific
	 *         instance of this framework type.
	 * @throws CoreException
	 *             If the framework instance could not be created.
	 */
	IFrameworkInstance createFrameworkInstance(File resource)
			throws CoreException;
}
