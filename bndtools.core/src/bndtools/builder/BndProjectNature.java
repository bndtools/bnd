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
package bndtools.builder;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.classpath.BndContainerInitializer;

public class BndProjectNature implements IProjectNature {

    public static final String NATURE_ID = Plugin.PLUGIN_ID + ".bndnature";

    private IProject project;

    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

    public void configure() throws CoreException {
        final IProjectDescription desc = project.getDescription();
        addBuilder(desc);
        updateProject(desc, true);
    }

    public void deconfigure() throws CoreException {
        IProjectDescription desc = project.getDescription();
        removeBuilder(desc);
        updateProject(desc, false);
    }

    private void addBuilder(IProjectDescription desc) {
        ICommand[] commands = desc.getBuildSpec();
        for (ICommand command : commands) {
            if (command.getBuilderName().equals(NewBuilder.BUILDER_ID))
                return;
        }

        ICommand[] nu = new ICommand[commands.length + 1];
        System.arraycopy(commands, 0, nu, 0, commands.length);

        ICommand command = desc.newCommand();
        command.setBuilderName(NewBuilder.BUILDER_ID);
        nu[commands.length] = command;
        desc.setBuildSpec(nu);
    }

    private void removeBuilder(IProjectDescription desc) {
        ICommand[] commands = desc.getBuildSpec();
        List<ICommand> nu = new ArrayList<ICommand>();
        for (ICommand command : commands) {
            if (!command.getBuilderName().equals(NewBuilder.BUILDER_ID)) {
                nu.add(command);
            }
        }
        desc.setBuildSpec(nu.toArray(new ICommand[nu.size()]));
    }

    private void ensureBndBndExists() throws CoreException {
        IFile bndfile = project.getFile(Project.BNDFILE);
        if (!bndfile.exists())
            bndfile.create(new ByteArrayInputStream(new byte[0]), false, null);
    }

    private void installBndClasspath() throws CoreException {
        IJavaProject javaProject = JavaCore.create(project);
        IClasspathEntry[] classpath = javaProject.getRawClasspath();
        for (IClasspathEntry entry : classpath) {
            if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && BndContainerInitializer.PATH_ID.equals(entry.getPath()))
                return; // already installed
        }

        IClasspathEntry[] newEntries = new IClasspathEntry[classpath.length + 1];
        System.arraycopy(classpath, 0, newEntries, 0, classpath.length);
        newEntries[classpath.length] = JavaCore.newContainerEntry(BndContainerInitializer.PATH_ID);

        javaProject.setRawClasspath(newEntries, null);
    }

    private void removeBndClasspath() throws CoreException {
        IJavaProject javaProject = JavaCore.create(project);
        IClasspathEntry[] classpath = javaProject.getRawClasspath();
        List<IClasspathEntry> newEntries = new ArrayList<IClasspathEntry>(classpath.length);

        boolean changed = false;
        for (IClasspathEntry entry : classpath) {
            if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && BndContainerInitializer.PATH_ID.equals(entry.getPath())) {
                changed = true;
            } else {
                newEntries.add(entry);
            }
        }

        if (changed)
            javaProject.setRawClasspath(newEntries.toArray(new IClasspathEntry[newEntries.size()]), null);
    }

    private void updateProject(final IProjectDescription desc, final boolean adding) throws CoreException {
        IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                project.setDescription(desc, monitor);
                if (adding) {
                    ensureBndBndExists();
                    installBndClasspath();
                } else {
                    removeBndClasspath();
                }
            }
        };
        project.getWorkspace().run(runnable, null);
    }

}
