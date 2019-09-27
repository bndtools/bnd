package bndtools.preferences.ui;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.bndtools.utils.swt.AddRemoveButtonBarPart;
import org.bndtools.utils.swt.AddRemoveButtonBarPart.AddRemoveListener;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import bndtools.preferences.BndPreferences;
import bndtools.shared.URLDialog;
import bndtools.shared.URLLabelProvider;

public class ReposPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private boolean			enableTemplateRepo;
	private List<String>	templateRepos;
	private TableViewer		vwrRepos;

	@Override
	public void init(IWorkbench workbench) {
		BndPreferences prefs = new BndPreferences();

		enableTemplateRepo = prefs.getEnableTemplateRepo();
		templateRepos = new ArrayList<>(prefs.getTemplateRepoUriList());
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginRight = 10;
		composite.setLayout(layout);

		Group group = new Group(composite, SWT.NONE);
		group.setText("Templates Repositories");
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		group.setLayout(new GridLayout(2, false));

		final Button btnEnableTemplateRepo = new Button(group, SWT.CHECK);
		btnEnableTemplateRepo.setText("Enable templates repositories");
		btnEnableTemplateRepo.setSelection(enableTemplateRepo);
		btnEnableTemplateRepo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

		ControlDecoration decoration = new ControlDecoration(btnEnableTemplateRepo, SWT.RIGHT | SWT.TOP, composite);
		decoration.setImage(FieldDecorationRegistry.getDefault()
			.getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
			.getImage());
		decoration.setMarginWidth(3);
		decoration.setDescriptionText(
			"These repositories are used to load\ntemplates, in addition to repositories\nconfigured in the Bnd OSGi Workspace.");
		decoration.setShowHover(true);
		decoration.setShowOnlyOnFocus(false);

		Label lblRepos = new Label(group, SWT.NONE);
		lblRepos.setText("Repository URLs:");
		lblRepos.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

		final Table tblRepos = new Table(group, SWT.BORDER | SWT.MULTI);
		vwrRepos = new TableViewer(tblRepos);
		vwrRepos.setContentProvider(ArrayContentProvider.getInstance());
		vwrRepos.setLabelProvider(new URLLabelProvider(tblRepos.getDisplay()));
		vwrRepos.setInput(templateRepos);

		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 260;
		gd.heightHint = 80;
		tblRepos.setLayoutData(gd);
		tblRepos.setEnabled(enableTemplateRepo);

		final AddRemoveButtonBarPart addRemoveRepoPart = new AddRemoveButtonBarPart();
		Control addRemovePanel = addRemoveRepoPart.createControl(group, SWT.FLAT | SWT.VERTICAL);
		addRemovePanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		addRemoveRepoPart.setRemoveEnabled(false);
		addRemoveRepoPart.addListener(new AddRemoveListener() {
			@Override
			public void addSelected() {
				doAddRepo();
			}

			@Override
			public void removeSelected() {
				doRemoveRepo();
			}
		});
		vwrRepos.addSelectionChangedListener(event -> addRemoveRepoPart.setRemoveEnabled(!vwrRepos.getSelection()
			.isEmpty()));
		tblRepos.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.DEL && e.stateMask == 0)
					doRemoveRepo();
			}
		});

		btnEnableTemplateRepo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent ev) {
				enableTemplateRepo = btnEnableTemplateRepo.getSelection();
				tblRepos.setEnabled(enableTemplateRepo);
				validate();
			}
		});

		return composite;
	}

	private void doAddRepo() {
		URLDialog dialog = new URLDialog(getShell(), "Add repository URL", false);
		if (dialog.open() == Window.OK) {
			URI location = dialog.getLocation();

			String locationStr = location.toString();
			templateRepos.add(locationStr);
			vwrRepos.add(locationStr);
		}
	}

	private void doRemoveRepo() {
		int[] selectedIndexes = vwrRepos.getTable()
			.getSelectionIndices();
		if (selectedIndexes == null)
			return;
		List<Object> selected = new ArrayList<>(selectedIndexes.length);
		for (int index : selectedIndexes) {
			selected.add(templateRepos.get(index));
		}
		templateRepos.removeAll(selected);
		vwrRepos.remove(selected.toArray());
		validate();
	}

	private void validate() {
		String error = null;
		if (enableTemplateRepo) {
			for (String templateRepo : templateRepos) {
				try {
					@SuppressWarnings("unused")
					URI uri = new URI(templateRepo);
				} catch (URISyntaxException e) {
					error = "Invalid URL: " + e.getMessage();
				}
			}
		}
		setErrorMessage(error);
		setValid(error == null);
	}

	@Override
	public boolean performOk() {
		BndPreferences prefs = new BndPreferences();

		prefs.setEnableTemplateRepo(enableTemplateRepo);
		prefs.setTemplateRepoUriList(templateRepos);

		return true;
	}

}
