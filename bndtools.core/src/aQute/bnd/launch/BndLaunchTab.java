package aQute.bnd.launch;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.plugin.Central;
import bndtools.Plugin;
import bndtools.builder.BndProjectNature;
import bndtools.launch.LaunchConstants;

public class BndLaunchTab extends AbstractLaunchConfigurationTab {

    private static final String[] LOG_LEVELS = new String[] {
        Level.OFF.toString(),
        Level.SEVERE.toString(),
        Level.WARNING.toString(),
        Level.INFO.toString(),
        Level.FINE.toString(),
        Level.ALL.toString()
    };

    private Image image = null;

    private Text projectNameTxt;
    private Button dynamicUpdateBtn;
    private Button cleanBtn;
    private Combo logLevelCombo;
    private Button consoleLogButton;
    private Button fileLogButton;
    private Text fileLogPathText;
    private Button fileLogBrowseButton;

    private final Listener updateListener = new Listener() {
        public void handleEvent(Event event) {
            setDirty(true);
            checkValid();
            updateLaunchConfigurationDialog();
            updateFieldEnablement();
        }
    };


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

        Control loggingGroup = createLoggingGroup(composite);

        // Listeners
        projectNameBrowseBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doBrowseProject();
            }
        });
        projectNameTxt.addListener(SWT.Modify, updateListener);
        dynamicUpdateBtn.addListener(SWT.Selection, updateListener);
        cleanBtn.addListener(SWT.Selection, updateListener);

        // LAYOUT
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(1, true);
        composite.setLayout(layout);

        // LAYOUT - Project Group
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        projectGroup.setLayoutData(gd);

        layout = new GridLayout(2, false);
        projectGroup.setLayout(layout);
        projectNameTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // LAYOUT - Framework Group
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        frameworkGroup.setLayoutData(gd);

        layout = new GridLayout(1, false);
        frameworkGroup.setLayout(layout);

        dynamicUpdateBtn.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
        cleanBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        // LAYOUT - Logging Group
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        loggingGroup.setLayoutData(gd);
	}

    protected Control createLoggingGroup(Composite composite) {
        Group loggingGroup = new Group(composite, SWT.NONE);
        loggingGroup.setText("Launcher Logging:");

        new Label(loggingGroup, SWT.NONE).setText("Log Level:");
        logLevelCombo = new Combo(loggingGroup, SWT.DROP_DOWN | SWT.READ_ONLY);

        consoleLogButton = new Button(loggingGroup, SWT.RADIO);
        consoleLogButton.setText("Output to console.");

        fileLogButton = new Button(loggingGroup, SWT.RADIO);
        fileLogButton.setText("Write to file:");

        fileLogPathText = new Text(loggingGroup, SWT.BORDER);
        fileLogBrowseButton = new Button(loggingGroup, SWT.PUSH);
        fileLogBrowseButton.setText("Browse");

        // Load data
        logLevelCombo.setItems(LOG_LEVELS);

        // Listeners
        logLevelCombo.addListener(SWT.Selection, updateListener);
        fileLogButton.addListener(SWT.Selection, updateListener);
        consoleLogButton.addListener(SWT.Selection, updateListener);
        fileLogPathText.addListener(SWT.Modify, updateListener);

        // LAYOUT
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(4, false);
        loggingGroup.setLayout(layout);

        gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
        logLevelCombo.setLayoutData(gd);

        gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1);
        consoleLogButton.setLayoutData(gd);

        gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        fileLogButton.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        fileLogPathText.setLayoutData(gd);

        return loggingGroup;
    }

    void doBrowseProject() {
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new WorkbenchLabelProvider());
        dialog.setTitle("Project Selection");

        dialog.setMessage("Select a project to constrain your search.");

        List<IProject> projects = loadProjects();
        dialog.setElements(projects.toArray(new IProject[projects.size()]));

        if (Window.OK == dialog.open()) {
            IProject selected = (IProject) dialog.getFirstResult();
            projectNameTxt.setText(selected.getName());
        }
    }

    void checkValid() {
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

            String logLevelStr = configuration.getAttribute(LaunchConstants.ATTR_LOGLEVEL, LaunchConstants.DEFAULT_LOGLEVEL);
            logLevelCombo.setText(logLevelStr);

            String logOutput = configuration.getAttribute(LaunchConstants.ATTR_LOG_OUTPUT, LaunchConstants.DEFAULT_LOG_OUTPUT);
            if(logOutput.startsWith("file:")) {
                logOutput = logOutput.substring("file:".length());
                consoleLogButton.setSelection(false);
                fileLogButton.setSelection(true);
                fileLogPathText.setText(logOutput);
            } else {
                consoleLogButton.setSelection(true);
                fileLogButton.setSelection(false);
            }

            updateFieldEnablement();
        } catch (CoreException e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error reading launch configuration.", e));
        }
    }

	void updateFieldEnablement() {
	    boolean enable = fileLogButton.getSelection();
        fileLogPathText.setEnabled(enable);
        fileLogBrowseButton.setEnabled(enable);
	}

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectNameTxt.getText());
        configuration.setAttribute(LaunchConstants.ATTR_DYNAMIC_BUNDLES, dynamicUpdateBtn.getSelection());
        configuration.setAttribute(LaunchConstants.ATTR_CLEAN, cleanBtn.getSelection());
        configuration.setAttribute(LaunchConstants.ATTR_LOGLEVEL, LOG_LEVELS[logLevelCombo.getSelectionIndex()]);
        String logOutput = consoleLogButton.getSelection()
            ? LaunchConstants.VALUE_LOG_OUTPUT_CONSOLE
            : "file:" + fileLogPathText.getText();
        configuration.setAttribute(LaunchConstants.ATTR_LOG_OUTPUT, logOutput);
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