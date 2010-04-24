package bndtools.launch.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.plugin.Central;
import bndtools.Plugin;
import bndtools.builder.BndProjectNature;

public class ProjectLaunchTabPiece extends AbstractLaunchTabPiece {

    private String projectName = "";
    private Text projectNameTxt;

    public Control createControl(Composite parent) {
        Group projectGroup = new Group(parent, SWT.NONE);
        projectGroup.setText("Project:");

        projectNameTxt = new Text(projectGroup, SWT.BORDER);
        Button projectNameBrowseBtn = new Button(projectGroup, SWT.PUSH);
        projectNameBrowseBtn.setText("Browse");

        // LISTENERS
        projectNameBrowseBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doBrowseProject();
            }
        });
        projectNameTxt.addListener(SWT.Modify, new Listener() {
            public void handleEvent(Event event) {
                String oldName = projectName;
                projectName = projectNameTxt.getText();
                setDirty(true);
                firePropertyChange("projectName", oldName, projectName);
            }
        });

        // LAYOUT
        GridLayout layout = new GridLayout(2, false);
        projectGroup.setLayout(layout);
        projectNameTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        return projectGroup;
    }

    void doBrowseProject() {
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(projectNameTxt.getShell(), new WorkbenchLabelProvider());
        dialog.setTitle("Project Selection");

        dialog.setMessage("Select a project to constrain your search.");

        List<IProject> projects = loadProjects();
        dialog.setElements(projects.toArray(new IProject[projects.size()]));

        if (Window.OK == dialog.open()) {
            IProject selected = (IProject) dialog.getFirstResult();
            projectNameTxt.setText(selected.getName());
        }
    }

    List<IProject> loadProjects() {
        Collection<Project> projects;
        try {
            Workspace workspace = Central.getWorkspace();
            projects = workspace.getAllProjects();
        } catch (Exception e) {
            Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Internal error querying projects.", e));
            return Collections.emptyList();
        }
        List<IProject> result = new ArrayList<IProject>(projects.size());
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        for (Project project : projects) {
            IProject iproject = workspaceRoot.getProject(project.getName());
            if (iproject != null && iproject.isOpen()) {
                result.add(iproject);
            }
        }
        return result;
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectNameTxt.getText());
    }

    public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
        projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
        if (projectName != null) {
            projectNameTxt.setText(projectName);
        }
    }

    @Override
    public String checkForError() {
        String projectName = projectNameTxt.getText();
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (project == null || !project.exists()) {
            return MessageFormat.format("Project {0} does not exist.", projectName);
        }
        if (!project.isOpen()) {
            return MessageFormat.format("Project {0} is closed.", projectName);
        }
        try {
            if (!project.hasNature(BndProjectNature.NATURE_ID)) {
                return MessageFormat.format("Project {0} is not a Bnd OSGi project.", projectName);
            }
        } catch (CoreException e) {
            Plugin.logError("Error checking for Bnd OSGi project nature", e);
            return "Error checking for Bnd OSGi project nature";
        }
        return null;
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }
}