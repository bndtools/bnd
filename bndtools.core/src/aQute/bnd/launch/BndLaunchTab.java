package aQute.bnd.launch;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.plugin.Central;
import bndtools.Plugin;
import bndtools.builder.BndProjectNature;
import bndtools.launch.LaunchConstants;

public class BndLaunchTab extends AbstractLaunchConfigurationTab {

    private Image image = null;

    private Text projectNameTxt;
    private Button dynamicUpdateBtn;
    private Button cleanBtn;

	private List<String> projectNames;


	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		setControl(composite);
		
        Group projectGroup = new Group(composite, SWT.NONE);
        projectGroup.setText("Project:");

        projectNameTxt = new Text(projectGroup, SWT.BORDER);
        Button projectNameBrowseBtn = new Button(projectGroup, SWT.PUSH);
        projectNameBrowseBtn.setText("Browse");

        Group frameworkGroup = new Group(composite, SWT.NONE);
        frameworkGroup.setText("Framework:");

        dynamicUpdateBtn = new Button(frameworkGroup, SWT.CHECK);
        dynamicUpdateBtn.setText("Update bundles during runtime.");

        cleanBtn = new Button(frameworkGroup, SWT.CHECK);
        cleanBtn.setText("Clean storage area before launch.");

        // Listeners
        Listener updateListener = new Listener() {
            public void handleEvent(Event event) {
                setDirty(true);
                checkValid();
                updateLaunchConfigurationDialog();
            }
        };
        projectNameTxt.addListener(SWT.Modify, updateListener);
        dynamicUpdateBtn.addListener(SWT.Selection, updateListener);
        cleanBtn.addListener(SWT.Selection, updateListener);
		
		// Layout
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(1, true);
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        projectGroup.setLayoutData(gd);

        layout = new GridLayout(2, false);
        projectGroup.setLayout(layout);
        projectNameTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        frameworkGroup.setLayoutData(gd);

        layout = new GridLayout(1, false);
        frameworkGroup.setLayout(layout);
        dynamicUpdateBtn.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
        cleanBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}

    void checkValid() {
        String error = null;

        // Check project
        String projectName = projectNameTxt.getText();
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (project == null || !project.exists()) {
            setErrorMessage(MessageFormat.format("Project {0} does not exist.", projectName));
            return;
        }
        if (!project.isOpen()) {
            setErrorMessage(MessageFormat.format("Project {0} is closed.", projectName));
            return;
        }
        try {
            if (!project.hasNature(BndProjectNature.NATURE_ID)) {
                setErrorMessage(MessageFormat.format("Project {0} is not a Bnd OSGi project.", projectName));
                return;
            }
        } catch (CoreException e) {
            setErrorMessage("Error checking for Bnd OSGi project nature");
            Plugin.logError("Error checking for Bnd OSGi project nature", e);
        }

        setErrorMessage(null);
    }

	List<String> loadProjectNames() {
		Collection<Project> projects;
		try {
			Workspace workspace = Central.getWorkspace();
			projects = workspace.getAllProjects();
		} catch (Exception e) {
			Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Internal error querying projects.", e));
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
            if (projectName != null) {
                projectNameTxt.setText(projectName);
            }

            boolean dynamicBundles = configuration.getAttribute(LaunchConstants.ATTR_DYNAMIC_BUNDLES, LaunchConstants.DEFAULT_DYNAMIC_BUNDLES);
            dynamicUpdateBtn.setSelection(dynamicBundles);

            boolean clean = configuration.getAttribute(LaunchConstants.ATTR_CLEAN, LaunchConstants.DEFAULT_CLEAN);
            cleanBtn.setSelection(clean);
        } catch (CoreException e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error reading launch configuration.", e));
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectNameTxt.getText());
        configuration.setAttribute(LaunchConstants.ATTR_DYNAMIC_BUNDLES, dynamicUpdateBtn.getSelection());
    }

    public String getName() {
        return "Main";
    }

    @Override
    public Image getImage() {
        synchronized (this) {
            if (image == null) {
                image = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();
            }
        }
        return image;
    }

    @Override
    public void dispose() {
        super.dispose();
        synchronized (this) {
            if (image != null)
                image.dispose();
        }
    }
}