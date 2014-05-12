package bndtools.preferences.ui;

import java.text.MessageFormat;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import aQute.bnd.build.Project;
import bndtools.preferences.BndPreferences;
import bndtools.wizards.workspace.CnfSetupWizard;

public class BndPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
    public BndPreferencePage() {}

    public static final String PAGE_ID = "bndtools.prefPages.basic";

    private boolean noCheckCnf = false;
    private boolean warnExistingLaunch = true;
    private boolean editorOpenSourceTab = false;

    @Override
    protected Control createContents(Composite parent) {
        // Layout
        GridLayout layout;
        GridData gd;

        Composite composite = new Composite(parent, SWT.NONE);

        // Create controls
        Group cnfCheckGroup = new Group(composite, SWT.NONE);
        cnfCheckGroup.setText(Messages.BndPreferencePage_cnfCheckGroup);

        final Button btnNoCheckCnf = new Button(cnfCheckGroup, SWT.CHECK);
        btnNoCheckCnf.setText(MessageFormat.format(Messages.BndPreferencePage_btnNoCheckCnf, Project.BNDCNF));
        final Button btnCheckCnfNow = new Button(cnfCheckGroup, SWT.PUSH);
        btnCheckCnfNow.setText(Messages.BndPreferencePage_btnCheckCnfNow);

        Group grpLaunching = new Group(composite, SWT.NONE);
        grpLaunching.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        grpLaunching.setText(Messages.BndPreferencePage_grpLaunching_text);
        grpLaunching.setLayout(new GridLayout(1, false));

        final Button btnWarnExistingLaunch = new Button(grpLaunching, SWT.CHECK);
        btnWarnExistingLaunch.setText(Messages.BndPreferencePage_btnWarnExistingLaunch);

        Group editorGroup = new Group(composite, SWT.NONE);
        editorGroup.setText(Messages.BndPreferencePage_editorGroup);

        final Button btnEditorOpenSourceTab = new Button(editorGroup, SWT.CHECK);
        btnEditorOpenSourceTab.setText(Messages.BndPreferencePage_btnEditorOpenSourceTab);

        // Load Data
        btnNoCheckCnf.setSelection(noCheckCnf);
        btnCheckCnfNow.setEnabled(!noCheckCnf);
        btnWarnExistingLaunch.setSelection(warnExistingLaunch);
        btnEditorOpenSourceTab.setSelection(editorOpenSourceTab);
        // headless already done
        // versionControlIgnores already done

        // Listeners
        btnNoCheckCnf.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                noCheckCnf = btnNoCheckCnf.getSelection();
                btnCheckCnfNow.setEnabled(!noCheckCnf);
            }
        });
        btnCheckCnfNow.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!CnfSetupWizard.showIfNeeded(true)) {
                    MessageDialog.openInformation(getShell(), Messages.BndPreferencePage_btnCheckCnfNow_BndConf, Messages.BndPreferencePage_btnCheckCnfNow_Exists);
                }
            }
        });
        btnWarnExistingLaunch.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                warnExistingLaunch = btnWarnExistingLaunch.getSelection();
            }
        });
        btnEditorOpenSourceTab.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editorOpenSourceTab = btnEditorOpenSourceTab.getSelection();
            }
        });
        // headless already done
        // versionControlIgnores already done

        layout = new GridLayout(1, false);
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        cnfCheckGroup.setLayoutData(gd);

        cnfCheckGroup.setLayout(new GridLayout(1, false));
        gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
        btnNoCheckCnf.setLayoutData(gd);
        gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
        btnCheckCnfNow.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        editorGroup.setLayoutData(gd);

        layout = new GridLayout(1, false);
        layout.verticalSpacing = 10;
        editorGroup.setLayout(layout);

        return composite;
    }

    @Override
    public boolean performOk() {
        BndPreferences prefs = new BndPreferences();
        prefs.setHideInitCnfWizard(noCheckCnf);
        prefs.setWarnExistingLaunch(warnExistingLaunch);
        prefs.setEditorOpenSourceTab(editorOpenSourceTab);
        return true;
    }

    @Override
    public void init(IWorkbench workbench) {
        BndPreferences prefs = new BndPreferences();

        noCheckCnf = prefs.getHideInitCnfWizard();
        warnExistingLaunch = prefs.getWarnExistingLaunches();
        editorOpenSourceTab = prefs.getEditorOpenSourceTab();
    }

}