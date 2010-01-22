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
package name.neilbartlett.eclipse.bndtools.launch.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;

import name.neilbartlett.eclipse.bndtools.classpath.FrameworkUtils;
import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel;
import name.neilbartlett.eclipse.bndtools.frameworks.ui.FrameworkSelector;
import name.neilbartlett.eclipse.bndtools.launch.IFrameworkLaunchConstants;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class OSGiMainTab extends JavaLaunchTab {
	
	private final FrameworkSelector frameworkSelector = new FrameworkSelector();
	private Text fProjText;
	private Button fProjButton;

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		
		createProjectEditor(composite);
		createFrameworkSelector(composite);
		
		// Layout
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout(1, false));
		
		setControl(composite);
	}

	public String getName() {
		return "OSGi";
	}

	public void initializeFrom(ILaunchConfiguration config) {
		updateProjectFromConfig(config);
		super.initializeFrom(config);
	}

	private void updateProjectFromConfig(ILaunchConfiguration config) {
		String projectName = "";
		
		OSGiSpecLevel specLevel = null;
		IFrameworkInstance frameworkInstance = null;
		
		boolean useSpec = true;
		try {
			useSpec = config.getAttribute(IFrameworkLaunchConstants.ATTR_USE_FRAMEWORK_SPEC_LEVEL, true);
			projectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
			
			if(useSpec) {
				String specLevelStr = config.getAttribute(IFrameworkLaunchConstants.ATTR_FRAMEWORK_SPEC_LEVEL, (String) null);
				if(specLevelStr != null)
					specLevel = Enum.valueOf(OSGiSpecLevel.class, specLevelStr);
			} else {
				String frameworkId = config.getAttribute(IFrameworkLaunchConstants.ATTR_FRAMEWORK_ID, (String) null);
				String frameworkInstancePath = config.getAttribute(IFrameworkLaunchConstants.ATTR_FRAMEWORK_INSTANCE_PATH, (String) null);
				if(frameworkId != null && frameworkId.length() > 0 && frameworkInstancePath != null && frameworkInstancePath.length() > 0) {
					IFramework framework = FrameworkUtils.findFramework(frameworkId);
					frameworkInstance = framework.createFrameworkInstance(new File(frameworkInstancePath));
					IStatus status = frameworkInstance.getStatus();
					setErrorMessage(status.isOK() ? null : status.getMessage());
				}
			}
		}
		catch (CoreException ce) {
			setErrorMessage(ce.getStatus().getMessage());
		}
		
		fProjText.setText(projectName);
		frameworkSelector.setUseSpecLevel(useSpec);
		frameworkSelector.setSelection(useSpec ? specLevel : frameworkInstance);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProjText.getText().trim());
		
		boolean useSpec = frameworkSelector.isUseSpecLevel();
		config.setAttribute(IFrameworkLaunchConstants.ATTR_USE_FRAMEWORK_SPEC_LEVEL, useSpec);
		if(useSpec) {
			OSGiSpecLevel specLevel = frameworkSelector.getSelectedSpecLevel();
			if(specLevel == null) {
				config.setAttribute(IFrameworkLaunchConstants.ATTR_FRAMEWORK_SPEC_LEVEL, (String) null);
			} else {
				config.setAttribute(IFrameworkLaunchConstants.ATTR_FRAMEWORK_SPEC_LEVEL, specLevel.toString());
			}
		} else {
			IFrameworkInstance frameworkInstance = frameworkSelector.getSelectedFramework();
			if(frameworkInstance == null) {
				config.setAttribute(IFrameworkLaunchConstants.ATTR_FRAMEWORK_ID, (String) null);
				config.setAttribute(IFrameworkLaunchConstants.ATTR_FRAMEWORK_INSTANCE_PATH, (String) null);
			} else {
				config.setAttribute(IFrameworkLaunchConstants.ATTR_FRAMEWORK_ID, frameworkInstance.getFrameworkId());
				config.setAttribute(IFrameworkLaunchConstants.ATTR_FRAMEWORK_INSTANCE_PATH, frameworkInstance.getInstancePath().toString());
			}
		}
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		IJavaElement javaElement = getContext();
		if (javaElement != null) {
			initializeJavaProject(javaElement, config);
		}
		else {
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
		}
	}
	
	protected IJavaProject getJavaProject() {
		String projectName = fProjText.getText().trim();
		if (projectName.length() < 1) {
			return null;
		}
		return getJavaModel().getJavaProject(projectName);		
	}

	protected void createFrameworkSelector(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("OSGi Framework");
		
		frameworkSelector.createControl(group);
		Control frameworkSelectorControl = frameworkSelector.getControl();
		
		// Events
		frameworkSelector.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		// Layout
		GridData groupLayoutData = new GridData(GridData.FILL_HORIZONTAL);
		groupLayoutData.heightHint = 200;
		group.setLayoutData(groupLayoutData);
		group.setLayout(new GridLayout(1, false));
		frameworkSelectorControl.setLayoutData(new GridData(GridData.FILL_BOTH));
	}
	
	protected void createProjectEditor(Composite parent) {
		Font font= parent.getFont();
		Group group= new Group(parent, SWT.NONE);
		group.setText("Project"); 
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setFont(font);
		fProjText = new Text(group, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProjText.setLayoutData(gd);
		fProjText.setFont(font);
		fProjButton = createPushButton(group, "Browse...", null);

		// Events
		fProjText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		fProjButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IJavaProject project = chooseJavaProject();
				if (project == null) {
					return;
				}
				String projectName = project.getElementName();
				fProjText.setText(projectName);		
			}
		});
	}
	
	@Override
	public boolean isValid(ILaunchConfiguration config) {
		setErrorMessage(null);
		setMessage(null);
		String name = fProjText.getText().trim();
		if (name.length() > 0) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IStatus status = workspace.validateName(name, IResource.PROJECT);
			if (status.isOK()) {
				IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(name);
				if (!project.exists()) {
					setErrorMessage(MessageFormat.format("Project {0} does not exist", new Object[] {name})); 
					return false;
				}
				if (!project.isOpen()) {
					setErrorMessage(MessageFormat.format("Project {0} is closed", new Object[] {name})); 
					return false;
				}
			}
			else {
				setErrorMessage(MessageFormat.format("Illegal project name: {0}", new Object[]{status.getMessage()})); 
				return false;
			}
		}
		if(frameworkSelector.isUseSpecLevel()) {
			if(frameworkSelector.getSelectedSpecLevel() == null) {
				setErrorMessage("No OSGi Framework was specified");
				return false;
			}
			String selectorError = frameworkSelector.getErrorMessage();
			if(selectorError != null) {
				setErrorMessage(selectorError);
				return false;
			}
		} else {
			IFrameworkInstance selectedFramework = frameworkSelector.getSelectedFramework();
			if(selectedFramework == null) {
				setErrorMessage("No OSGi Framework was specified");
				return false;
			}
			IStatus status = selectedFramework.getStatus();
			if(!status.isOK()) {
				setErrorMessage(status.getMessage());
				return false;
			}
		}
		return true;
	}
	
	private IJavaProject chooseJavaProject() {
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setTitle("Project Selection"); 
		dialog.setMessage("Select a Java Project"); 
		try {
			dialog.setElements(JavaCore.create(getWorkspaceRoot()).getJavaProjects());
		}
		catch (JavaModelException e) {
			e.printStackTrace();
			// TODO log exception
		}
		IJavaProject javaProject= getJavaProject();
		if (javaProject != null) {
			dialog.setInitialSelections(new Object[] { javaProject });
		}
		if (dialog.open() == Window.OK) {			
			return (IJavaProject) dialog.getFirstResult();
		}		
		return null;		
	}

	private IJavaModel getJavaModel() {
		return JavaCore.create(getWorkspaceRoot());
	}

	private IWorkspaceRoot getWorkspaceRoot() {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		return workspaceRoot;
	}

}
