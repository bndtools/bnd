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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.ResourceUtil;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.osgi.Constants;
import aQute.libg.header.Attrs;
import bndtools.Plugin;
import bndtools.editor.contents.PackageInfoDialog;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.pkgpatterns.PkgPatternsListPart;
import bndtools.internal.pkgselection.IPackageFilter;
import bndtools.internal.pkgselection.JavaSearchScopePackageLister;
import bndtools.internal.pkgselection.PackageSelectionDialog;
import bndtools.model.clauses.ExportedPackage;
import bndtools.preferences.BndPreferences;

public class ExportPatternsListPart extends PkgPatternsListPart<ExportedPackage> {

	private static final String PACKAGEINFO = "packageinfo";

    public ExportPatternsListPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style, Constants.EXPORT_PACKAGE, "Export Packages", new ExportedPackageLabelProvider());
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
				ExportedPackage newPackage = new ExportedPackage(newPackageName, new Attrs());
				added.add(newPackage);
			}
		}
		return added;
	}

	@Override
	protected void doAddClauses(Collection<? extends ExportedPackage> pkgs, int index, boolean select) {
	    Map<String, File> missingPkgInfoDirs;
        try {
            missingPkgInfoDirs = findSourcePackagesWithoutPackageInfo(pkgs);
        } catch (Exception e) {
            ErrorDialog.openError(getManagedForm().getForm().getShell(), "Error", null,
                    new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error finding source package for exported 1packages.", e));
            missingPkgInfoDirs = Collections.emptyMap();
        }
	    Collection<File> generatePkgInfoDirs = new ArrayList<File>(missingPkgInfoDirs.size());

	    BndPreferences prefs = new BndPreferences();
	    boolean noAskPackageInfo = prefs.getNoAskPackageInfo();

	    if(noAskPackageInfo || missingPkgInfoDirs.isEmpty()) {
	        generatePkgInfoDirs.addAll(missingPkgInfoDirs.values());
	    } else {
	        PackageInfoDialog dlg = new PackageInfoDialog(getSection().getShell(), missingPkgInfoDirs);
	        if (dlg.open() == Window.CANCEL)
	            return;
	        prefs.setNoAskPackageInfo(dlg.isDontAsk());
	        generatePkgInfoDirs.addAll(dlg.getSelectedPackageDirs());
	    }

	    try {
            generatePackageInfos(generatePkgInfoDirs);
        } catch (CoreException e) {
            ErrorDialog.openError(getManagedForm().getForm().getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error generated packageinfo files.", e));
        }

		// Actually add the new exports
		super.doAddClauses(pkgs, index, select);
	}


    private Map<String, File> findSourcePackagesWithoutPackageInfo(Collection<? extends ExportedPackage> pkgs) throws Exception {
        Map<String, File> result = new HashMap<String, File>();

        Collection<File> sourceDirs = getProject().getSourcePath();
        for (File sourceDir : sourceDirs) {
            for (ExportedPackage pkg : pkgs) {
                if (!result.containsKey(pkg.getName())) {
                    File pkgDir = new File(sourceDir, pkg.getName().replace('.', '/'));
                    if (pkgDir.isDirectory()) {
                        File pkgInfo = new File(pkgDir, PACKAGEINFO);
                        if (!pkgInfo.exists())
                            result.put(pkg.getName(), pkgDir);
                    }
                }
            }
        }

        return result;
    }

    private void generatePackageInfos(final Collection<? extends File> pkgDirs) throws CoreException {
        final IWorkspaceRunnable wsOperation = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                SubMonitor progress = SubMonitor.convert(monitor, pkgDirs.size());
                MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Errors occurred while creating packageinfo files.", null);
                for (File pkgDir : pkgDirs) {
                    IContainer[] locations = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(pkgDir.toURI());
                    if (locations != null && locations.length > 0) {
                        IFile pkgInfoFile = locations[0].getFile(new Path(PACKAGEINFO));

                        ByteArrayInputStream input = new ByteArrayInputStream("version 1.0".getBytes());
                        try {
                            pkgInfoFile.create(input, false, progress.newChild(1, 0));
                        } catch (CoreException e) {
                            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error creating file " + pkgInfoFile.getFullPath(), e));
                        }
                    }
                }

                if (!status.isOK()) throw new CoreException(status);
            }
        };
        IRunnableWithProgress uiOperation = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    ResourcesPlugin.getWorkspace().run(wsOperation, monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                }
            }
        };
        try {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, true, uiOperation);
        } catch (InvocationTargetException e) {
            throw (CoreException) e.getTargetException();
        } catch (InterruptedException e) {
            // ignore
        }
    }

	@Override
	protected ExportedPackage newHeaderClause(String text) {
		return new ExportedPackage(text, new Attrs());
	}
	@Override
	protected List<ExportedPackage> loadFromModel(BndEditModel model) {
		return model.getExportedPackages();
	}
	@Override
	protected void saveToModel(BndEditModel model, List<? extends ExportedPackage> clauses) {
		model.setExportedPackages(clauses);
	}

    Project getProject() {
        Project project = null;
        try {
            BndEditModel model = (BndEditModel) getManagedForm().getInput();
            File projectDir = model.getBndResource().getProject().getLocation().toFile();
            project = Workspace.getProject(projectDir);
        } catch (Exception e) {
            Plugin.logError("Error getting project from editor model", e);
        }
        return project;
    }


}
