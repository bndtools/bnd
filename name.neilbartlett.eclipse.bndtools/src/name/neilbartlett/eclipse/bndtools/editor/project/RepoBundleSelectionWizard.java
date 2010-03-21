package name.neilbartlett.eclipse.bndtools.editor.project;

import java.util.Collection;

import name.neilbartlett.eclipse.bndtools.editor.model.VersionedClause;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;

public class RepoBundleSelectionWizard extends Wizard {
	
	private final RepoBundleSelectionWizardPage selectionPage = new RepoBundleSelectionWizardPage("bundleSelect");

	/**
	 * Create a wizard for editing the specified list of bundles. The supplied collection will be modified by this wizard.
	 * @param bundles A mutable collection of bundles.
	 */
	public RepoBundleSelectionWizard(Collection<VersionedClause> bundles) {
		selectionPage.setSelectedBundles(bundles);
		
		addPage(selectionPage);
	}
	@Override
	public boolean performFinish() {
		return true;
	}
	
	public WizardPage getBundleSelectionPage() {
		return selectionPage;
	}
}