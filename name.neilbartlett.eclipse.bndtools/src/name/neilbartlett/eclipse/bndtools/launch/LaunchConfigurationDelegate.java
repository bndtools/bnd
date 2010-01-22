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
package name.neilbartlett.eclipse.bndtools.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.classpath.FrameworkUtils;
import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkBuildJob;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel;
import name.neilbartlett.eclipse.bndtools.prefs.frameworks.FrameworkPreferencesInitializer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

public class LaunchConfigurationDelegate extends JavaLaunchDelegate {
	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		// Pre-launch build steps. Placed here rather than in buildForLaunch,
		// because we want them to *actually* be executed.
		IProject project = getProjectForConfiguration(configuration);
		
		IFrameworkInstance frameworkInstance = getFrameworkInstanceForConfiguration(configuration);
		
		Collection<IFrameworkBuildJob> jobs = FrameworkUtils.findFrameworkBuildJob(frameworkInstance.getFrameworkId(), null);
		SubMonitor subMonitor = SubMonitor.convert(monitor, jobs.size() * 10);
		for (IFrameworkBuildJob job : jobs) {
			job.buildForLaunch(project, frameworkInstance, subMonitor.newChild(10));
		}
		
		// Launch the VM now
		super.launch(configuration, mode, launch, monitor);
	}
	
	@Override
	public String[] getClasspath(ILaunchConfiguration configuration)
			throws CoreException {
		List<String> result = new ArrayList<String>();
		
		IFrameworkInstance frameworkInstance = getFrameworkInstanceForConfiguration(configuration);
		IClasspathEntry[] entries = frameworkInstance.getClasspathEntries();
		
		for (IClasspathEntry entry : entries) {
			IPath path = entry.getPath();
			result.add(path.toString());
		}
		
		return result.toArray(new String[result.size()]);
	}
	
	@Override
	public String getMainTypeName(ILaunchConfiguration configuration)
			throws CoreException {
		return getFrameworkInstanceForConfiguration(configuration).getMainClassName();
	}
	
	@Override
	public String getProgramArguments(ILaunchConfiguration configuration)
			throws CoreException {
		File workingDir = getWorkingDirectory(configuration);
		
		IFrameworkInstance frameworkInstance = getFrameworkInstanceForConfiguration(configuration);
		String standardArgs = frameworkInstance.getStandardProgramArguments(workingDir);
		if(standardArgs == null)
			standardArgs = "";

		String additionalArgs = super.getProgramArguments(configuration);
		if(additionalArgs == null)
			additionalArgs = "";
		
		return standardArgs + " " + additionalArgs;
	}
	
	@Override
	public String getVMArguments(ILaunchConfiguration configuration)
			throws CoreException {
		File workingDir = getWorkingDirectory(configuration);
		
		IFrameworkInstance frameworkInstance = getFrameworkInstanceForConfiguration(configuration);
		String standardArgs = frameworkInstance.getStandardVMArguments(workingDir);
		if(standardArgs == null)
			standardArgs = "";

		String additionalArgs = super.getVMArguments(configuration);
		if(additionalArgs == null)
			additionalArgs = "";
		
		return standardArgs + " " + additionalArgs;
	}
	
	private IProject getProjectForConfiguration(
			ILaunchConfiguration configuration) throws CoreException {
		String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
		if(projectName == null)
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Project name is not specified.", null));
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if(!project.exists())
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Specified project does not exist.", null));
		if(!project.isOpen())
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Specified project is closed.", null));
		return project;
	}

	private final IFrameworkInstance getFrameworkInstanceForConfiguration(ILaunchConfiguration configuration) throws CoreException {
		IFrameworkInstance frameworkInstance;
		boolean useSpec = configuration.getAttribute(IFrameworkLaunchConstants.ATTR_USE_FRAMEWORK_SPEC_LEVEL, true);
		if(useSpec) {
			OSGiSpecLevel specLevel = null;
			try {
				String specLevelStr = configuration.getAttribute(IFrameworkLaunchConstants.ATTR_FRAMEWORK_SPEC_LEVEL, (String) null);
				if(specLevelStr != null) specLevel = Enum.valueOf(OSGiSpecLevel.class, specLevelStr);
			} catch (IllegalArgumentException e) {
			}
			if(specLevel == null) {
				throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "No OSGi framework specification level specified", null));
			}
			
			frameworkInstance = FrameworkPreferencesInitializer.getFrameworkInstance(specLevel);
			if(frameworkInstance == null) {
				throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "No OSGi framework instance could be found for the specified specification level.", null));
			}
		} else {
			String frameworkId = configuration.getAttribute(IFrameworkLaunchConstants.ATTR_FRAMEWORK_ID, (String) null);
			String instancePath = configuration.getAttribute(IFrameworkLaunchConstants.ATTR_FRAMEWORK_INSTANCE_PATH, (String) null);
			if(frameworkId == null || instancePath == null) {
				throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "No OSGi framework instance was specified.", null));
			}
			IFramework framework = FrameworkUtils.findFramework(frameworkId);
			if(framework == null)
				throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, String.format("No OSGi framework could be found with as ID of %s.", frameworkId), null));
			frameworkInstance = framework.createFrameworkInstance(new File(instancePath));
		}
		if(!frameworkInstance.getStatus().isOK())
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Invalid OSGi framework instance: " + frameworkInstance.getStatus().getMessage(), null));
		if(!frameworkInstance.isLaunchable())
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "A non-launchable framework instance was selected.", null));
		return frameworkInstance;
	}
}
