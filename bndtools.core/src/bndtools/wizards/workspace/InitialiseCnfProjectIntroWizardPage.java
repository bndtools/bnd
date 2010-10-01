package bndtools.wizards.workspace;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class InitialiseCnfProjectIntroWizardPage extends AbstractInitialisationWizardPage {

    private final String message;

    protected InitialiseCnfProjectIntroWizardPage(String pageName, String message) {
        super(pageName, "Welcome to Bndtools!", AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/bndtools-wizban.png"));
        this.message = message;
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);

        Text text = new Text(composite, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        text.setBackground(parent.getBackground());
        text.setText(message);

        final Button hideWizardButton = new Button(composite, SWT.CHECK);
        hideWizardButton.setText(Messages.InitialiseCnfProjectIntroWizardPage_dontShowLabel);

        hideWizardButton.setSelection(isDontShow());

        hideWizardButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setDontShow(hideWizardButton.getSelection());

                validate();
            }
        });

        GridLayout layout = new GridLayout(1, false);
        composite.setLayout(layout);

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 250;
        text.setLayoutData(gd);

        setControl(composite);
    }
}