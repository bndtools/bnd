/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools;

import name.neilbartlett.eclipse.bndtools.builder.BndIncrementalBuilder;
import name.neilbartlett.eclipse.bndtools.builder.BndProjectNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;

public class StartupBuildJob extends Job {

	public StartupBuildJob(String name) {
		super(name);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor);
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProject[] projects = workspace.getRoot().getProjects();

		IWorkspaceRunnable operation = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				SubMonitor progress = SubMonitor.convert(monitor, projects.length);
				for (IProject project : projects) {
					if(project.hasNature(BndProjectNature.NATURE_ID)) {
						project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, BndIncrementalBuilder.BUILDER_ID, null, progress.newChild(1));
					}
				}
			}
		};
		ISchedulingRule rule = workspace.getRuleFactory().buildRule();
		try {
			System.out.println("--> Initialising JavaCore before startup build job...");
			JavaCore.initializeAfterLoad(progress.newChild(1));
			
			System.out.println("--> Beginning startup build job");
			workspace.run(operation, rule, 0, progress.newChild(1));
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error building Bnd projects.", e);
		}
		
	}
}
