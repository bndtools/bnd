package name.neilbartlett.eclipse.bndtools.wizards.newbundle;

import name.neilbartlett.eclipse.bndtools.internal.libs.MutableRefCell;
import name.neilbartlett.eclipse.bndtools.internal.libs.RefCell;
import name.neilbartlett.eclipse.bndtools.internal.pkgselection.IPackageLister;
import name.neilbartlett.eclipse.bndtools.wizards.BundleModel;
import name.neilbartlett.eclipse.bndtools.wizards.PackageListGroup;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class PackagesWizardPage extends WizardPage {

	private final BundleModel bundleModel;
	private final RefCell<IPackageLister> packageListerRef;
	private MutableRefCell<Boolean> allExportsRef;

	public PackagesWizardPage(String pageName, String title,
			ImageDescriptor titleImage, final BundleModel bundleModel, RefCell<IPackageLister> packageListerRef) {
		super(pageName, title, titleImage);
		this.bundleModel = bundleModel;
		this.packageListerRef = packageListerRef;
		
		allExportsRef = new MutableRefCell<Boolean>() {
			public synchronized void setValue(Boolean value) {
				bundleModel.setAllExports(value);
			}
			public synchronized Boolean getValue() {
				return bundleModel.isAllExports();
			}
		};
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout(SWT.VERTICAL));
		
		Group exportsGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		exportsGroup.setText("Exported Packages");
		PackageListGroup exportsControls = new PackageListGroup(bundleModel.getExports(), allExportsRef, packageListerRef, true, null);
		exportsControls.createControl(exportsGroup);
		
		Group privatePackagesGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		privatePackagesGroup.setText("Private Packages");
		PackageListGroup privatePackagesControls = new PackageListGroup(bundleModel.getPrivatePackages(), null, packageListerRef, false, bundleModel.getExports());
		privatePackagesControls.createControl(privatePackagesGroup);
		
		setControl(composite);
	}

}
