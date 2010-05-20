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
package bndtools.wizards.project;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;

@SuppressWarnings("restriction")
class NewBndProjectWizard extends JavaProjectWizard {

	private final NewBndProjectWizardPageOne pageOne;
	private final NewBndProjectWizardBundlesPage bundlesPage;
	private final NewJavaProjectWizardPageTwo pageTwo;

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
			// Create the cnf project, if not already created
			bundlesPage.doCreateCnfIfNeeded();

			// Generate the bnd.bnd content
			BndEditModel bndModel = new BndEditModel();
			bndModel.setBuildPath(bundlesPage.getSelectedBundles());
			IDocument document = new Document();
			bndModel.saveChangesTo(document);
			final ByteArrayInputStream bndInput = new ByteArrayInputStream(document.get().getBytes());

			// Add the bnd.bnd and build.xml files to the new project
			final IJavaProject javaProj = (IJavaProject) getCreatedElement();
			final IWorkspaceRunnable op = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
				    SubMonitor progress = SubMonitor.convert(monitor, 2);

					IFile bndBndFile = javaProj.getProject().getFile(Project.BNDFILE);
					if(bndBndFile.exists()) {
						bndBndFile.setContents(bndInput, false, false, progress.newChild(1));
					} else {
						bndBndFile.create(bndInput, false, progress.newChild(1));
					}
					IFile buildXmlFile = javaProj.getProject().getFile("build.xml");
					InputStream buildXmlInput = getClass().getResourceAsStream("template_bnd_build.xml");
					if(buildXmlFile.exists()) {
					    buildXmlFile.setContents(buildXmlInput, false, false, progress.newChild(1));
					} else {
					    buildXmlFile.create(buildXmlInput, false, progress.newChild(1));
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
