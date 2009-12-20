package name.neilbartlett.eclipse.bndtools.prefs.frameworks.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class FrameworkPathWizardPage extends WizardPage implements PropertyChangeListener {
	
	private IFramework framework = null;
	private IFrameworkInstance instance = null;
	
	private Shell parentShell;
	private Text txtPath;
	
	public FrameworkPathWizardPage() {
		super("frameworkPath");
	}

	public void createControl(Composite parent) {
		parentShell = parent.getShell();
		setTitle("Add OSGi Framework Instance");
		setMessage("Select the path to the framework installation.");
		
		Composite composite = new Composite(parent, SWT.NONE);
		
		new Label(composite, SWT.NONE).setText("Select path:");
		txtPath = new Text(composite, SWT.BORDER);
		
		Button btnBrowseDir = new Button(composite, SWT.PUSH);
		btnBrowseDir.setText("Browse Directory...");
		
		new Label(composite, SWT.NONE); // spacer
		new Label(composite, SWT.NONE); // spacer
		Button btnBrowseFile = new Button(composite, SWT.PUSH);
		btnBrowseFile.setText("Browse JAR File...");
		
		// Events
		btnBrowseFile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(parentShell, SWT.OPEN);
				dialog.setText("Open Framework File...");
				
				String currentPath = txtPath.getText();
				if(currentPath != null && currentPath.length() > 0) {
					dialog.setFilterPath(currentPath);
				}
				String result = dialog.open();
				if(result != null) {
					txtPath.setText(result);
				}
			}
		});
		btnBrowseDir.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(parentShell, SWT.OPEN);
				dialog.setText("Open Framework Directory...");
				
				String currentPath = txtPath.getText();
				if(currentPath != null && currentPath.length() > 0) {
					dialog.setFilterPath(currentPath);
				}
				String result = dialog.open();
				if(result != null) {
					txtPath.setText(result);
				}
			}
		});
		txtPath.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getFromPath();
			}
		});
		
		// Layout
		composite.setLayout(new GridLayout(3, false));
		txtPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		btnBrowseDir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnBrowseFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		setControl(composite);
	}
	
	void getFromPath() {
		String error = null;
		instance = null;
		
		if(framework == null) {
			error = "A framework type was not selected";
		} else {
			File resource = new File(txtPath.getText());
			try {
				instance = framework.createFrameworkInstance(resource);
				error = instance.getValidationError();
			} catch (CoreException e) {
				error = e.getStatus().getMessage();
			}
		}
		setErrorMessage(error);
		getContainer().updateButtons();
	}
	
	public IFrameworkInstance getInstance() {
		return instance;
	}
	
	@Override
	public boolean isPageComplete() {
		return instance != null && instance.getValidationError() == null;
	}
	
	public void propertyChange(PropertyChangeEvent evt) {
		if(FrameworkTypeWizardPage.PROP_FRAMEWORK_TYPE.equals(evt.getPropertyName())) {
			this.framework = (IFramework) evt.getNewValue();
		}
	}

}
