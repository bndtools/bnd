package bndtools.wizards.workspace;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import bndtools.Plugin;

public class InitialiseCnfProjectIntroWizardPage extends WizardPage {

    protected InitialiseCnfProjectIntroWizardPage(String pageName) {
        super(pageName);
        setPageComplete(false);
    }

    public void createControl(Composite parent) {
        setTitle(Messages.InitialiseCnfProjectIntroWizardPage_title);

        Composite composite = new Composite(parent, SWT.NONE);

        Text text = new Text(composite, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        text.setBackground(parent.getBackground());
        text.setText(Messages.InitialiseCnfProjectIntroWizardPage_message);

        final Button hideWizardButton = new Button(composite, SWT.CHECK);
        hideWizardButton.setText("Do not show this wizard again.");

        hideWizardButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean hide = hideWizardButton.getSelection();

                IPreferenceStore store = Plugin.getDefault().getPreferenceStore();
                store.setValue(Plugin.PREF_HIDE_INITIALISE_CNF_WIZARD, hide);

                if(hide) {
                    setMessage("Warning: Bndtools will no longer check for the Bnd configuration project when creating a new Bnd project.", IMessageProvider.WARNING);
                } else {
                    setMessage(null);
                }
            }
        });

        GridLayout layout = new GridLayout(1, false);
        composite.setLayout(layout);

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 250;
        text.setLayoutData(gd);

        setControl(composite);
    }

    @Override
    public void setVisible(boolean visible) {
        // Allow the wizard to complete once we have left this page
        if(!visible)
            setPageComplete(true);

        super.setVisible(visible);
    }

    @Override
    public boolean canFlipToNextPage() {
        return getNextPage() != null;
    }
}
