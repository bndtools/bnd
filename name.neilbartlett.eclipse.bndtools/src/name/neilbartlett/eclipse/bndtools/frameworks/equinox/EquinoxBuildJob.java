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
package name.neilbartlett.eclipse.bndtools.frameworks.equinox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkBuildJob;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import aQute.bnd.plugin.Activator;

public class EquinoxBuildJob implements IFrameworkBuildJob {

	public void buildForLaunch(final IProject project,
			IFrameworkInstance frameworkInstance, IProgressMonitor monitor)
			throws CoreException {
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				SubMonitor subMonitor = SubMonitor.convert(monitor, 2);
				// Create the 'equinox' folder
				IFolder equinoxFolder = project.getFolder("equinox");
				if(!equinoxFolder.exists()) {
					equinoxFolder.create(IResource.NONE, true, subMonitor.newChild(1));
				} else {
					equinoxFolder.refreshLocal(IResource.DEPTH_ONE, subMonitor.newChild(1));
				}
				
				// Create the 'equinox/config.ini' file
				IFile configIniFile = equinoxFolder.getFile("config.ini");
				if(!configIniFile.exists()) {
					InputStream inputStream;
					try {
						URL sampleConfigIni = EquinoxBuildJob.class.getResource("sample_config.ini");
						inputStream = sampleConfigIni.openStream();
					} catch (IOException e) {
						Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error accessing sample 'config.ini' resource.", e));
						inputStream = new ByteArrayInputStream(new byte[0]);
					}
					configIniFile.create(inputStream, IResource.NONE, subMonitor.newChild(1));
				} else {
					configIniFile.refreshLocal(IResource.DEPTH_ZERO, subMonitor.newChild(1));
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(runnable, monitor);
	}

}
