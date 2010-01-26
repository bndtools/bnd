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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.ui.OSGiFrameworkLabelProvider;
import name.neilbartlett.eclipse.bndtools.utils.SWTConcurrencyUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

public class FrameworkPathWizardPage extends WizardPage implements PropertyChangeListener {
	
	private final Set<IPath> existingInstancePaths = new HashSet<IPath>();
	private IFramework framework = null;
	private IFrameworkInstance instance = null;
	
	private Display display;
	private Shell parentShell;
	private Text txtPath;
	private TableViewer viewer;
	
	public FrameworkPathWizardPage(List<IFrameworkInstance> existingInstances) {
		super("frameworkPath");
		
		for (IFrameworkInstance instance : existingInstances) {
			IPath path = instance.getInstancePath();
			existingInstancePaths.add(path);
		}
	}

	public void createControl(Composite parent) {
		this.display = parent.getDisplay();
		
		parentShell = parent.getShell();
		setTitle("Add OSGi Framework Instance");
		setMessage("Select the path to the framework installation.");
		
		Composite composite = new Composite(parent, SWT.NONE);
		Composite topPanel = new Composite(composite, SWT.NONE)
		;
		new Label(topPanel, SWT.NONE).setText("Select path:");
		txtPath = new Text(topPanel, SWT.BORDER);
		
		Button btnBrowseDir = new Button(topPanel, SWT.PUSH);
		btnBrowseDir.setText("Browse Directory...");
		
		new Label(topPanel, SWT.NONE); // spacer
		new Label(topPanel, SWT.NONE); // spacer
		Button btnBrowseFile = new Button(topPanel, SWT.PUSH);
		btnBrowseFile.setText("Browse JAR File...");
		
		Composite bottomPanel = new Composite(composite, SWT.NONE);
		new Label(bottomPanel, SWT.NONE).setText("Auto-configured Locations:");
		Table table = new Table(bottomPanel, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER);
		
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new OSGiFrameworkLabelProvider(parent.getDisplay(), null));
		
		updateAutoConfiguredFrameworks();
		
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
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IFrameworkInstance selected = (IFrameworkInstance) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
				if(selected != null) {
					txtPath.setText(selected.getInstancePath().toString());
				}
			}
		});
		
		// Layout
		composite.setLayout(new GridLayout(1, false));
		topPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		topPanel.setLayout(new GridLayout(3, false));
		txtPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		btnBrowseDir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnBrowseFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		bottomPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		bottomPanel.setLayout(new GridLayout(1, false));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = 75;
		table.setLayoutData(gd);
		
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
			setFramework((IFramework) evt.getNewValue());
		}
	}
	private void setFramework(IFramework newValue) {
		this.framework = newValue;
		
		updateAutoConfiguredFrameworks();
	}

	void updateAutoConfiguredFrameworks() {
		final List<IFrameworkInstance> instances;
		
		if(framework == null) {
			instances = Collections.emptyList();
		} else {
			Collection<File> locations = framework.getAutoConfiguredLocations();
			instances = new ArrayList<IFrameworkInstance>(locations.size());
			for (File location : locations) {
				try {
					instances.add(framework.createFrameworkInstance(location));
				} catch (CoreException e) {
					Plugin.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error creating auto-configured framework instance at location: " + location.toString(), e));
				}
			}
		}
		
		SWTConcurrencyUtil.execForDisplay(display, new Runnable() {
			public void run() {
				viewer.setInput(instances);
			}
		});
	}
}
