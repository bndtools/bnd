package name.neilbartlett.eclipse.bndtools.wizards.newbundle;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.internal.libs.RefCell;
import name.neilbartlett.eclipse.bndtools.internal.pkgselection.IPackageLister;
import name.neilbartlett.eclipse.bndtools.internal.pkgselection.JavaSearchScopePackageLister;
import name.neilbartlett.eclipse.bndtools.wizards.BundleModel;
import name.neilbartlett.eclipse.bndtools.wizards.WizardNewBndFileCreationPage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;

public class WizardNewBndDescriptor extends Wizard implements INewWizard {

	private final BundleModel bundleModel = new BundleModel();
	
	private RefCell<IPackageLister> packageListerRef;
	private RefCell<IJavaProject> javaProjectRef;
	
	private IWorkbench workbench;
	private IStructuredSelection selection;
	
	private WizardNewBndFileCreationPage fileCreationPage;
	private BundleDetailsWizardPage detailsPage;
	private PackagesWizardPage packagesPage;

	private RefCell<IJavaSearchScope> searchScopeRef;

	@Override
	public boolean performFinish() {
		IFile file = fileCreationPage.createNewFile();
		if(file == null) {
			return false;
		}
		
		FileEditorInput input = new FileEditorInput(file);
		try {
			workbench.getActiveWorkbenchWindow().getActivePage().openEditor(input, Plugin.BND_EDITOR_ID);
		} catch (PartInitException e) {
			ErrorDialog.openError(getContainer().getShell(), "Error", null, e.getStatus());
		}
		return true;
	}
	
	@Override
	public void addPages() {
		// The file creation page
		fileCreationPage = new WizardNewBndFileCreationPage("New File", selection, bundleModel);
		fileCreationPage.setFileExtension("bnd");
		addPage(fileCreationPage);
		
		javaProjectRef = new RefCell<IJavaProject>() {
			public IJavaProject getValue() {
				IPath path = fileCreationPage.getContainerFullPath();
				
				IProject project = null;
				IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
				if(container != null) {
					project = container.getProject();
				}
				
				IJavaProject javaProject = null;
				if(project != null) {
					javaProject = JavaCore.create(project);
				}

				return javaProject;
			}
		};
		
		searchScopeRef = new RefCell<IJavaSearchScope>() {
			public IJavaSearchScope getValue() {
				IJavaProject javaProject = javaProjectRef.getValue();
				IJavaSearchScope scope;
				if(javaProject != null) {
					scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject });
				} else {
					scope = SearchEngine.createWorkspaceScope();
				}
				return scope;
			}
		};
		
		packageListerRef = new RefCell<IPackageLister>() {
			public IPackageLister getValue() {
				return new JavaSearchScopePackageLister(searchScopeRef.getValue(), getContainer());
			}
		};
		
		detailsPage = new BundleDetailsWizardPage("Details", "Bundle Details", null, javaProjectRef, searchScopeRef, bundleModel);
		addPage(detailsPage);
		
		packagesPage = new PackagesWizardPage("Packages", "Exported and Private Packages", null, bundleModel, packageListerRef);
		addPage(packagesPage);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

}
