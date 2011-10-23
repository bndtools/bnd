package bndtools.wizards.workspace;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import bndtools.Plugin;
import bndtools.types.Pair;
import bndtools.utils.SWTConcurrencyUtil;
import bndtools.wizards.workspace.CnfSetupTask.CnfStatus;
import bndtools.wizards.workspace.CnfSetupUserConfirmation.Decision;

public class CnfSetupWizard extends Wizard {

    private CnfSetupUserConfirmation confirmation;

    private final CnfSetupUserConfirmationWizardPage confirmPage;
    private final CnfImportOrOpenWizardPage importPage;
    private final CnfTemplateSelectionWizardPage templatePage = new CnfTemplateSelectionWizardPage();

    private RequiredOperation operation;
    private final IPath cnfPath;

    public enum RequiredOperation {
        Nothing, Open, Import, Create
    }

    public CnfSetupWizard(CnfSetupUserConfirmation confirmation, RequiredOperation operation, IPath cnfPath) {
        this.confirmation = confirmation;
        this.cnfPath = cnfPath;
        importPage = new CnfImportOrOpenWizardPage(cnfPath);
        this.confirmPage = new CnfSetupUserConfirmationWizardPage(confirmation);

        this.operation = operation;

        setNeedsProgressMonitor(true);

        addPage(confirmPage);
        if (operation == RequiredOperation.Import) {
            importPage.setOperation(operation);
            addPage(importPage);
        }

        addPage(templatePage);

        confirmation.addPropertyChangeListener("decision", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateUi();
            }
        });
        importPage.addPropertyChangeListener(CnfImportOrOpenWizardPage.PROP_OPERATION, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                CnfSetupWizard.this.operation = importPage.getOperation();
                updateUi();
            }
        });
        templatePage.addPropertyChangeListener(CnfTemplateSelectionWizardPage.PROP_ELEMENT, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateUi();
            }
        });
    }

    private void updateUi() {
        IWizardContainer container = getContainer();
        if (container != null) {
            container.updateButtons();
            container.updateMessage();
        }
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        IWizardPage next = null;

        if (confirmPage == page) {
            if (confirmation.getDecision() == Decision.SETUP)
                next = operation == RequiredOperation.Import ? importPage : templatePage;
            else
                next = null;
        }
        else if (importPage == page)
            next = operation == RequiredOperation.Create ? templatePage : null;

        return next;
    }

    @Override
    public boolean canFinish() {
        if (confirmation.getDecision() != Decision.SETUP)
            return true;

        if (operation == RequiredOperation.Import)
            return importPage.isPageComplete();

        if (operation == RequiredOperation.Create)
            return templatePage.isPageComplete();

        return false;
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
        final Pair<RequiredOperation,IPath> cnf = determineNecessaryOperation(overridePreference);
        RequiredOperation operation = cnf.getFirst();
        if (operation == RequiredOperation.Nothing)
            return false;

        if (operation == RequiredOperation.Open) {
            try {
                new CnfSetupTask(operation, null).run(null);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }

        SWTConcurrencyUtil.execForDisplay(PlatformUI.getWorkbench().getDisplay(), true, new Runnable() {
            public void run() {
                final CnfSetupWizard wizard = new CnfSetupWizard(new CnfSetupUserConfirmation(), cnf.getFirst(), cnf.getSecond());
                // Modified wizard dialog -- change "Finish" to "OK"
                WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
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

    private static Pair<RequiredOperation, IPath> determineNecessaryOperation(boolean overridePreference) {
        if (!overridePreference && isDisabled())
            return Pair.newInstance(RequiredOperation.Nothing, null);

        Pair<CnfStatus, IPath> cnf = CnfSetupTask.getWorkspaceCnfStatus();
        RequiredOperation operation = RequiredOperation.Nothing;

        switch (cnf.getFirst()) {
        case ImportedOpen:
            operation = RequiredOperation.Nothing;
            break;
        case ImportedClosed:
            operation = RequiredOperation.Open;
            break;
        case DirExists:
            operation = RequiredOperation.Import;
            break;
        case NotExists:
            operation = RequiredOperation.Create;
            break;
        }

        return Pair.newInstance(operation, cnf.getSecond());
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
            return true;
        }

        try {
            getContainer().run(false, false, new CnfSetupTask(operation, templatePage.getSelectedElement()));
            return true;
        } catch (InvocationTargetException e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error creating workspace configuration project.",
                    e.getCause()));
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