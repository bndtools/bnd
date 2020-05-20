package bndtools.preferences.ui;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import bndtools.preferences.BndPreferences;

public class BndPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	public BndPreferencePage() {}

	public static final String		PAGE_ID				= "bndtools.prefPages.basic";

	private boolean					noCheckCnf			= false;
	private boolean					warnExistingLaunch	= true;
	private boolean					buildBeforeLaunch	= true;
	private boolean					editorOpenSourceTab	= false;
	private boolean					workspaceIsOffline	= false;
	private final BndPreferences	prefs				= new BndPreferences();

	private Text					prompt;

	@Override
	protected Control createContents(Composite parent) {
		// Layout
		GridLayout layout;
		GridData gd;

		Composite composite = new Composite(parent, SWT.NONE);

		final Button btnOfflineWorkspace = new Button(composite, SWT.CHECK);
		btnOfflineWorkspace.setText(Messages.BndPreferencePage_btnOfflineWorkspace);

		ControlDecoration decoration = new ControlDecoration(btnOfflineWorkspace, SWT.RIGHT | SWT.TOP, composite);
		decoration.setImage(FieldDecorationRegistry.getDefault()
			.getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
			.getImage());
		decoration.setDescriptionText(Messages.BndPreferencePage_decorOfflineWorkspace);

		// Create controls
		Group grpLaunching = new Group(composite, SWT.NONE);
		grpLaunching.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		grpLaunching.setText(Messages.BndPreferencePage_grpLaunching_text);
		grpLaunching.setLayout(new GridLayout(1, false));

		final Button btnWarnExistingLaunch = new Button(grpLaunching, SWT.CHECK);
		btnWarnExistingLaunch.setText(Messages.BndPreferencePage_btnWarnExistingLaunch);

		final Button btnBuildBeforeLaunch = new Button(grpLaunching, SWT.CHECK);
		btnBuildBeforeLaunch.setText(Messages.BndPreferencePage_btnBuildBeforeLaunch);

		// Source option
		Group editorGroup = new Group(composite, SWT.NONE);
		editorGroup.setText(Messages.BndPreferencePage_editorGroup);

		final Button btnEditorOpenSourceTab = new Button(editorGroup, SWT.CHECK);
		btnEditorOpenSourceTab.setText(Messages.BndPreferencePage_btnEditorOpenSourceTab);

		// Bndtools Explorer
		Group explorerGroup = new Group(composite, SWT.NONE);
		explorerGroup.setText("Explorer");

		prompt = new Text(explorerGroup, SWT.NONE);
		prompt.setText(prefs.getPrompt());
		Label promptLabel = new Label(explorerGroup, SWT.NONE);
		promptLabel.setText("Macro definition for Explorer message, set to '-' to not have any text");
		explorerGroup.setLayout(new GridLayout(2, false));

		// Load Data
		btnWarnExistingLaunch.setSelection(warnExistingLaunch);
		btnBuildBeforeLaunch.setSelection(buildBeforeLaunch);
		btnEditorOpenSourceTab.setSelection(editorOpenSourceTab);
		btnOfflineWorkspace.setSelection(workspaceIsOffline);
		// headless already done
		// versionControlIgnores already done

		// Listeners
		btnOfflineWorkspace.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				workspaceIsOffline = btnOfflineWorkspace.getSelection();
			}
		});

		btnBuildBeforeLaunch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				buildBeforeLaunch = btnBuildBeforeLaunch.getSelection();
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
		editorGroup.setLayoutData(gd);
		explorerGroup.setLayoutData(gd);
		prompt.setLayoutData(gd);

		layout = new GridLayout(1, false);
		layout.verticalSpacing = 10;
		editorGroup.setLayout(layout);

		return composite;
	}

	@Override
	public boolean performOk() {
		prefs.setHideInitCnfWizard(noCheckCnf);
		prefs.setWarnExistingLaunch(warnExistingLaunch);
		prefs.setBuildBeforeLaunch(buildBeforeLaunch);
		prefs.setEditorOpenSourceTab(editorOpenSourceTab);
		prefs.setWorkspaceOffline(workspaceIsOffline);
		prefs.setPrompt(prompt.getText());
		return true;
	}

	@Override
	public void init(IWorkbench workbench) {
		noCheckCnf = prefs.getHideInitCnfWizard();
		warnExistingLaunch = prefs.getWarnExistingLaunches();
		buildBeforeLaunch = prefs.getBuildBeforeLaunch();
		editorOpenSourceTab = prefs.getEditorOpenSourceTab();
		workspaceIsOffline = prefs.isWorkspaceOffline();
	}

}
