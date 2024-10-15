package org.bndtools.elph.importer;

import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;

class LocateRepoPage extends WizardPage {
	final Config config;
	LocateRepoPage(Config config) {
		super(LocateRepoPage.class.getSimpleName());
		setTitle("Locate the Open Liberty Repository");
		setDescription("Specify the directory containing the local Open Liberty git repository");
		setMessage("Please choose the directory containing your local Open Liberty git repository");
		this.config = config;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
		setControl(page);
		setPageComplete(false);

		page.setLayout(new GridLayout(2, false));
		String olPathTemplate = "Open Liberty repository: ";
		Label olLabel = new Label(page, SWT.NONE);
		olLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
		olLabel.setText(olPathTemplate + "<unspecified>");
		    
		Button fileBrowser = new Button(page, SWT.PUSH);
		fileBrowser.setText("Browse");
		fileBrowser.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog dialog = new DirectoryDialog(page.getShell(), SWT.NULL);
				String dir = dialog.open();
				try {
					Path repo = Optional.ofNullable(dir)
						.map(Controller::new)
						.map(Controller::getRepo)
						.orElseThrow(() -> new Exception("Please choose a valid directory for your Open Liberty repository. "));
					// save the setting
					config.saveOlRepoPath(repo);
					// display the setting
					olLabel.setText(olPathTemplate + dir);
					// let the user continue
					setPageComplete(true);
				} catch (Exception e) {
					setPageComplete(false);
					errorDialogue(parent, e.getLocalizedMessage());
					olLabel.setText(olPathTemplate + "<unspecified>");
				}
			}
		});
	}	

	private void errorDialogue(Composite parent, String errMsg) { 
		MessageDialog.openError(parent.getShell(), getTitle(), errMsg);
	}
}
