package org.bndtools.core.ui.wizards.packge;

import static org.bndtools.refactor.types.PackageInfoRefactorer.FILE_NAME;
import static org.bndtools.refactor.types.PackageInfoRefactorer.ensureThat;
import static org.bndtools.refactor.types.PackageInfoRefactorer.getPackages;

import java.util.Collections;
import java.util.List;

import org.bndtools.core.ui.icons.Icons;
import org.bndtools.core.ui.util.ReflectiveTableViewer;
import org.bndtools.refactor.types.PackageInfoRefactorer.PackageEntry;
import org.bndtools.refactor.util.RefactorAssistant;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import aQute.bnd.osgi.Verifier;
import bndtools.utils.JobSupport;

/**
 *
 *
 */
public class NewPackageInfoWizard extends Wizard implements INewWizard {
	ActivePage			active;
	List<PackageEntry>	packages;

	class ActivePage {
		final Composite								container;
		final WizardPage							page;
		final ReflectiveTableViewer<PackageEntry>	table;

		ActivePage(WizardPage page, Composite parent) {
			this.container = parent;
			this.page = page;

			this.table = new ReflectiveTableViewer<>(parent, SWT.BORDER | SWT.FULL_SELECTION);

			table.checkbox("", 20, pe -> pe.include, (pe, v) -> pe.include = v);
			table.text("Package", 200, pe -> pe.packageName);
			table.checkbox("Export", 50, pe -> pe.export, (pe, v) -> pe.export = v);
			table.text("Version", 200, pe -> pe.version, (pe, v) -> pe.version = v)
					.enabled(pe -> pe.export && pe.include)
					.validate(version -> Verifier.isVersion((String) version) ? null : "invalid version");
			table.checkbox("Provider", 50, pe -> pe.hasProvider, (pe, v) -> pe.hasProvider = v)
					.enabled(pe -> pe.export && pe.include);
			table.build();
			table.add(packages.toArray());
		}

		public boolean isPageComplete() {
			String s = table.validate();
			if (s != null) {
				page.setErrorMessage(s);
				return false;
			} else {
				page.setMessage(null);
				page.setErrorMessage(null);
				return true;
			}
		}
	}

	public NewPackageInfoWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(new WizardPage("package-info.java", "Create package-info.java", Icons.desc("package-info.png")) {

			@Override
			public void createControl(Composite parent) {
				active = new ActivePage(this, parent);
				setControl(active.container);
			}

			@Override
			public boolean isPageComplete() {
				return active.isPageComplete();
			}
		});
	}

	@Override
	public void dispose() {
		super.dispose();
		active = null;
	}

	@Override
	public boolean performFinish() {
		List<PackageEntry> list = packages.stream()
				.filter(pe -> pe.include)
				.toList();
		if (list.isEmpty())
			return true;

		JobSupport.background("Creating package-info.java", mon -> {
			SubMonitor subMonitor = SubMonitor.convert(mon, list.size());

			int i = 0;
			for (PackageEntry ipf : list) {
				if (subMonitor.isCanceled())
					return null;

				subMonitor.setTaskName("Package " + ipf.packageName);

				ICompilationUnit compilationUnit = ensureExists(ipf.package_, subMonitor);
				RefactorAssistant assistant = new RefactorAssistant(compilationUnit);

				ensureThat(assistant, ipf, subMonitor);

				assistant.apply(subMonitor);

				subMonitor.subTask("Created " + ipf.packageName + " " + (i + 1) + " of " + list.size());
				subMonitor.worked(1);

			}
			return null;
		}, x -> {
		});
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		if (selection != null) {
			packages = getPackages(selection.toList());
		} else
			packages = Collections.emptyList();
	}

	/**
	 * Get the package-info.java compilation unit, create if not exists
	 *
	 * @param package_ the parent package
	 * @param monitor a monitor
	 * @return an existing ICompilationUnit
	 * @throws JavaModelException
	 */
	public static ICompilationUnit ensureExists(IPackageFragment package_, IProgressMonitor monitor)
		throws JavaModelException {
		ICompilationUnit compilationUnit = package_.getCompilationUnit(FILE_NAME);
		if (compilationUnit.exists())
			return compilationUnit;

		return package_.createCompilationUnit(FILE_NAME, "package " + package_.getElementName() + ";", true, monitor);
	}

}
