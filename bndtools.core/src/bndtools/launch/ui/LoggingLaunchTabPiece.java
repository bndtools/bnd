package bndtools.launch.ui;

import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
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

import bndtools.launch.LaunchConstants;

public class LoggingLaunchTabPiece extends AbstractLaunchTabPiece {

    private static final String[] LOG_LEVELS = new String[] {
        Level.OFF.toString(),
        Level.SEVERE.toString(),
        Level.WARNING.toString(),
        Level.INFO.toString(),
        Level.FINE.toString(),
        Level.ALL.toString()
    };

    private String logLevel;
    private String logOutput;

    private Combo logLevelCombo;
    private Button consoleLogButton;
    private Button fileLogButton;
    private Text fileLogPathText;
    private Button fileLogBrowseButton;

    public Control createControl(Composite parent) {
        Group loggingGroup = new Group(parent, SWT.NONE);
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
        logLevelCombo.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                String old = logLevel;
                logLevel = logLevelCombo.getText();
                setDirty(true);
                firePropertyChange("logLevel", old, logLevel);
            }
        });
        Listener logOutputListener = new Listener() {
            public void handleEvent(Event event) {
                String old = logOutput;
                logOutput = consoleLogButton.getSelection()
                    ? LaunchConstants.VALUE_LOG_OUTPUT_CONSOLE
                    : "file:" + fileLogPathText.getText();
                setDirty(true);
                firePropertyChange("logOutput", old, logOutput);

                updateFieldEnablement();
            }
        };
        fileLogButton.addListener(SWT.Selection, logOutputListener);
        consoleLogButton.addListener(SWT.Selection, logOutputListener);
        fileLogPathText.addListener(SWT.Modify, logOutputListener);

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

        return loggingGroup;    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }

    public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
        logLevel = configuration.getAttribute(LaunchConstants.ATTR_LOGLEVEL, LaunchConstants.DEFAULT_LOGLEVEL);
        logLevelCombo.setText(logLevel);

        logOutput = configuration.getAttribute(LaunchConstants.ATTR_LOG_OUTPUT, LaunchConstants.DEFAULT_LOG_OUTPUT);
        if(logOutput.startsWith("file:")) {
            String logPath = logOutput.substring("file:".length());
            consoleLogButton.setSelection(false);
            fileLogButton.setSelection(true);
            fileLogPathText.setText(logPath);
        } else {
            consoleLogButton.setSelection(true);
            fileLogButton.setSelection(false);
        }
        updateFieldEnablement();
    }

    void updateFieldEnablement() {
        boolean enable = fileLogButton.getSelection();
        fileLogPathText.setEnabled(enable);
        fileLogBrowseButton.setEnabled(enable);
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(LaunchConstants.ATTR_LOGLEVEL, LOG_LEVELS[logLevelCombo.getSelectionIndex()]);
        configuration.setAttribute(LaunchConstants.ATTR_LOG_OUTPUT, logOutput);
    }
}