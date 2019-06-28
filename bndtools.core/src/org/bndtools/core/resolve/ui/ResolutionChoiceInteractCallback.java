package org.bndtools.core.resolve.ui;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.bndtools.core.resolve.ResolveCancelledException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import biz.aQute.resolve.ResolutionCallback;

public class ResolutionChoiceInteractCallback implements ResolutionCallback {

	@Override
	public void processCandidates(final Requirement requirement, Set<Capability> wired,
		final List<Capability> candidates) {
		if (wired.size() > 0 || candidates.size() < 2)
			return;

		final Display display = PlatformUI.getWorkbench()
			.getDisplay();
		final AtomicInteger resultRef = new AtomicInteger();

		Runnable runnable = () -> {
			Shell shell = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow()
				.getShell();
			if (!shell.isDisposed()) {
				ResolutionChoiceSelectionDialog dialog = new ResolutionChoiceSelectionDialog(shell, requirement,
					candidates);
				resultRef.set(dialog.open());
			} else {
				resultRef.set(IDialogConstants.CANCEL_ID);
			}
		};

		display.syncExec(runnable);
		int result = resultRef.get();

		if (result == IDialogConstants.CANCEL_ID)
			throw new ResolveCancelledException();
	}

}
