package name.neilbartlett.eclipse.bndtools.pref.ui;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.utils.ModificationLock;

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

public class BndPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private final ModificationLock lock = new ModificationLock();
	
	private IPreferenceStore store;
	private String enableSubs;

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		
		Group group = new Group(composite, SWT.NONE);
		group.setText(Messages.BndPreferencePage_titleSubBundles);
		
		final Button btnAlways = new Button(group, SWT.RADIO);
		btnAlways.setText(Messages.BndPreferencePage_optionAlwaysEnable);
		final Button btnNever = new Button(group, SWT.RADIO);
		btnNever.setText(Messages.BndPreferencePage_optionNeverEnable);
		Button btnPrompt = new Button(group, SWT.RADIO);
		btnPrompt.setText(Messages.BndPreferencePage_optionPrompt);
		
		// Load
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
		
		// Layout
		GridLayout layout;
		GridData gd;
		
		layout = new GridLayout(1, false);
		composite.setLayout(layout);
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		group.setLayoutData(gd);
		
		layout = new GridLayout(1, false);
		group.setLayout(layout);
		
		return composite;
	}
	
	@Override
	public boolean performOk() {
		store.setValue(Plugin.PREF_ENABLE_SUB_BUNDLES, enableSubs);
		return true;
	}
	
	public void init(IWorkbench workbench) {
		store = Plugin.getDefault().getPreferenceStore();
		enableSubs = store.getString(Plugin.PREF_ENABLE_SUB_BUNDLES);
	}
}