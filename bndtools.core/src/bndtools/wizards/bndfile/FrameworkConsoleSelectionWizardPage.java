package bndtools.wizards.bndfile;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class FrameworkConsoleSelectionWizardPage extends WizardPage {

    private static final String EQUINOX = "org.eclipse.osgi";
    private static final String FELIX = "org.apache.felix.framework";
    private static final String DEFAULT_FWK = FELIX;

    private String framework = DEFAULT_FWK;
    private boolean console = true;

    boolean shown = false;

    protected FrameworkConsoleSelectionWizardPage(String pageName) {
        super(pageName);
        setTitle("Framework and Console Settings");
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);

        // CONTROLS
        Group fwkGroup = new Group(composite, SWT.NONE);
        fwkGroup.setText("Framework Selection:");

        final Button btnFelix = new Button(fwkGroup, SWT.RADIO);
        btnFelix.setText("Felix");

        final Button btnEquinox = new Button(fwkGroup, SWT.RADIO);
        btnEquinox.setText("Equinox");

        final Button btnConsole = new Button(fwkGroup, SWT.CHECK);
        btnConsole.setText("Enable OSGi console");

        // LOAD
        if(EQUINOX.equals(framework)) {
            btnFelix.setSelection(false);
            btnEquinox.setSelection(true);
        } else {
            // Felix
            btnFelix.setSelection(true);
            btnEquinox.setSelection(false);
        }
        btnConsole.setSelection(console);

        // LISTENERS
        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if(btnEquinox.getSelection())
                    framework = EQUINOX;
                else
                    framework = FELIX;
            }
        };
        btnFelix.addSelectionListener(listener);
        btnEquinox.addSelectionListener(listener);

        btnConsole.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                console = btnConsole.getSelection();
            }
        });

        // LAYOUT
        GridLayout layout;
        GridData gd;

        layout = new GridLayout();
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        fwkGroup.setLayoutData(gd);

        layout = new GridLayout();
        fwkGroup.setLayout(layout);

        setControl(composite);
    }

    @Override
    public boolean isPageComplete() {
        return shown;
    }

    @Override
    public void setVisible(boolean visible) {
        if(visible) {
            shown = true;
        }
        super.setVisible(visible);
        getContainer().updateButtons();
    }

    public boolean getConsole() {
        return console;
    }

    public String getFramework() {
        return framework;
    }
}
