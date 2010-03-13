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
package name.neilbartlett.eclipse.bndtools.wizards;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.builder.BndProjectNature;
import name.neilbartlett.eclipse.bndtools.utils.BundleUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

@SuppressWarnings("restriction")
public class NewBndProjectWizardPageTwo extends NewJavaProjectWizardPageTwo {
	
	public NewBndProjectWizardPageTwo(NewJavaProjectWizardPageOne pageOne) {
		super(pageOne);
	}
	
	/* Need this if inserting an intermediate page between PageOne and PageTwo
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if(!visible && getContainer().getCurrentPage() == frameworkPage) {
			removeProvisonalProject();
		}
	}
	*/
	@Override
	public void configureJavaProject(IProgressMonitor monitor) throws CoreException,
			InterruptedException {
		super.configureJavaProject(monitor);
		
		IProject project = getJavaProject().getProject();
		IProjectDescription desc = project.getDescription();
		String[] natures = desc.getNatureIds();
		for (String nature : natures) {
			if(BndProjectNature.NATURE_ID.equals(nature))
				return;
		}
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = BndProjectNature.NATURE_ID;
		desc.setNatureIds(newNatures);
		project.setDescription(desc, null);
	}
	
	void doSetProjectDesc(final IProject project, final IProjectDescription desc) throws CoreException {
		final IWorkspaceRunnable workspaceOp = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				project.setDescription(desc, monitor);
			}
		};
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						project.getWorkspace().run(workspaceOp, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			throw (CoreException) e.getTargetException();
		} catch (InterruptedException e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Interrupted while adding Bnd OSGi Project nature to project.", e));
		}
	}
	
	@Override
	protected IProject createProvisonalProject() {
		createCnfProject();
		return super.createProvisonalProject();
	}
	
	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException, InterruptedException {
		createCnfProject();
		super.performFinish(monitor);
	}

	private void createCnfProject() {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		
		final IWorkspaceRunnable createCnfProjectOp = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				try {
					IProject cnfProject = workspace.getRoot().getProject(Project.BNDCNF);
					if(cnfProject == null || !cnfProject.exists()) {
						SubMonitor progress = SubMonitor.convert(monitor, 4);
						
						// Create the project and configure it as a Java project
						createProject(cnfProject, (URI) null, progress.newChild(1));
						configureJavaProject(JavaCore.create(cnfProject), null, progress.newChild(1));
						
						// Copy build.bnd from the template
						InputStream templateStream = getClass().getResourceAsStream("template_build.bnd");
						IFile buildBnd = cnfProject.getFile(Workspace.BUILDFILE);
						if(!buildBnd.exists())
							buildBnd.create(templateStream, true, progress.newChild(1));
						progress.setWorkRemaining(1);
						
						// Create the bundle repository
						createRepository(cnfProject, progress.newChild(1));
					} else if(!cnfProject.isOpen()) {
						cnfProject.open(monitor);
					}
				} catch (InterruptedException e) {
				}
			}
		};
		
		try {
			// Check whether we can report on the GUI thread or not
			Display display = Display.getCurrent();
			if(display != null) {
				getContainer().run(true, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException {
						try {
							workspace.run(createCnfProjectOp, monitor);
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
					}
				});
			} else {
				workspace.run(createCnfProjectOp, null);
			}
		} catch (InvocationTargetException e) {
			ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error createing Bnd configuration project '{0}'.", Project.BNDCNF), e.getTargetException()));
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error createing Bnd configuration project '{0}'.", Project.BNDCNF), e));
		} catch (InterruptedException e) {
		}
	}
	
	void createRepository(IProject cnfProject, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		
		// Create the repository folder if it doesn't already exist
		IFolder repoFolder = cnfProject.getFolder("repo");
		if(!repoFolder.exists()) {
			repoFolder.create(true, true, progress.newChild(1)); 
		}
		
		// Copy in the repository contributions
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, Plugin.EXTPOINT_REPO_CONTRIB);
		if(elements != null && elements.length > 0) {
			progress.setWorkRemaining(elements.length);
			
			for (IConfigurationElement element : elements) {
				String path = element.getAttribute("path");
				if(path != null) {
					String bsn = element.getContributor().getName();
					Bundle bundle = BundleUtils.findBundle(bsn, null);
					if(bundle != null) {
						copyFromBundleToFolder(bundle, new Path(path), repoFolder, progress.newChild(1));
					}
				}
			}
		}
	}
	void copyFromBundleToFolder(Bundle bundle, IPath path, IFolder toFolder, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, "Copying repository contribution...", IProgressMonitor.UNKNOWN);
		
		URL baseUrl = bundle.getEntry(path.toString());
		IPath basePath = new Path(baseUrl.getPath()).makeAbsolute();
		
		@SuppressWarnings("unchecked")
		Enumeration<URL> entriesEnum = bundle.findEntries(path.toString(), null, true);
		int entryCount = 0;
		List<URL> entries = new ArrayList<URL>();
		while(entriesEnum.hasMoreElements()) {
			entries.add(entriesEnum.nextElement());
			entryCount++;
		}
		progress.setWorkRemaining(entryCount);
		
		path = path.makeAbsolute();
		if(entryCount > 0) {
			for (URL entry : entries) {
				IPath entryPath = new Path(entry.getPath()).makeAbsolute();
				if(basePath.isPrefixOf(entryPath)) {
					entryPath = entryPath.removeFirstSegments(basePath.segmentCount());
					
					if(entryPath.hasTrailingSeparator()) {
						// It's a folder
						IFolder newFolder = toFolder.getFolder(entryPath);
						if(!newFolder.exists())
							newFolder.create(true, true, progress.newChild(1));
					} else {
						// It's a file
						IFile newFile = toFolder.getFile(entryPath);
						try {
							if(newFile.exists()) {
								newFile.setContents(entry.openStream(), true, true, progress.newChild(1));
							} else {
								newFile.create(entry.openStream(), true, progress.newChild(1));
							}
						} catch (IOException e) {
							throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error reading file from repository contributor bundle.", e));
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	void configureJavaProject(IJavaProject javaProject, String newProjectCompliance, IProgressMonitor monitor) throws CoreException, InterruptedException {
		SubMonitor progress = SubMonitor.convert(monitor, 5);
		try {
			IProject project= javaProject.getProject();
			BuildPathsBlock.addJavaNature(project, progress.newChild(1));
			
			// Create the source folder
			IFolder srcFolder = project.getFolder("src");
			if(!srcFolder.exists()) {
				srcFolder.create(true, true, progress.newChild(1));
			}
			progress.setWorkRemaining(3);
			
			// Create the output location
			IFolder outputFolder = project.getFolder("bin");
			if(!outputFolder.exists())
				outputFolder.create(true, true, progress.newChild(1));
			outputFolder.setDerived(true);
			progress.setWorkRemaining(2);
			
			// Set the output location
			javaProject.setOutputLocation(outputFolder.getFullPath(), progress.newChild(1));
			
			// Create classpath entries
			IClasspathEntry[] classpath = new IClasspathEntry[2];
			classpath[0] = JavaCore.newSourceEntry(srcFolder.getFullPath());
			classpath[1] = JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"));
			
			javaProject.setRawClasspath(classpath, progress.newChild(1));
		} catch (OperationCanceledException e) {
			throw new InterruptedException();
		}
	}
}
