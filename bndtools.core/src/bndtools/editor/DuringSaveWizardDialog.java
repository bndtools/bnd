package bndtools.editor;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Just a WizardDialog with custom button labels to make them more suitable for
 * a mid-save popup.
 */
public class DuringSaveWizardDialog extends WizardDialog {

	public DuringSaveWizardDialog(Shell parentShell, IWizard wizard) {
		super(parentShell, wizard);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		updateButtonText(IDialogConstants.CANCEL_ID, "Abort Save");
		updateButtonText(IDialogConstants.FINISH_ID, "Save Anyway");
	}

	private void updateButtonText(int buttonId, String text) {
		Button button = getButton(buttonId);
		if (button != null) {
			String oldText = button.getText();
			if (!text.equals(oldText)) {
				button.setText(text);
				// Re-Layout required because width probably changed.
				setButtonLayoutData(button);
			}
		}
	}

}
