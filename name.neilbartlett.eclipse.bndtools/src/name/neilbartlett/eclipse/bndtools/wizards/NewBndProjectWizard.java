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

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import aQute.bnd.build.Project;

@SuppressWarnings("restriction")
class NewBndProjectWizard extends JavaProjectWizard {

	private final NewBndProjectWizardPageOne pageOne;
	private final NewJavaProjectWizardPageTwo pageTwo;
	private final NewBndProjectWizardBundlesPage bundlesPage;

	NewBndProjectWizard(NewBndProjectWizardPageOne pageOne, NewBndProjectWizardBundlesPage bundlesPage, NewJavaProjectWizardPageTwo pageTwo) {
		super(pageOne, pageTwo);
		setWindowTitle("New Bnd OSGi Project");
		
		this.pageOne = pageOne;
		this.bundlesPage = bundlesPage;
		this.pageTwo = pageTwo;
	}
	
	@Override
	public void addPages() {
		addPage(pageOne);
		addPage(bundlesPage);
		addPage(pageTwo);
	}
	
	
	@Override
	public boolean performFinish() {
		boolean result = super.performFinish();
		if(result) {
			final IJavaProject javaProj = (IJavaProject) getCreatedElement();
			
			final IWorkspaceRunnable op = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					IFile bndBndFile = javaProj.getProject().getFile(Project.BNDFILE);
					if(!bndBndFile.exists()) {
						bndBndFile.create(new ByteArrayInputStream(new byte[0]), false, monitor);
					}
				}
			};
			try {
				getContainer().run(false, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							javaProj.getProject().getWorkspace().run(op, monitor);
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
					}
				});
				result = true;
			} catch (InvocationTargetException e) {
				ErrorDialog.openError(getShell(), "Error", "", new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error creating Bnd project descriptor file ({0}).", Project.BNDFILE), e.getTargetException()));
				result = false;
			} catch (InterruptedException e) {
				// Shouldn't happen
			}
		}
		return result;
	}
}
