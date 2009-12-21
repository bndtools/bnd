package name.neilbartlett.eclipse.bndtools.frameworks;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;

public interface IFrameworkInstance {
	/**
	 * Check whether the instance is valid for the selected framework type.
	 * 
	 * @return {@code null} if valid, otherwise an error message.
	 */
	String getValidationError();

	/**
	 * Get logical installation path of this instance, which may be a directory
	 * containing the core runtime and other resources.
	 * 
	 * @return The installation path of the framework instance
	 */
	IPath getInstancePath();

	/**
	 * Get the display name of the instance.
	 * 
	 * @return The human-readable display name of the framework instance.
	 */
	String getDisplayString();

	/**
	 * Get the array of classpath entries that must be added to the project at
	 * compile and run time.
	 * 
	 * @return An array {@code IClasspathEntry} for the core framework.
	 */
	IClasspathEntry[] getClasspathEntries();

	/**
	 * Return an icon image representing this framework type or instance. The
	 * caller of this method shall be responsible for cleaning the image
	 * resources when finished.
	 * 
	 * @param device
	 *            The SWT device.
	 * @return A new icon image.
	 */
	Image createIcon(Device device);

	String getFrameworkId();
}
