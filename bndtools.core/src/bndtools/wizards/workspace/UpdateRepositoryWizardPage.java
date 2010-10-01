package bndtools.wizards.workspace;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class UpdateRepositoryWizardPage extends AbstractInitialisationWizardPage {

    private boolean performUpdate = true;

    public UpdateRepositoryWizardPage() {
        super("updateRepoPage", "Update Bnd Bundle Repository", AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/bndtools-wizban.png"));
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        setControl(composite);

        Text text = new Text(composite, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        text.setBackground(parent.getBackground());
        text.setText(Messages.UpdateRepositoryWizardPage_updateRepositoryMessage);

        final Button btnUpdate = new Button(composite, SWT.RADIO);
        btnUpdate.setText("Update repository");

        final Button btnSkip = new Button(composite, SWT.RADIO);
        btnSkip.setText("Skip this update");

        final Button btnDontShow = new Button(composite, SWT.RADIO);
        btnDontShow.setText("Don't show this wizard again");

        // INIT CONTROLS
        btnUpdate.setSelection(performUpdate && !isDontShow());
        btnSkip.setSelection(!performUpdate && !isDontShow());
        btnDontShow.setSelection(isDontShow());

        // EVENTS
        Listener l = new Listener() {
            public void handleEvent(Event event) {
                setDontShow(btnDontShow.getSelection());
                performUpdate = btnUpdate.getSelection();

                validate();
            }
        };
        btnUpdate.addListener(SWT.Selection, l);
        btnSkip.addListener(SWT.Selection, l);
        btnDontShow.addListener(SWT.Selection, l);

        // LAYOUT
        composite.setLayout(new GridLayout(1, false));

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 250;
        text.setLayoutData(gd);
    }

    @Override
    protected String getWarning() {
        String warning = super.getWarning();
        if(warning == null && !performUpdate) {
            warning = "WARNING: Bndtools will not prompt to update the repository until the next version of Bndtools is installed.";
        }
        return warning;
    }

}
