package org.bndtools.core.ui.wizards.shared;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;

public interface ISkippingWizard extends IWizard {
	// Implementers should override and delegate to super.getPreviousPage()
	IWizardPage getUnfilteredPreviousPage(IWizardPage page);

	// Implementers should override and delegate to super.getNextPage()
	IWizardPage getUnfilteredNextPage(IWizardPage page);

	@Override
	default IWizardPage getPreviousPage(IWizardPage page) {
		return getPreviousPage(this, page);
	}

	@Override
	default IWizardPage getNextPage(IWizardPage page) {
		return getNextPage(this, page);
	}

	static IWizardPage getPreviousPage(ISkippingWizard wizard, IWizardPage page) {
		IWizardPage prev = wizard.getUnfilteredPreviousPage(page);
		if (prev instanceof ISkippableWizardPage) {
			if (((ISkippableWizardPage) prev).shouldSkip()) {
				return wizard.getUnfilteredPreviousPage(prev);
			}
		}
		return prev;
	}

	static IWizardPage getNextPage(ISkippingWizard wizard, IWizardPage page) {
		IWizardPage next = wizard.getUnfilteredNextPage(page);
		if (next instanceof ISkippableWizardPage) {
			if (((ISkippableWizardPage) next).shouldSkip()) {
				return wizard.getUnfilteredNextPage(next);
			}
		}
		return next;
	}
}
