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
package bndtools.editor.exports;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.ResourceUtil;
import org.osgi.framework.Version;

import bndtools.editor.exports.ExportedPackageSelectionDialog.ExportVersionPolicy;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.model.ExportedPackage;
import bndtools.editor.pkgpatterns.PkgPatternsListPart;
import bndtools.internal.pkgselection.IPackageFilter;
import bndtools.internal.pkgselection.JavaSearchScopePackageLister;

import aQute.lib.osgi.Constants;

public class ExportPatternsListPart extends PkgPatternsListPart<ExportedPackage> {

	public static final Version DEFAULT_BUNDLE_VERSION = new Version(0, 0, 0);
	
	public ExportPatternsListPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style, Constants.EXPORT_PACKAGE);
	}
	
	@Override
	protected Collection<ExportedPackage> generateClauses() {
		return selectPackagesToAdd();
	}
	protected List<ExportedPackage> selectPackagesToAdd() {
		List<ExportedPackage> added = null;
		
		final IPackageFilter filter = new IPackageFilter() {
			public boolean select(String packageName) {
				if(packageName.equals("java") || packageName.startsWith("java."))
					return false;
				
				// TODO: check already included patterns
				
				return true;
			}
		};
		
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		IWorkbenchWindow window = page.getEditorSite().getWorkbenchWindow();

		// Prepare the package lister from the Java project
		IProject project = ResourceUtil.getResource(page.getEditorInput()).getProject();
		IJavaProject javaProject = JavaCore.create(project);
		
		IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject });
		JavaSearchScopePackageLister packageLister = new JavaSearchScopePackageLister(searchScope, window);
		
		// Create and open the dialog
		ExportedPackageSelectionDialog dialog = new ExportedPackageSelectionDialog(window.getShell(), packageLister, filter, "Select new packages to export from the bundle.");
		dialog.setSourceOnly(true);
		dialog.setMultipleSelection(true);
		if(dialog.open() == Window.OK) {
			Object[] results = dialog.getResult();
			added = new LinkedList<ExportedPackage>();
			boolean usedBundleVersionMacro = false;
			
			// Get the version string
			ExportVersionPolicy versionPolicy = dialog.getExportVersionPolicy();
			String version = null;
			if(versionPolicy == ExportVersionPolicy.linkWithBundle) {
				version = BndEditModel.BUNDLE_VERSION_MACRO;
				usedBundleVersionMacro = true;
			} else if(versionPolicy == ExportVersionPolicy.specified) {
				version = dialog.getSpecifiedVersion();
			}
			
			// Select the results
			for (Object result : results) {
				String newPackageName = (String) result;
				ExportedPackage newPackage = new ExportedPackage(newPackageName, new HashMap<String, String>());
				newPackage.getAttribs().put(org.osgi.framework.Constants.VERSION_ATTRIBUTE, version);
				added.add(newPackage);
			}
			
			// Make sure that the bundle has a version when the Bundle-Version macro was used
			if(usedBundleVersionMacro) {
				BndEditModel model = (BndEditModel) getManagedForm().getInput();
				String bundleVersion = model.getBundleVersionString();
				if(bundleVersion == null) {
					model.setBundleVersion(DEFAULT_BUNDLE_VERSION.toString());
				}
			}
		}
		return added;
	}
	@Override
	protected ExportedPackage newHeaderClause(String text) {
		return new ExportedPackage(text, new HashMap<String, String>());
	}
	@Override
	protected List<ExportedPackage> loadFromModel(BndEditModel model) {
		return model.getExportedPackages();
	}
	@Override
	protected void saveToModel(BndEditModel model, List<? extends ExportedPackage> clauses) {
		model.setExportedPackages(clauses);
	}
}
