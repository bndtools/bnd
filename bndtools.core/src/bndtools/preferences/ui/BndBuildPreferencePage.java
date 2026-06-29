package bndtools.preferences.ui;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import aQute.bnd.osgi.Constants;
import bndtools.preferences.BndPreferences;

public class BndBuildPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private BndPreferences	prefs;
	private int				buildLogging;
	private Button			parallel;
	private Combo			cmbRebuildPolicy;

	@Override
	public void init(IWorkbench workbench) {
		prefs = new BndPreferences();
		buildLogging = prefs.getBuildLogging();
	}

	@Override
	protected Control createContents(Composite parent) {
		GridLayout layout;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout(2, false);
		composite.setLayout(layout);

		// Build logging
		new Label(composite, SWT.NONE).setText("Build Debug Logging:");
		final Combo cmbBuildLogging = new Combo(composite, SWT.READ_ONLY);
		cmbBuildLogging.setItems(Messages.BndPreferencePage_cmbBuildLogging_None,
			Messages.BndPreferencePage_cmbBuildLogging_Basic, Messages.BndPreferencePage_cmbBuildLogging_Full);
		cmbBuildLogging.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Allow Build parallel
		new Label(composite, SWT.NONE).setText("Allow build in parallel (highly experimental)");
		parallel = new Button(composite, SWT.CHECK);
		parallel.setSelection(prefs.isParallel());

		// Rebuild trigger policy
		Label lblRebuildPolicy = new Label(composite, SWT.NONE);
		lblRebuildPolicy.setText("Rebuild Trigger Policy:");
		cmbRebuildPolicy = new Combo(composite, SWT.READ_ONLY);
		cmbRebuildPolicy.setItems("Default (use build.bnd setting)", "Always rebuild",
			"API-based (skip if API unchanged)");
		cmbRebuildPolicy.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cmbRebuildPolicy.setToolTipText(
			"'Default' uses the value of '-rebuildtriggerpolicy' from build.bnd (or 'always' if not set). "
				+ "'Always rebuild' triggers a rebuild on every change. "
				+ "'API-based' skips rebuilds when only non-API changes are detected. "
				+ "Changes take effect on the next build.");
		lblRebuildPolicy.setToolTipText(cmbRebuildPolicy.getToolTipText());

		// Load Data
		cmbBuildLogging.select(buildLogging);
		String currentPolicy = prefs.getRebuildTriggerPolicy();
		if (Constants.REBUILDTRIGGERPOLICY_API.equals(currentPolicy)) {
			cmbRebuildPolicy.select(2);
		} else if (Constants.REBUILDTRIGGERPOLICY_ALWAYS.equals(currentPolicy)) {
			cmbRebuildPolicy.select(1);
		} else {
			cmbRebuildPolicy.select(0); // Default
		}

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
		prefs.setParallel(parallel.getSelection());
		String policy;
		int idx = cmbRebuildPolicy.getSelectionIndex();
		if (idx == 2) {
			policy = Constants.REBUILDTRIGGERPOLICY_API;
		} else if (idx == 1) {
			policy = Constants.REBUILDTRIGGERPOLICY_ALWAYS;
		} else {
			policy = ""; // Default: do not override build.bnd
		}
		prefs.setRebuildTriggerPolicy(policy);
		return true;
	}

}
