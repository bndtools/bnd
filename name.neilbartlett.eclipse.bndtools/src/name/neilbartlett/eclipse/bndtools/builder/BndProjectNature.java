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
package name.neilbartlett.eclipse.bndtools.builder;

import java.util.ArrayList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class BndProjectNature implements IProjectNature {
	
	public static final String NATURE_ID = Plugin.PLUGIN_ID + ".bndnature";

	private IProject project;

	public void configure() throws CoreException {
		final IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		
		for (ICommand command : commands) {
			if(command.getBuilderName().equals(BndIncrementalBuilder.BUILDER_ID))
				return;
		}
		
		ICommand[] nu = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, nu, 0, commands.length);
		
		ICommand command = desc.newCommand();
		command.setBuilderName(BndIncrementalBuilder.BUILDER_ID);
		nu[commands.length] = command;
		desc.setBuildSpec(nu);
		
		doSetProjectDesc(desc);
	}

	void doSetProjectDesc(final IProjectDescription desc)
			throws CoreException {
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				project.setDescription(desc, monitor);
			}
		};
		project.getWorkspace().run(runnable, null);
	}

	public void deconfigure() throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		List<ICommand> nu = new ArrayList<ICommand>();
		
		for (ICommand command : commands) {
			if(!command.getBuilderName().equals(BndIncrementalBuilder.BUILDER_ID)) {
				nu.add(command);
			}
		}
		
		desc.setBuildSpec((ICommand[]) nu.toArray(new ICommand[nu.size()]));
		doSetProjectDesc(desc);
	}

	public IProject getProject() {
		return project;
	}

	public void setProject(IProject project) {
		this.project = project;
	}

}
