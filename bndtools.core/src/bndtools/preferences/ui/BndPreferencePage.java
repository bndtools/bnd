package bndtools.preferences.ui;


import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.utils.ModificationLock;
import bndtools.wizards.workspace.CnfSetupWizard;

public class BndPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
    public BndPreferencePage() {
    }

    public static final String PAGE_ID = "bndtools.prefPages.basic";

	private final ModificationLock lock = new ModificationLock();

	private IPreferenceStore store;
	private String enableSubs;
	private boolean noAskPackageInfo = false;
	private boolean noCheckCnf = false;
	private int buildLogging = 0;

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);

		// Create controls
		Group cnfCheckGroup = new Group(composite, SWT.NONE);
		cnfCheckGroup.setText("Configuration Project");

		final Button btnNoCheckCnf = new Button(cnfCheckGroup, SWT.CHECK);
		btnNoCheckCnf.setText(MessageFormat.format("Do not check for the Bnd Configuration project (\"{0}\").", Project.BNDCNF));
		final Button btnCheckCnfNow = new Button(cnfCheckGroup, SWT.PUSH);
		btnCheckCnfNow.setText("Check Now");

		Group enableSubBundlesGroup = new Group(composite, SWT.NONE);
		enableSubBundlesGroup.setText(Messages.BndPreferencePage_titleSubBundles);

		final Button btnAlways = new Button(enableSubBundlesGroup, SWT.RADIO);
		btnAlways.setText(Messages.BndPreferencePage_optionAlwaysEnable);
		final Button btnNever = new Button(enableSubBundlesGroup, SWT.RADIO);
		btnNever.setText(Messages.BndPreferencePage_optionNeverEnable);
		Button btnPrompt = new Button(enableSubBundlesGroup, SWT.RADIO);
		btnPrompt.setText(Messages.BndPreferencePage_optionPrompt);

		Group exportsGroup = new Group(composite, SWT.NONE);
		exportsGroup.setText( "Exported Source Packages");

		final Button btnNoAskPackageInfo = new Button(exportsGroup, SWT.CHECK);
		btnNoAskPackageInfo.setText("Always generate \"packageinfo\" file.");

        Group grpDebugging = new Group(composite, SWT.NONE);
        grpDebugging.setText(Messages.BndPreferencePage_grpDebugging_text);

        Label lblBuildLogging = new Label(grpDebugging, SWT.NONE);
        lblBuildLogging.setText(Messages.BndPreferencePage_lblBuildLogging_text);

        final Combo cmbBuildLogging = new Combo(grpDebugging, SWT.READ_ONLY);
        cmbBuildLogging.setItems(new String[] { "None", "Basic", "Full" });

		// Load Data
		if(MessageDialogWithToggle.ALWAYS.equals(enableSubs)) {
			btnAlways.setSelection(true);
			btnNever.setSelection(false);
			btnPrompt.setSelection(false);
		} else if(MessageDialogWithToggle.NEVER.equals(enableSubs)) {
			btnAlways.setSelection(false);
			btnNever.setSelection(true);
			btnPrompt.setSelection(false);
		} else {
			btnAlways.setSelection(false);
			btnNever.setSelection(false);
			btnPrompt.setSelection(true);
		}
		btnNoAskPackageInfo.setSelection(noAskPackageInfo);
		btnNoCheckCnf.setSelection(noCheckCnf);
		btnCheckCnfNow.setEnabled(!noCheckCnf);
		cmbBuildLogging.select(buildLogging);

		// Listeners
		SelectionAdapter adapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				lock.ifNotModifying(new Runnable() {
					public void run() {
						if(btnAlways.getSelection()) {
							enableSubs = MessageDialogWithToggle.ALWAYS;
						} else if(btnNever.getSelection()) {
							enableSubs = MessageDialogWithToggle.NEVER;
						} else {
							enableSubs = MessageDialogWithToggle.PROMPT;
						}
					}
				});
			}
		};
		btnAlways.addSelectionListener(adapter);
		btnNever.addSelectionListener(adapter);
		btnPrompt.addSelectionListener(adapter);
		btnNoAskPackageInfo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				noAskPackageInfo = btnNoAskPackageInfo.getSelection();
			}
		});
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
		            MessageDialog.openInformation(getShell(), "Bnd Configuration", "The configuration project exists and does not need to be updated.");
		        }
		    }
        });
		cmbBuildLogging.addSelectionListener(new SelectionAdapter() {
		    @Override
		    public void widgetSelected(SelectionEvent e) {
		        buildLogging = cmbBuildLogging.getSelectionIndex();
		    }
        });

		// Layout
		GridLayout layout;
		GridData gd;

		layout = new GridLayout(1, false);
		composite.setLayout(layout);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		enableSubBundlesGroup.setLayoutData(gd);

		layout = new GridLayout(1, false);
		enableSubBundlesGroup.setLayout(layout);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		exportsGroup.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        cnfCheckGroup.setLayoutData(gd);

        layout = new GridLayout(1, false);
		layout.verticalSpacing = 10;
		exportsGroup.setLayout(layout);

		cnfCheckGroup.setLayout(new GridLayout(1, false));
		gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		btnNoCheckCnf.setLayoutData(gd);
		gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		btnCheckCnfNow.setLayoutData(gd);

        grpDebugging.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        grpDebugging.setLayout(new GridLayout(2, false));
        cmbBuildLogging.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		return composite;
	}

	@Override
	public boolean performOk() {
		store.setValue(Plugin.PREF_ENABLE_SUB_BUNDLES, enableSubs);
		store.setValue(Plugin.PREF_NOASK_PACKAGEINFO, noAskPackageInfo);
		store.setValue(Plugin.PREF_HIDE_INITIALISE_CNF_WIZARD, noCheckCnf);
		store.setValue(Plugin.PREF_BUILD_LOGGING, buildLogging);
		return true;
	}

	public void init(IWorkbench workbench) {
		store = Plugin.getDefault().getPreferenceStore();

		enableSubs = store.getString(Plugin.PREF_ENABLE_SUB_BUNDLES);
		noAskPackageInfo = store.getBoolean(Plugin.PREF_NOASK_PACKAGEINFO);
		noCheckCnf = store.getBoolean(Plugin.PREF_HIDE_INITIALISE_CNF_WIZARD);
		buildLogging = store.getInt(Plugin.PREF_BUILD_LOGGING);
	}
}