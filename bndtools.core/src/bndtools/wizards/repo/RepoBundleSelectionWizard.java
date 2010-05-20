package bndtools.wizards.repo;

import java.util.List;

import org.eclipse.jface.wizard.Wizard;

import bndtools.model.clauses.VersionedClause;

public class RepoBundleSelectionWizard extends Wizard {

	private final RepoBundleSelectionWizardPage selectionPage = new RepoBundleSelectionWizardPage("bundleSelect");

	/**
	 * Create a wizard for editing the specified list of bundles. The supplied collection will be modified by this wizard.
	 * @param bundles A mutable collection of bundles.
	 */
	public RepoBundleSelectionWizard(List<VersionedClause> bundles) {
		selectionPage.setSelectedBundles(bundles);

		addPage(selectionPage);
	}
	@Override
	public boolean performFinish() {
		return true;
	}

	public RepoBundleSelectionWizardPage getBundleSelectionPage() {
		return selectionPage;
	}
}