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
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;

/**
 * Represents an OSGi framework kind (e.g. Equinox, Felix...)
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

	/**
	 * Provides a list of framework locations, suitable to be passed into
	 * {@link #createFrameworkInstance(File)}, that have been automatically
	 * configured or detected by this framework type. This method will only be
	 * called if the {@code supportsAutoConfig} attribute is present as set to
	 * true on the contributed {@code osgiFrameworks} extension.
	 * 
	 * @return A collection of locations, or {@code null} if auto-configuration
	 *         is not supported or unavailable.
	 */
	Collection<File> getAutoConfiguredLocations();
}
