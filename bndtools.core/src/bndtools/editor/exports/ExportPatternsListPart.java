package bndtools.editor.exports;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.editor.contents.PackageInfoDialog;
import bndtools.editor.contents.PackageInfoDialog.FileVersionTuple;
import bndtools.editor.contents.PackageInfoStyle;
import bndtools.editor.pkgpatterns.PkgPatternsListPart;
import bndtools.internal.pkgselection.IPackageFilter;
import bndtools.internal.pkgselection.JavaSearchScopePackageLister;
import bndtools.internal.pkgselection.PackageSelectionDialog;
import bndtools.preferences.BndPreferences;

public class ExportPatternsListPart extends PkgPatternsListPart<ExportedPackage> {
	private static final ILogger logger = Logger.getLogger(ExportPatternsListPart.class);

	public ExportPatternsListPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style, Constants.EXPORT_PACKAGE, "Export Packages", new ExportedPackageLabelProvider());
	}

	@Override
	protected Collection<ExportedPackage> generateClauses() {
		return selectPackagesToAdd();
	}

	protected List<ExportedPackage> selectPackagesToAdd() {
		List<ExportedPackage> added = null;

		final IPackageFilter filter = packageName -> {
			if (packageName.equals("java") || packageName.startsWith("java."))
				return false;

			// TODO: check already included patterns

			return true;
		};

		IFormPage page = (IFormPage) getManagedForm().getContainer();
		IWorkbenchWindow window = page.getEditorSite()
			.getWorkbenchWindow();

		// Prepare the package lister from the Java project
		IProject project = ResourceUtil.getResource(page.getEditorInput())
			.getProject();
		IJavaProject javaProject = JavaCore.create(project);

		IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] {
			javaProject
		});
		JavaSearchScopePackageLister packageLister = new JavaSearchScopePackageLister(searchScope, window);

		// Create and open the dialog
		PackageSelectionDialog dialog = new PackageSelectionDialog(window.getShell(), packageLister, filter,
			"Select new packages to export from the bundle.");
		dialog.setSourceOnly(true);
		dialog.setMultipleSelection(true);
		if (dialog.open() == Window.OK) {
			Object[] results = dialog.getResult();
			added = new LinkedList<>();

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
		List<FileVersionTuple> missingPkgInfoDirs;
		try {
			missingPkgInfoDirs = new ArrayList<>(findSourcePackagesWithoutPackageInfo(pkgs));
		} catch (Exception e) {
			ErrorDialog.openError(getManagedForm().getForm()
				.getShell(), "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error finding source package for exported 1packages.",
					e));
			missingPkgInfoDirs = Collections.emptyList();
		}
		List<FileVersionTuple> generatePkgInfoDirs = new ArrayList<>(missingPkgInfoDirs.size());

		BndPreferences prefs = new BndPreferences();
		boolean noAskPackageInfo = prefs.getNoAskPackageInfo();

		if (noAskPackageInfo || missingPkgInfoDirs.isEmpty()) {
			generatePkgInfoDirs.addAll(missingPkgInfoDirs);
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
			ErrorDialog.openError(getManagedForm().getForm()
				.getShell(), "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error generated packageinfo files.", e));
		}

		// Actually add the new exports
		super.doAddClauses(pkgs, index, select);
	}

	private Collection<FileVersionTuple> findSourcePackagesWithoutPackageInfo(
		Collection<? extends ExportedPackage> pkgs) throws Exception {
		Map<String, FileVersionTuple> result = new HashMap<>();

		Project project = getProject();

		if (project != null) {
			Collection<File> sourceDirs = project.getSourcePath();
			for (File sourceDir : sourceDirs) {
				for (ExportedPackage pkg : pkgs) {
					if (!result.containsKey(pkg.getName())) {
						File pkgDir = new File(sourceDir, pkg.getName()
							.replace('.', '/'));
						if (pkgDir.isDirectory()) {
							PackageInfoStyle existingPkgInfo = PackageInfoStyle.findExisting(pkgDir);
							if (existingPkgInfo == null)
								result.put(pkg.getName(), new FileVersionTuple(pkg.getName(), pkgDir));
						}
					}
				}
			}
		}

		return result.values();
	}

	private static void generatePackageInfos(final Collection<? extends FileVersionTuple> pkgs) throws CoreException {
		final IWorkspaceRunnable wsOperation = monitor -> {
			SubMonitor progress = SubMonitor.convert(monitor, pkgs.size());
			MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0,
				"Errors occurred while creating packageinfo files.", null);
			for (FileVersionTuple pkg : pkgs) {
				IContainer[] locations = ResourcesPlugin.getWorkspace()
					.getRoot()
					.findContainersForLocationURI(pkg.getFile()
						.toURI());
				if (locations != null && locations.length > 0) {
					IContainer container = locations[0];

					PackageInfoStyle packageInfoStyle = PackageInfoStyle
						.calculatePackageInfoStyle(container.getProject());
					IFile pkgInfoFile = container.getFile(new Path(packageInfoStyle.getFileName()));

					try {
						String formattedPackageInfo = packageInfoStyle.format(pkg.getVersion(), pkg.getName());
						ByteArrayInputStream input = new ByteArrayInputStream(formattedPackageInfo.getBytes("UTF-8"));
						if (pkgInfoFile.exists())
							pkgInfoFile.setContents(input, false, true, progress.newChild(1, 0));
						else
							pkgInfoFile.create(input, false, progress.newChild(1, 0));
					} catch (CoreException e1) {
						status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
							"Error creating file " + pkgInfoFile.getFullPath(), e1));
					} catch (UnsupportedEncodingException e2) {
						/* just ignore, should never happen */
					}
				}
			}

			if (!status.isOK())
				throw new CoreException(status);
		};
		IRunnableWithProgress uiOperation = monitor -> {
			try {
				ResourcesPlugin.getWorkspace()
					.run(wsOperation, monitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		};
		try {
			PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow()
				.run(true, true, uiOperation);
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
			File bndFile = model.getBndResource();
			IPath path = Central.toPath(bndFile);
			IFile resource = ResourcesPlugin.getWorkspace()
				.getRoot()
				.getFile(path);
			project = Central.getProject(resource.getProject());
		} catch (Exception e) {
			logger.logError("Error getting project from editor model", e);
		}
		return project;
	}

}
