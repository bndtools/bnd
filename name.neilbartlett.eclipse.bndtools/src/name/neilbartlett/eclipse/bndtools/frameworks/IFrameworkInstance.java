/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package name.neilbartlett.eclipse.bndtools.frameworks;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;

/**
 * <p>
 * Represents a specific installation of an OSGi framework based on a resource
 * path, which may be either an installation directory or a JAR file.
 * </p>
 * 
 * <p>
 * <strong>NB:</strong> Implementations of this interface must provide working
 * {@link #hashCode()} and {@link #equals(Object)} methods based on the {@code
 * instancePath} property. I.e., two instances that are the same concrete type
 * and return the same path from {@link #getInstancePath()} should be considered
 * equal and return the same hash code.
 * </p>
 * 
 * @author Neil Bartlett
 * 
 */
public interface IFrameworkInstance {
	
	OSGiSpecLevel getOSGiSpecLevel();
	
	/**
	 * Check whether the instance is valid for the selected framework type.
	 * 
	 * @return An {@link IStatus} object, which should have a severity of
	 *         {@code OK} if this framework instance is valid.
	 */
	IStatus getStatus();

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

	/**
	 * Return the ID of this framework.
	 * @return
	 */
	String getFrameworkId();
	
	/**
	 * Return whether this framework instance is launchable.
	 */
	boolean isLaunchable();
	
	/**
	 * Return the main class name used to launch instances of this OSGi
	 * framework. Non-launchable frameworks may return {@code null}.
	 * 
	 * @return The fully qualified "main" class name, or {@code null} if the
	 *         framework is not launchable.
	 */
	String getMainClassName();

	/**
	 * Return the standard program arguments for launching this framework
	 * instance in the specified working directory. Non-launchable framework instances may return {@code null} from this method.
	 * 
	 * @param workingDir
	 *            The working directory in which to launch
	 * @return The standard program arguments, as a String, with multiple
	 *         arguments separated by whitespace.
	 */
	String getStandardProgramArguments(File workingDir);
	
	/**
	 * Return the standard program arguments for launching this framework
	 * instance in the specified working directory. Non-launchable framework instances may return {@code null} from this method.
	 * 
	 * @param workingDir
	 *            The working directory in which to launch
	 * @return The standard program arguments, as a String, with multiple
	 *         arguments separated by whitespace.
	 */
	String getStandardVMArguments(File workingDir);
	
	public int hashCode();
	
	public boolean equals(Object obj);
}
