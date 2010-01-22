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
package name.neilbartlett.eclipse.bndtools.frameworks.felix;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

public class FelixBuildJob implements IFrameworkBuildJob {

	public static final String ISO_8859_1 = "ISO-8859-1";
	
	private static final String README_TXT = "Bundle files placed in this directory will be automatically deployed to the Felix framework.";
	private static final String MARKER_SHELL_VERSION = "@SHELL_VERSION@";
	private static final String MARKER_SHELL_TUI_VERSION = "@SHELL_TUI_VERSION@";

	public void buildForLaunch(final IProject project,
			final IFrameworkInstance frameworkInstance, IProgressMonitor monitor)
			throws CoreException {
		
		final FelixInstance felixInstance = (FelixInstance) frameworkInstance;
		
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				SubMonitor progress = SubMonitor.convert(monitor, 3);
				
				// Create the 'felix' folder
				IFolder felixFolder = project.getFolder("felix");
				if(!felixFolder.exists()) {
					felixFolder.create(IResource.NONE, true, progress.newChild(1));
				} else {
					felixFolder.refreshLocal(IResource.DEPTH_INFINITE, progress.newChild(1));
				}
				// Create the 'felix/config.properies' file
				IFile configFile = felixFolder.getFile("config.properties");
				if(!configFile.exists()) {
					InputStream content;
					try {
						StringBuilder buffer = new StringBuilder();
						
						URL sampleConfigIni = FelixBuildJob.class.getResource("sample_config.properties");
						InputStream inputStream = sampleConfigIni.openStream();
						BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, ISO_8859_1));
						
						String line = reader.readLine();
						while(line != null) {
							line = line.replaceFirst(MARKER_SHELL_VERSION, felixInstance.getShellTuiVersion());
							line = line.replaceFirst(MARKER_SHELL_TUI_VERSION, felixInstance.getShellTuiVersion());
							buffer.append(line).append('\n');
							
							line = reader.readLine();
						}
						content = new ByteArrayInputStream(buffer.toString().getBytes(ISO_8859_1));
					} catch (IOException e) {
						Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error accessing sample 'config.properties' resource.", e));
						content = new ByteArrayInputStream(new byte[0]);
					}
					configFile.create(content, IResource.NONE, progress.newChild(1));
				} else {
					configFile.refreshLocal(IResource.DEPTH_ZERO, progress.newChild(1));
				}

				
				// Create the 'felix/bundle' directory, if possible
				IFolder bundleFolder = felixFolder.getFolder("bundle");
				try {
					if(!bundleFolder.exists())	
						bundleFolder.create(false, true, progress.newChild(1));
					IFile readmeFile = bundleFolder.getFile("README.txt");
					if(!readmeFile.exists())
						readmeFile.create(new ByteArrayInputStream(README_TXT.getBytes()), false, progress.newChild(1));
				} catch (CoreException e) {
					Activator.getDefault().getLog().log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, "Problem creating 'felix/bundle' directory or 'felix/bundle/README.txt' file, skipping", e));
					progress.worked(1);
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(runnable, monitor);
	}

}
