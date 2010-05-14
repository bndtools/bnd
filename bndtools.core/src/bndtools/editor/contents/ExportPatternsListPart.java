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
package bndtools.editor.contents;

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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.ResourceUtil;

import aQute.lib.osgi.Constants;
import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.model.ExportedPackage;
import bndtools.editor.pkgpatterns.PkgPatternsListPart;
import bndtools.internal.pkgselection.IPackageFilter;
import bndtools.internal.pkgselection.JavaSearchScopePackageLister;
import bndtools.internal.pkgselection.PackageSelectionDialog;
import bndtools.pieces.ExportVersionPolicy;

public class ExportPatternsListPart extends PkgPatternsListPart<ExportedPackage> {

	public ExportPatternsListPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style, Constants.EXPORT_PACKAGE, "Export Patterns");
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
        PackageSelectionDialog dialog = new PackageSelectionDialog(window.getShell(), packageLister, filter, "Select new packages to export from the bundle.");
		dialog.setSourceOnly(true);
		dialog.setMultipleSelection(true);
		if(dialog.open() == Window.OK) {
			Object[] results = dialog.getResult();
			added = new LinkedList<ExportedPackage>();

			// Select the results
			for (Object result : results) {
				String newPackageName = (String) result;
				ExportedPackage newPackage = new ExportedPackage(newPackageName, new HashMap<String, String>());
				added.add(newPackage);
			}
		}
		return added;
	}

	@Override
	protected void doAddClauses(Collection<? extends ExportedPackage> pkgs, int index, boolean select) {
		ExportVersionPolicy policy = null;
		String specificVersion = null;

		// Load the defaults from prefs
		IPreferenceStore store = Plugin.getDefault().getPreferenceStore();
		boolean noAskPolicy = store.getBoolean(Plugin.PREF_NOASK_EXPORT_VERSION);
		String policyStr = store.getString(Plugin.PREF_DEFAULT_EXPORT_VERSION_POLICY);
		try {
			policy = Enum.valueOf(ExportVersionPolicy.class, policyStr);
		} catch (IllegalArgumentException e) {
			policy = ExportVersionPolicy.linkWithBundle;
		}
		specificVersion = store.getString(Plugin.PREF_DEFAULT_EXPORT_VERSION);
		if(specificVersion == null || specificVersion.length() == 0) {
			specificVersion = Plugin.DEFAULT_VERSION.toString();
		}

		// Allow the user to change the defaults
		if(!noAskPolicy) {
			QueryExportVersionPolicyDialog dialog = new QueryExportVersionPolicyDialog(getSection().getShell());
			dialog.setExportVersionPolicy(policy);
			dialog.setSpecifiedVersion(specificVersion);

			if(Window.CANCEL == dialog.open()) {
				return;
			}
			policy = dialog.getExportVersionPolicy();
			specificVersion = dialog.getSpecifiedVersion();
			if(dialog.getToggleState()) {
				store.setValue(Plugin.PREF_NOASK_EXPORT_VERSION, true);
				store.setValue(Plugin.PREF_DEFAULT_EXPORT_VERSION_POLICY, policy.toString());
				store.setValue(Plugin.PREF_DEFAULT_EXPORT_VERSION, specificVersion);
			}
		}

		// Use the values provided to modify the export list
		String selectedVersion = null;
		if(policy == ExportVersionPolicy.linkWithBundle) {
			selectedVersion = BndEditModel.BUNDLE_VERSION_MACRO;
			addDefaultBundleVersion();
		} else if(policy == ExportVersionPolicy.specified) {
			selectedVersion = specificVersion;
		}
		if(selectedVersion != null) {
			for (ExportedPackage pkg : pkgs) {
				pkg.setVersionString(selectedVersion);
			}
		}

		// Actually add the new exports
		super.doAddClauses(pkgs, index, select);
	}


	private void addDefaultBundleVersion() {
		BndEditModel model = (BndEditModel) getManagedForm().getInput();
		String bundleVersion = model.getBundleVersionString();
		if(bundleVersion == null) {
			model.setBundleVersion(Plugin.DEFAULT_VERSION.toString());
		}
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
