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
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import aQute.bnd.build.Project;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Jar;
import bndtools.diff.JarDiff;
import bndtools.release.api.ReleaseContext;
import bndtools.release.api.ReleaseUtils;
import bndtools.release.nl.Messages;

public class ReleaseJob  extends Job {
	
	private Project project;
	private List<JarDiff> diffs;
	private String repository;

	public ReleaseJob(Project project, List<JarDiff> diffs, String repository) {
		super(Messages.bundleReleaseJob);
		this.project = project;
		this.diffs = diffs;
		this.repository = repository;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		try {
						
			IProject proj = ReleaseUtils.getProject(project);
			proj.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			
			RepositoryPlugin repo = Activator.getRepositoryPlugin(repository);
			ReleaseContext context = new ReleaseContext(project, diffs, repo, monitor);
			
			boolean ok = ReleaseHelper.release(context, diffs);
			
			ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName()).refreshLocal(IResource.DEPTH_INFINITE, context.getProgressMonitor());
			
			if (repo != null) {
				File f = Activator.getLocalRepoLocation(repo);
				Activator.refreshFile(f);
			}
			if (ok) {
				StringBuilder sb = new StringBuilder();
				sb.append(Messages.project);
				sb.append(" : ");
				sb.append(project.getName());
				sb.append("\n\n");
				sb.append(Messages.released);
				sb.append(" :\n");

				for (Jar jar : context.getReleasedJars()) {
					sb.append(ReleaseUtils.getBundleSymbolicName(jar) + "-" + ReleaseUtils.getBundleVersion(jar) + "\n");
				}
				
				sb.append("\n\n");
				sb.append(Messages.releasedTo);
				sb.append(" : ");
				sb.append(repository);
				
				Activator.getDefault().message(sb.toString());
			}

		} catch (Exception e) {
			return new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
		}

		return Status.OK_STATUS;
	}
}
