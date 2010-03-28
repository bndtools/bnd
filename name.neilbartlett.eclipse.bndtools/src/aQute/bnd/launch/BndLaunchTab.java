package aQute.bnd.launch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.plugin.Central;

public class BndLaunchTab extends AbstractLaunchConfigurationTab {

	private Combo combo;
	private List<String> projectNames;

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		setControl(composite);
		
		new Label(composite, SWT.NONE).setText("Project Name:");
		projectNames = loadProjectNames();
		combo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		combo.setItems((String[]) projectNames.toArray(new String[projectNames.size()]));
		combo.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});
		
		// Layout
		composite.setLayout(new GridLayout(2, false));
		combo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
	}

	List<String> loadProjectNames() {
		Collection<Project> projects;
		try {
			Workspace workspace = Central.getWorkspace();
			projects = workspace.getAllProjects();
		} catch (Exception e) {
			Plugin.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Internal error querying projects.", e));
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<String>(projects.size());
		for (Project project : projects) {
			result.add(project.getName());
		}
		return result;
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}

	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
			if(projectName != null) {
				int index = projectNames.indexOf(projectName);
				if(index != -1)
					combo.select(index);
			}
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error reading launch configuration.", e));
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		int index = combo.getSelectionIndex();
		if(index > -1 && index < projectNames.size()) {
			String projectName = projectNames.get(index);
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
		}
	}

	public String getName() {
		return "Project";
	}

}
