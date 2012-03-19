package bndtools.wizards.workspace;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import aQute.bnd.build.Workspace;
import bndtools.Plugin;
import bndtools.preferences.BndPreferences;

public class CnfSetupWizard extends Wizard {

    private CnfSetupDecision decision;

    private final CnfSetupUserConfirmationWizardPage confirmPage;
    private final CnfImportOrOpenWizardPage importPage;
    private final CnfTemplateSelectionWizardPage templatePage = new CnfTemplateSelectionWizardPage();

    private CnfSetupOperation operation;

    public CnfSetupWizard(CnfSetupOperation op) {
        this.decision = CnfSetupDecision.SETUP;
        this.operation = op;
        setForcePreviousAndNextButtons(true);
        setNeedsProgressMonitor(true);

        confirmPage = new CnfSetupUserConfirmationWizardPage(decision);
        importPage = new CnfImportOrOpenWizardPage();

        addPage(confirmPage);
        addPage(templatePage);
        
        importPage.setWizard(this);
        
        confirmPage.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                decision = confirmPage.getDecision();
                
                if (confirmPage.isCreateInEclipseWorkspace())
                    operation = determineNecessaryOperation(true);
                else
                    operation = determineNecessaryOperation(new Path(confirmPage.getExternalLocation()));

                importPage.setOperation(operation);
                updateUi();
            }
        });
        importPage.addPropertyChangeListener(CnfImportOrOpenWizardPage.PROP_OPERATION, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                operation = importPage.getOperation();
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
            if (decision == CnfSetupDecision.SETUP)
                next = operation.getType() == CnfSetupOperation.Type.Import ? importPage : templatePage;
            else
                next = null;
        }
        else if (importPage == page)
            next = operation.getType() == CnfSetupOperation.Type.Create ? templatePage : null;

        return next;
    }

    @Override
    public boolean canFinish() {
        if (decision != CnfSetupDecision.SETUP)
            return true;

        if (operation.getType() == CnfSetupOperation.Type.Import)
            return importPage.isPageComplete();

        if (operation.getType() == CnfSetupOperation.Type.Create)
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
        // Determine whether anything needs to be done...
        CnfSetupOperation operation = determineNecessaryOperation(overridePreference);
        if (operation.getType() == CnfSetupOperation.Type.Nothing)
            return false;
        if (operation.getType() == CnfSetupOperation.Type.Open) {
            try {
                new CnfSetupTask(operation, null).run(null);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }

        // Open wizard
        CnfSetupWizard wizard = new CnfSetupWizard(operation);
        // Modified wizard dialog -- change "Finish" to "OK"
        WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
        dialog.open();

        return true;
    }

    private static boolean isDisabled() {
        return new BndPreferences().getHideInitCnfWizard();
    }

    private void setDisabled(boolean disabled) {
        new BndPreferences().setHideInitCnfWizard(disabled);
    }
    
    private static boolean isValidCnf(File dir) {
        if (!dir.isDirectory())
            return false;
        
        File buildFile = new File(dir, Workspace.BUILDFILE);
        return buildFile.isFile();
    }
    
    private static CnfSetupOperation determineNecessaryOperation(IPath externalLocation) {
        CnfSetupOperation result;
        
        File file = externalLocation.toFile();
        if (isValidCnf(file))
            result = new CnfSetupOperation(CnfSetupOperation.Type.Import, externalLocation);
        else
            result = new CnfSetupOperation(CnfSetupOperation.Type.Create, externalLocation);
        
        return result;
    }
    
    private static CnfSetupOperation determineNecessaryOperation(boolean overridePreference) {
        if (!overridePreference && isDisabled())
            return CnfSetupOperation.NOTHING;

        CnfInfo info = CnfSetupTask.getWorkspaceCnfInfo();
        IPath location = info.getLocation();

        CnfSetupOperation operation = CnfSetupOperation.NOTHING;
        switch (info.getExistence()) {
        case ImportedOpen:
            operation = new CnfSetupOperation(CnfSetupOperation.Type.Nothing, location);
            break;
        case ImportedClosed:
            operation = new CnfSetupOperation(CnfSetupOperation.Type.Open, null);
            break;
        case Exists:
            operation = new CnfSetupOperation(CnfSetupOperation.Type.Import, location);
            break;
        case None:
            operation = new CnfSetupOperation(CnfSetupOperation.Type.Create, null);
            break;
        }
        return operation;
    }
    
    @Override
    public boolean performFinish() {
        if (decision == CnfSetupDecision.NEVER) {
            if (confirmNever()) {
                setDisabled(true);
                return true;
            } else {
                return false;
            }
        }
        if (decision == CnfSetupDecision.SKIP) {
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
        BndPreferences prefs = new BndPreferences();
        boolean hideWarning = prefs.getHideInitCnfAdvice();
        if (hideWarning)
            return true;

        MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(getShell(), Messages.CnfSetupNeverWarningTitle,
                Messages.CnfSetupNeverWarning, Messages.DontShowMessageAgain, false, null, null);

        if (dialog.getToggleState()) {
            prefs.setHideInitCnfAdvice(true);
        }
        return dialog.getReturnCode() == MessageDialogWithToggle.OK;
    }

}