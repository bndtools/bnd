package bndtools.preferences.ui;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import bndtools.preferences.BndPreferences;

public class BndBuildPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private BndPreferences prefs;
    private int buildLogging;

    @Override
    public void init(IWorkbench workbench) {
        prefs = new BndPreferences();
        buildLogging = prefs.getBuildLogging();
    }

    @Override
    protected Control createContents(Composite parent) {
        GridLayout layout;
        GridData gd;

        Composite composite = new Composite(parent, SWT.NONE);
        layout = new GridLayout(2, false);
        composite.setLayout(layout);

        // Create controls
        new Label(composite, SWT.NONE).setText("Build Debug Logging:");
        final Combo cmbBuildLogging = new Combo(composite, SWT.READ_ONLY);
        cmbBuildLogging.setItems(new String[] {
                Messages.BndPreferencePage_cmbBuildLogging_None, Messages.BndPreferencePage_cmbBuildLogging_Basic, Messages.BndPreferencePage_cmbBuildLogging_Full
        });
        cmbBuildLogging.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Load Data
        cmbBuildLogging.select(buildLogging);

        // Listeners
        cmbBuildLogging.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                buildLogging = cmbBuildLogging.getSelectionIndex();
            }
        });

        return composite;
    }

    @Override
    public boolean performOk() {
        prefs.setBuildLogging(buildLogging);

        return true;
    }

}
