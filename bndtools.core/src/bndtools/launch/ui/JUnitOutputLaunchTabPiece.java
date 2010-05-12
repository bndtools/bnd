package bndtools.launch.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import bndtools.launch.LaunchConstants;

public class JUnitOutputLaunchTabPiece extends AbstractLaunchTabPiece {

    private Button portButton;
    private Button fileButton;
    private Text filePathText;
    private Button filePathBrowseButton;

    private String reporter;

    public Control createControl(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Test Results Output");

        portButton = new Button(group, SWT.RADIO);
        portButton.setText("Display in Workbench");

        fileButton = new Button(group, SWT.RADIO);
        fileButton.setText("Report to XML file:");

        filePathText = new Text(group, SWT.BORDER);
        filePathBrowseButton = new Button(group, SWT.PUSH);
        filePathBrowseButton.setText("Browse");

        // Listeners
        Listener l = new Listener() {
            public void handleEvent(Event event) {
                String old = reporter;
                reporter = portButton.getSelection()
                    ? "port"
                    : "file:" + filePathText.getText();
                setDirty(true);
                firePropertyChange("reporter", old, reporter);

                updateFieldEnablement();
            }
        };
        portButton.addListener(SWT.Selection, l);
        fileButton.addListener(SWT.Selection, l);
        filePathText.addListener(SWT.Modify, l);

        // LAYOUT
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(3, false);
        group.setLayout(layout);

        gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
        portButton.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        filePathText.setLayoutData(gd);

        return group;
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }

    public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
        reporter = configuration.getAttribute(LaunchConstants.ATTR_JUNIT_REPORTER, "");
        if(reporter.startsWith("file:")) {
            String outputPath = reporter.substring("file:".length());
            portButton.setSelection(false);
            fileButton.setSelection(true);
            filePathText.setText(outputPath);
        } else {
            portButton.setSelection(true);
            fileButton.setSelection(false);
        }
        updateFieldEnablement();
    }

    void updateFieldEnablement() {
        boolean enable = fileButton.getSelection();
        filePathText.setEnabled(enable);
        filePathBrowseButton.setEnabled(enable);
    }

    @Override
    public String checkForError() {
        String error = null;

        if(fileButton.getSelection()) {
            String text = filePathText.getText();
            if(text == null || text.length() == 0) {
                error = "Test report file name must be specified.";
            }
        }

        return error;
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(LaunchConstants.ATTR_JUNIT_REPORTER, reporter);
    }
}
