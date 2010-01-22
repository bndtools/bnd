/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package name.neilbartlett.eclipse.bndtools.prefs.frameworks.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
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
	
	private final Set<IPath> existingInstancePaths = new HashSet<IPath>();
	private IFramework framework = null;
	private IFrameworkInstance instance = null;
	
	private Shell parentShell;
	private Text txtPath;
	
	public FrameworkPathWizardPage(List<IFrameworkInstance> existingInstances) {
		super("frameworkPath");
		
		for (IFrameworkInstance instance : existingInstances) {
			IPath path = instance.getInstancePath();
			existingInstancePaths.add(path);
		}
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
			Path path = new Path(resource.getAbsolutePath());
			if(existingInstancePaths.contains(path)) {
				error = "This framework instance is already installed.";
			} else {
				try {
					instance = framework.createFrameworkInstance(resource);
					IStatus status = instance.getStatus();
					error = status.isOK() ? null : status.getMessage();
				} catch (CoreException e) {
					error = e.getStatus().getMessage();
				}
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
		return instance != null && instance.getStatus().isOK();
	}
	
	public void propertyChange(PropertyChangeEvent evt) {
		if(FrameworkTypeWizardPage.PROP_FRAMEWORK_TYPE.equals(evt.getPropertyName())) {
			this.framework = (IFramework) evt.getNewValue();
		}
	}

}
