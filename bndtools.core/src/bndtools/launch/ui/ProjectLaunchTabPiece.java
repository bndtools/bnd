package bndtools.launch.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.window.Window;
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
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.launch.LaunchConstants;
import bndtools.utils.FileExtensionFilter;

public class ProjectLaunchTabPiece extends AbstractLaunchTabPiece {
	@SuppressWarnings("deprecation")
	private static final String		ATTR_LOGLEVEL		= LaunchConstants.ATTR_LOGLEVEL;
	private static final ILogger	logger				= Logger.getLogger(ProjectLaunchTabPiece.class);

	private static final String		PROP_LAUNCH_TARGET	= "targetName";
	private static final String		PROP_ENABLE_TRACE	= "enableTrace";

	// Model State
	private String					targetName			= null;
	private boolean					enableTrace			= false;

	// View
	private Text					launchTargetTxt;
	private Button					enableTraceBtn;

	@Override
	@SuppressWarnings("unused")
	public Control createControl(Composite parent) {
		Group projectGroup = new Group(parent, SWT.NONE);
		projectGroup.setText("Launch:");

		launchTargetTxt = new Text(projectGroup, SWT.BORDER);

		Button projectNameBrowseBtn = new Button(projectGroup, SWT.PUSH);
		projectNameBrowseBtn.setText("Browse Projects");

		new Label(projectGroup, SWT.NONE); // Spacer

		Button bndrunBrowseBtn = new Button(projectGroup, SWT.PUSH);
		bndrunBrowseBtn.setText("Browse Run Files");

		enableTraceBtn = new Button(projectGroup, SWT.CHECK);
		enableTraceBtn.setText("Enable launcher tracing.");

		// LISTENERS
		projectNameBrowseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doBrowseProject();
			}
		});
		bndrunBrowseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doBrowseBndrun();
			}
		});
		launchTargetTxt.addListener(SWT.Modify, event -> {
			String oldName = targetName;
			targetName = launchTargetTxt.getText();
			setDirty(true);
			firePropertyChange(PROP_LAUNCH_TARGET, oldName, targetName);
		});
		enableTraceBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean oldValue = enableTrace;
				enableTrace = enableTraceBtn.getSelection();
				setDirty(true);
				firePropertyChange(PROP_ENABLE_TRACE, oldValue, enableTrace);
			}
		});

		// LAYOUT
		GridLayout layout = new GridLayout(2, false);
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		projectGroup.setLayout(layout);
		launchTargetTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		projectNameBrowseBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		bndrunBrowseBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		return projectGroup;
	}

	void doBrowseProject() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(launchTargetTxt.getShell(),
			new WorkbenchLabelProvider());
		dialog.setTitle("Project Selection");

		dialog.setMessage("Select a project to constrain your search.");

		List<IProject> projects = loadProjects();
		dialog.setElements(projects.toArray());

		if (Window.OK == dialog.open()) {
			IProject selected = (IProject) dialog.getFirstResult();
			launchTargetTxt.setText(selected.getName());
		}
	}

	void doBrowseBndrun() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(launchTargetTxt.getShell(),
			new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setValidator(selection -> {
			if (selection.length > 0 && selection[0] instanceof IFile) {
				return new Status(IStatus.OK, Plugin.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
			}
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.ERROR, "", null); //$NON-NLS-1$
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle("Run File Selection");
		dialog.setMessage("Select the Run File to launch.");
		dialog.addFilter(new FileExtensionFilter(LaunchConstants.EXT_BNDRUN));
		dialog.setInput(ResourcesPlugin.getWorkspace());

		if (dialog.open() == Window.OK) {
			Object[] files = dialog.getResult();
			if (files != null && files.length == 1) {
				IPath path = ((IResource) files[0]).getFullPath()
					.makeRelative();
				launchTargetTxt.setText(path.toString());
			} else {
				launchTargetTxt.setText("");
			}
		}
	}

	static List<IProject> loadProjects() {
		Collection<Project> projects;
		try {
			Workspace workspace = Central.getWorkspace();
			projects = workspace.getAllProjects();
		} catch (Exception e) {
			logger.logError("Internal error querying projects.", e);
			return Collections.emptyList();
		}
		List<IProject> result = new ArrayList<>(projects.size());
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace()
			.getRoot();
		for (Project project : projects) {
			IProject iproject = workspaceRoot.getProject(project.getName());
			if (iproject != null && iproject.isOpen()) {
				result.add(iproject);
			}
		}
		return result;
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(LaunchConstants.ATTR_LAUNCH_TARGET, launchTargetTxt.getText());

		IResource targetResource = ResourcesPlugin.getWorkspace()
			.getRoot()
			.findMember(launchTargetTxt.getText());
		if (targetResource != null && targetResource.exists()) {
			IProject project = targetResource.getProject();
			if (project != null)
				configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
			else
				configuration.removeAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME);
		}

		configuration.setAttribute(LaunchConstants.ATTR_TRACE, enableTrace);
		configuration.removeAttribute(ATTR_LOGLEVEL);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		targetName = configuration.getAttribute(LaunchConstants.ATTR_LAUNCH_TARGET, (String) null);
		if (targetName == null)
			targetName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
		if (targetName != null) {
			launchTargetTxt.setText(targetName);
		}
		enableTrace = configuration.getAttribute(LaunchConstants.ATTR_TRACE, LaunchConstants.DEFAULT_TRACE);
		enableTraceBtn.setSelection(enableTrace);
	}

	@Override
	public String checkForError() {
		if (targetName == null || targetName.length() == 0) {
			return "Launch target must be specified";
		}

		IResource targetResource = ResourcesPlugin.getWorkspace()
			.getRoot()
			.findMember(targetName);
		if (targetResource == null || !targetResource.exists()) {
			return MessageFormat.format("Launch target {0} does not exist.", targetName);
		}

		if (targetResource.getType() == IResource.PROJECT) {
			IProject project = (IProject) targetResource;
			if (!project.isOpen()) {
				return MessageFormat.format("Project {0} is closed.", targetName);
			}
			try {
				if (!project.hasNature(BndtoolsConstants.NATURE_ID)) {
					return MessageFormat.format("Project {0} is not a Bnd OSGi project.", targetName);
				}
			} catch (CoreException e) {
				logger.logError("Error checking for Bnd OSGi project nature", e);
				return "Error checking for Bnd OSGi project nature";
			}
		} else if (targetResource.getType() == IResource.FILE) {
			if (!targetResource.getName()
				.endsWith(LaunchConstants.EXT_BNDRUN)) {
				return MessageFormat.format("Selected file {0} is not a .bndrun file.", targetName);
			}
		}
		return null;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(LaunchConstants.ATTR_TRACE, LaunchConstants.DEFAULT_TRACE);
	}
}
