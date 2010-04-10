package bndtools.preferences.ui;


import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
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

import bndtools.Plugin;
import bndtools.pieces.ExportVersionPolicy;
import bndtools.pieces.ExportVersionPolicyPiece;
import bndtools.utils.ModificationLock;

public class BndPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private final ModificationLock lock = new ModificationLock();
	
	private IPreferenceStore store;
	private String enableSubs;
	private boolean noAskVersionPolicy = false;
	
	private final ExportVersionPolicyPiece versionPolicyPiece = new ExportVersionPolicyPiece();

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		
		// Create controls
		Group enableSubBundlesGroup = new Group(composite, SWT.NONE);
		enableSubBundlesGroup.setText(Messages.BndPreferencePage_titleSubBundles);
		
		final Button btnAlways = new Button(enableSubBundlesGroup, SWT.RADIO);
		btnAlways.setText(Messages.BndPreferencePage_optionAlwaysEnable);
		final Button btnNever = new Button(enableSubBundlesGroup, SWT.RADIO);
		btnNever.setText(Messages.BndPreferencePage_optionNeverEnable);
		Button btnPrompt = new Button(enableSubBundlesGroup, SWT.RADIO);
		btnPrompt.setText(Messages.BndPreferencePage_optionPrompt);
		
		Group versionPolicyGroup = new Group(composite, SWT.NONE);
		versionPolicyGroup.setText( "Default Export Version");
		
		Control versionPolicyControl = versionPolicyPiece.createVersionPolicyComposite(versionPolicyGroup, SWT.NONE);
		final Button btnNoAskVersionPolicy = new Button(versionPolicyGroup, SWT.CHECK);
		btnNoAskVersionPolicy.setText("Use this default without asking.");
		
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
		btnNoAskVersionPolicy.setSelection(noAskVersionPolicy);
		
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
		btnNoAskVersionPolicy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				noAskVersionPolicy = btnNoAskVersionPolicy.getSelection();
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
		versionPolicyGroup.setLayoutData(gd);
		
		layout = new GridLayout(1, false);
		layout.verticalSpacing = 10;
		versionPolicyGroup.setLayout(layout);
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		versionPolicyControl.setLayoutData(gd);
		
		layout = versionPolicyPiece.getLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		
		return composite;
	}
	
	@Override
	public boolean performOk() {
		store.setValue(Plugin.PREF_ENABLE_SUB_BUNDLES, enableSubs);
		store.setValue(Plugin.PREF_DEFAULT_EXPORT_VERSION_POLICY, versionPolicyPiece.getExportVersionPolicy().toString());
		store.setValue(Plugin.PREF_DEFAULT_EXPORT_VERSION, versionPolicyPiece.getSpecifiedVersion());
		store.setValue(Plugin.PREF_NOASK_EXPORT_VERSION, noAskVersionPolicy);
		return true;
	}
	
	public void init(IWorkbench workbench) {
		store = Plugin.getDefault().getPreferenceStore();
		
		// Sub-bundles
		enableSubs = store.getString(Plugin.PREF_ENABLE_SUB_BUNDLES);
		
		// Version Policy 
		String policyStr = store.getString(Plugin.PREF_DEFAULT_EXPORT_VERSION_POLICY);
		ExportVersionPolicy policy;
		try {
			policy = Enum.valueOf(ExportVersionPolicy.class, policyStr);
		} catch (IllegalArgumentException e) {
			policy = ExportVersionPolicy.linkWithBundle;
		}
		versionPolicyPiece.setExportVersionPolicy(policy);
		
		// Specified version (if any)
		String version = store.getString(Plugin.PREF_DEFAULT_EXPORT_VERSION);
		if(version == null || version.length() == 0) {
			version = Plugin.DEFAULT_VERSION.toString();
		}
		versionPolicyPiece.setSpecifiedVersion(version);

		// Ask about version policy?
		noAskVersionPolicy = store.getBoolean(Plugin.PREF_NOASK_EXPORT_VERSION);
	}
}