/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Constants;

import aQute.bnd.build.Project;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Builder;
import bndtools.diff.JarDiff;
import bndtools.release.nl.Messages;

public class ReleaseDialogJob extends Job {

	private final Shell shell;
	private final Project project;
	private final List<File> subBundles;
	
	public ReleaseDialogJob(Project project, List<File> subBundles) {
		super(Messages.releaseJob);
		this.project = project;
		this.shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
		this.subBundles = subBundles;
		setUser(true);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
        try {
	    	monitor.beginTask(Messages.cleaningProject, 100);
	        try {
				IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName());
				proj.build(IncrementalProjectBuilder.FULL_BUILD, null);
			} catch (CoreException e) {
				return e.getStatus();
			}
			monitor.setTaskName(Messages.releasing);
			monitor.worked(33);
			monitor.subTask(Messages.checkingExported);
			
			final List<JarDiff> diffs = new ArrayList<JarDiff>();
			
			List<Builder> builders = project.getBuilder(null).getSubBuilders();
			for (Builder b : builders) {
				
				if (subBundles != null) {
					if (!subBundles.contains(b.getPropertiesFile())) {
						continue;
					}
				}
				
				RepositoryPlugin baselineRepository = ReleaseHelper.getBaselineRepository(project, b.getBsn(), b.getProperty(Constants.BUNDLE_VERSION));
				
				JarDiff jarDiff = JarDiff.createJarDiff(project, baselineRepository, b.getBsn());
				if (jarDiff != null) {
					diffs.add(jarDiff);
				}
			}
			if (diffs.size() == 0) {
				//TODO: message
				return Status.OK_STATUS;
			}
			monitor.worked(33);
			
			Runnable runnable = new Runnable() {
				public void run() {
					BundleReleaseDialog dialog = new BundleReleaseDialog(shell, project, diffs);
					dialog.open();
				}
			};

			if (Display.getCurrent() == null) {
				Display.getDefault().asyncExec(runnable);
			} else {
				runnable.run();
			}
			
			monitor.worked(33);
	        return Status.OK_STATUS;
        } catch (Exception e) {
        	return new Status(Status.ERROR, Activator.PLUGIN_ID, "Error : " + e.getMessage(), e);
        } finally {
        	
        	monitor.done();
        }

	}
	
}
