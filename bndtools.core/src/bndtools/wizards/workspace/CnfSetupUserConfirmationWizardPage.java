package bndtools.wizards.workspace;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import bndtools.Plugin;
import bndtools.wizards.workspace.CnfSetupUserConfirmation.Decision;
import bndtools.wizards.workspace.CnfSetupUserConfirmation.Operation;

public class CnfSetupUserConfirmationWizardPage extends WizardPage {

	private CnfSetupUserConfirmation confirmation;

	public CnfSetupUserConfirmationWizardPage(CnfSetupUserConfirmation confirmation) {
		super(CnfSetupUserConfirmationWizardPage.class.getSimpleName());
		setImageDescriptor(Plugin.imageDescriptorFromPlugin("/icons/bndtools-wizban.png")); //$NON-NLS-1$
		this.confirmation = confirmation;
	}

	public void createControl(Composite parent) {
		setControl(parent = new Composite(parent, SWT.NONE));

		Text text = new Text(parent, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
		text.setBackground(parent.getBackground());

		final Button btnSetup = new Button(parent, SWT.RADIO);
		final Button btnSkip = new Button(parent, SWT.RADIO);
		final Button btnNever = new Button(parent, SWT.RADIO);

		// LABELS
		if (confirmation.getOperation() == Operation.CREATE) {
			setTitle(Messages.CnfSetupCreateTitle);
			text.setText(Messages.CnfSetupCreateExplanation);
			btnSetup.setText(Messages.CnfSetupCreate);
			btnSkip.setText(Messages.CnfSetupCreateSkip);
		} else if (confirmation.getOperation() == Operation.UPDATE) {
			setTitle(Messages.CnfSetupUpdateTitle);
			text.setText(Messages.CnfSetupUpdateExplanation);
			btnSetup.setText(Messages.CnfSetupUpdate);
			btnSkip.setText(Messages.CnfSetupUpdateSkip);
		}
		btnNever.setText(Messages.CnfSetupNever);

		// INIT CONTROLS
		btnSetup.setSelection(confirmation.getDecision() == Decision.SETUP);
		btnSkip.setSelection(confirmation.getDecision() == Decision.SKIP);
		btnNever.setSelection(confirmation.getDecision() == Decision.NEVER);

		// EVENTS
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (btnSetup.getSelection())
					confirmation.setDecision(Decision.SETUP);
				else if (btnSkip.getSelection())
					confirmation.setDecision(Decision.SKIP);
				else if (btnNever.getSelection())
					confirmation.setDecision(Decision.NEVER);
			}
		};
		btnSetup.addSelectionListener(listener);
		btnSkip.addSelectionListener(listener);
		btnNever.addSelectionListener(listener);

		// LAYOUT
		GridLayoutFactory.fillDefaults().applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).hint(250, SWT.DEFAULT).applyTo(text);
	}

}
