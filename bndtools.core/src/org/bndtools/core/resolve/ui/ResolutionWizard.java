package org.bndtools.core.resolve.ui;

import java.util.Collections;

import org.bndtools.core.resolve.ResolutionResult;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.help.instructions.ResolutionInstructions.ResolveMode;
import biz.aQute.resolve.RunResolution;

public class ResolutionWizard extends Wizard {
	private final ResolutionResultsWizardPage	resultsPage;
	private final BndEditModel					model;
	private boolean								preserveRunBundleUnresolved;

	@SuppressWarnings("unused")
	public ResolutionWizard(BndEditModel model, IFile file, ResolutionResult result) {
		this.model = model;

		resultsPage = new ResolutionResultsWizardPage(model);
		resultsPage.setResult(result);

		setWindowTitle("Resolve");
		setNeedsProgressMonitor(true);

		addPage(resultsPage);
	}

	@Override
	public boolean performFinish() {
		ResolutionResult result = resultsPage.getResult();

		if (result != null && result.getOutcome() == ResolutionResult.Outcome.Resolved) {
			RunResolution resolution = result.getResolution();
			assert resolution.isOK();

			if (model.getResolveMode() == ResolveMode.beforelaunch) {
				resolution.cache();
			} else {
				resolution.updateBundles(model);
			}
		} else {
			if (!preserveRunBundleUnresolved)
				model.setRunBundles(Collections.emptyList());
		}
		return true;
	}

	public void setAllowFinishUnresolved(boolean allowFinishUnresolved) {
		resultsPage.setAllowCompleteUnresolved(allowFinishUnresolved);
	}

	public void setPreserveRunBundlesUnresolved(boolean preserve) {
		this.preserveRunBundleUnresolved = preserve;
	}
}
