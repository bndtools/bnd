package bndtools.wizards.workspace;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import bndtools.LocalRepositoryTasks;
import bndtools.Plugin;
import bndtools.utils.SWTConcurrencyUtil;
import bndtools.wizards.workspace.CnfSetupUserConfirmation.Decision;

public class CnfSetupWizard extends Wizard {

	private CnfSetupUserConfirmation confirmation;

	public CnfSetupWizard(CnfSetupUserConfirmation confirmation) {
		this.setNeedsProgressMonitor(true);
		this.addPage(new CnfSetupUserConfirmationWizardPage(confirmation));
		this.confirmation = confirmation;
	}

    /**
     * Show the wizard if it needs to be shown (i.e. the cnf project does not
     * exist and the preference to show the wizard has not been disabled). This
     * method is safe to call from a non-UI thread.
     *
     * @param overridePreference
     *            If this parameter is {@code true} then the dialog will be
     *            shown irrespective of the workspace preference.
     *
     * @return Whether any dialog or wizard was shown. The false return may be
     *         used to decide whether to give the user alternative feedback.
     */
    public static boolean showIfNeeded(boolean overridePreference) {
        final boolean shouldInstall = determineNecessaryOperation(overridePreference);
        if (!shouldInstall)
            return false;

        SWTConcurrencyUtil.execForDisplay(PlatformUI.getWorkbench().getDisplay(), true, new Runnable() {
            public void run() {
                final CnfSetupWizard wizard = new CnfSetupWizard(new CnfSetupUserConfirmation());
                // Modified wizard dialog -- change "Finish" to "OK"
                WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard) {
                    @Override
                    protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
                        if (id == IDialogConstants.FINISH_ID)
                            label = IDialogConstants.OK_LABEL;
                        return super.createButton(parent, id, label, defaultButton);
                    }
                };
                dialog.open();
            }
        });
        return true;
    }

	private static boolean isDisabled() {
		IPreferenceStore store = Plugin.getDefault().getPreferenceStore();
		return store.getBoolean(Plugin.PREF_HIDE_INITIALISE_CNF_WIZARD);
	}

	private void setDisabled(boolean disabled) {
		IPreferenceStore store = Plugin.getDefault().getPreferenceStore();
		store.setValue(Plugin.PREF_HIDE_INITIALISE_CNF_WIZARD, disabled);
	}

	private static boolean determineNecessaryOperation(boolean overridePreference) {
		if (!overridePreference && isDisabled())
			return false;
		if (!LocalRepositoryTasks.isBndWorkspaceConfigured())
			return true;
		return false;
	}

	@Override
	public boolean performFinish() {
		if (confirmation.getDecision() == Decision.NEVER) {
			if (confirmNever()) {
				setDisabled(true);
				return true;
			} else {
				return false;
			}
		}

		if (confirmation.getDecision() == Decision.SKIP) {
		    /*
			try {
				getContainer().run(false, false, new CnfSetupTask(true));
				return true;
			} catch (InvocationTargetException e) {
				ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error creating workspace configuration project.", e.getCause()));
			} catch (InterruptedException e) {
				// ignore
			}
			*/
		    return true;
		}

		try {
			getContainer().run(false, false, new CnfSetupTask(false));
			return true;
		} catch (InvocationTargetException e) {
			ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error creating workspace configuration project.", e.getCause()));
		} catch (InterruptedException e) {
			// ignore
		}
		return false;
	}

	private boolean confirmNever() {
		IPreferenceStore store = Plugin.getDefault().getPreferenceStore();
		boolean hideWarning = store.getBoolean(Plugin.PREF_HIDE_INITIALISE_CNF_ADVICE);
		if (hideWarning)
			return true;

		MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(getShell(),
				Messages.CnfSetupNeverWarningTitle, Messages.CnfSetupNeverWarning, Messages.DontShowMessageAgain,
				false, null, null);

		if (dialog.getToggleState()) {
			store.setValue(Plugin.PREF_HIDE_INITIALISE_CNF_ADVICE, true);
		}
		return dialog.getReturnCode() == MessageDialogWithToggle.OK;
	}

}