package name.neilbartlett.eclipse.bndtools.editor.project;

import java.util.Collection;

import name.neilbartlett.eclipse.bndtools.editor.model.VersionedClause;

import org.eclipse.jface.wizard.Wizard;

public class RepoBundleSelectionWizard extends Wizard {
	
	private final Collection<? super VersionedClause> bundles;
	private RepoBundleSelectionWizardPage selectionPage;

	/**
	 * Create a wizard for editing the specified list of bundles. The supplied collection will be modified by this wizard.
	 * @param bundles A mutable collection of bundles.
	 */
	public RepoBundleSelectionWizard(Collection<? super VersionedClause> bundles) {
		this.bundles = bundles;
	}
	@Override
	public void addPages() {
		selectionPage = new RepoBundleSelectionWizardPage("bundleSelect");
		selectionPage.setSelectedBundles(bundles);
		addPage(selectionPage);
	}
	@Override
	public boolean performFinish() {
		return true;
	}
}