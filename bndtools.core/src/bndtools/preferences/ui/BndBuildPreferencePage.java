package bndtools.preferences.ui;

import static bndtools.central.RebuildTriggerPolicy.REBUILDTRIGGERPOLICY_ALWAYS;
import static bndtools.central.RebuildTriggerPolicy.REBUILDTRIGGERPOLICY_API;

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

import aQute.bnd.build.Workspace;
import bndtools.central.Central;
import bndtools.preferences.BndPreferences;

public class BndBuildPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private BndPreferences	prefs;
	private int				buildLogging;
	private Button			parallel;
	private Button			rbAlways;
	private Button			rbOptimized;

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
		lblRebuildPolicy.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

		Composite policyGroup = new Composite(composite, SWT.NONE);
		GridLayout policyLayout = new GridLayout(1, false);
		policyLayout.marginWidth = 0;
		policyLayout.marginHeight = 0;
		policyGroup.setLayout(policyLayout);
		policyGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		// Radio button: Always rebuild
		rbAlways = new Button(policyGroup, SWT.RADIO);
		rbAlways.setText("Always rebuild (default)");
		rbAlways.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		Label lblAlwaysDesc = new Label(policyGroup, SWT.WRAP);
		lblAlwaysDesc
			.setText(
				"Every change triggers rebuilds in all downstream projects.\nSimple and predictable behavior, but can lead to long rebuild cascades in larger workspaces.");
		lblAlwaysDesc.setForeground(composite.getDisplay()
			.getSystemColor(SWT.COLOR_DARK_GRAY));
		GridData descData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		descData.horizontalIndent = 20;
		lblAlwaysDesc.setLayoutData(descData);

		// Radio button: Optimized (API-based)
		rbOptimized = new Button(policyGroup, SWT.RADIO);
		rbOptimized.setText("Optimized (Experimental)");
		rbOptimized.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		Label lblOptimizedDesc = new Label(policyGroup, SWT.WRAP);
		lblOptimizedDesc.setText(
			"Prevents unnecessary rebuilds cascades in incremental build scenarios,\n"
				+ "which can speed up development for larger workspaces.\n"
				+ "It is done by preserving JAR timestamps when only non-API changes occur.\n"
				+ "Limitations: If you have a jar dependency on the path that you are pulling classes into your bundle \n"
				+ "e.g. via -includepackage or -conditionalpackage, the bundle need to be rebuilt \n"
				+ "even if the public API of the dependency hasn't changed. "
				+ "This policy does not work well in such cases.");
		lblOptimizedDesc.setForeground(composite.getDisplay()
			.getSystemColor(SWT.COLOR_DARK_GRAY));
		descData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		descData.horizontalIndent = 20;
		lblOptimizedDesc.setLayoutData(descData);

		// Load Data
		cmbBuildLogging.select(buildLogging);
		String currentPolicy = prefs.getRebuildTriggerPolicy();
		if (REBUILDTRIGGERPOLICY_API.equals(currentPolicy)) {
			rbOptimized.setSelection(true);
		} else {
			rbAlways.setSelection(true); // Default
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
		String policy = getRebuildTriggerPolicy();
		prefs.setRebuildTriggerPolicy(policy);

		Workspace ws = Central.getWorkspaceIfPresent();
		if (ws != null) {
			Central.applyRebuildTriggerPolicy(ws);
		}

		return true;
	}

	private String getRebuildTriggerPolicy() {
		if (rbOptimized.getSelection()) {
			return REBUILDTRIGGERPOLICY_API;
		}
		return REBUILDTRIGGERPOLICY_ALWAYS;
	}


}
