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
package name.neilbartlett.eclipse.bndtools.classpath;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkBuildJob;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

public class FrameworkUtils {
	
	private FrameworkUtils() {}
	
	
	/**
	 * Find and instantiate a framework object representing the framework with
	 * the specified ID.
	 * 
	 * @param id
	 *            The ID of the framework extension element.
	 * @return An {@link IFramework} object, or {@code null} if no frameworks
	 *         exist with the specified ID.
	 * @throws CoreException
	 *             If there was an error instantiating the {@link IFramework}
	 *             object.
	 */
	public static IFramework findFramework(String id) throws CoreException {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, Plugin.EXTPOINT_OSGI_FRAMEWORKS);
		for (IConfigurationElement element : elements) {
			String elementId = element.getAttribute("id");
			if(id.equals(elementId)) {
				return (IFramework) element.createExecutableExtension("class");
			}
		}
		return null;
	}

	/**
	 * Find and instantiate all framework build jobs associated with the
	 * specified framework ID.
	 * 
	 * @param frameworkId
	 *            The framework ID.
	 * @param status
	 *            A {@link MultiStatus} that accumulates any extension
	 *            instantiation errors arising from the operation; may be
	 *            {@code null}.
	 * @return
	 */
	public static Collection<IFrameworkBuildJob> findFrameworkBuildJob(String frameworkId, MultiStatus status) {
		List<IFrameworkBuildJob> result = new LinkedList<IFrameworkBuildJob>();
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, Plugin.EXTPOINT_OSGI_FRAMEWORK_BUILD_JOBS);
		for (IConfigurationElement element : elements) {
			String elementframeworkId = element.getAttribute("frameworkId");
			if(frameworkId.equals(elementframeworkId)) {
				try {
					result.add((IFrameworkBuildJob) element.createExecutableExtension("class"));
				} catch (CoreException e) {
					if(status != null)
						status.add(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, "Error creating build job instance.", e));
				}
			}
		}
		return result;
	}

}
