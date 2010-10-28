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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import aQute.bnd.build.Project;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.diff.JarDiff;
import bndtools.release.api.ReleaseContext;

public class ReleaseJob  extends Job {
	
	private Project project;
	private List<JarDiff> diffs;
	private String repository;

	public ReleaseJob(Project project, List<JarDiff> diffs, String repository) {
		super("Release Job");
		this.project = project;
		this.diffs = diffs;
		this.repository = repository;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		try {
			RepositoryPlugin repo = Activator.getRepositoryPlugin(repository);
			ReleaseContext context = new ReleaseContext(project, diffs, repo, monitor, project);
			
			ReleaseHelper.updateProject(context);

			StringBuilder sb = new StringBuilder();
			sb.append("Project : ");
			sb.append(project.getName());
			sb.append("\n\n");
			sb.append("Released :\n");
			boolean ok = true;

			for (JarDiff diff : diffs) {
				sb.append(diff.getSymbolicName() + "-" + diff.getSuggestedVersion() + ".jar\n");
				if (!ReleaseHelper.release(context, diff)) {
					ok = false;
					break;
				}
			}
			sb.append("\n\nto : ");
			sb.append(repository);
			
			// Necessary???
			ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName()).refreshLocal(IResource.DEPTH_INFINITE, context.getProgressMonitor());
			
			if (repo != null) {
				File f = Activator.getLocalRepoLocation(repo);
				Activator.refreshFile(f);
			}
			if (ok) {
				Activator.getDefault().message(sb.toString());
			}

		} catch (Exception e) {
			return new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
		}

		return Status.OK_STATUS;
	}
}
