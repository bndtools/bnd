package bndtools.launch.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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

import bndtools.launch.LaunchConstants;

public class JUnitTestParamsLaunchTabPiece extends AbstractLaunchTabPiece {

    private boolean keepAlive = false;
    private String startTimeoutStr = LaunchConstants.DEFAULT_LAUNCH_JUNIT_START_TIMEOUT;

    private Button keepAliveButton;
    private Text timeoutText;

    public Control createControl(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText(Messages.JUnitTestParamsLaunchTabPiece_title);

        keepAliveButton = new Button(group, SWT.CHECK);
        keepAliveButton.setText(Messages.JUnitTestParamsLaunchTabPiece_labelKeepAlive);

        new Label(group, SWT.NONE).setText(Messages.JUnitTestParamsLaunchTabPiece_labelStartingTimeout);
        timeoutText = new Text(group, SWT.BORDER);
        timeoutText.setEnabled(!keepAlive);

        FieldDecoration infoDecor = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);

        ControlDecoration timeoutDecor = new ControlDecoration(timeoutText, SWT.LEFT | SWT.CENTER, group);
        timeoutDecor.setImage(infoDecor.getImage());
        timeoutDecor.setShowHover(true);
        timeoutDecor.setDescriptionText(Messages.JUnitTestParamsLaunchTabPiece_descStartingTimeout);

        // Listeners
        keepAliveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setDirty(true);
                boolean old = keepAlive;
                keepAlive = keepAliveButton.getSelection();
                firePropertyChange("keepAlive", old, keepAlive); //$NON-NLS-1$
                timeoutText.setEnabled(!keepAlive);
            }
        });
        timeoutText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setDirty(true);
                String old = startTimeoutStr;
                startTimeoutStr = timeoutText.getText();
                firePropertyChange("startTimeout", old, startTimeoutStr); //$NON-NLS-1$
            }
        });

        // Layout
        GridData gd;

        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        group.setLayout(layout);

        gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        keepAliveButton.setLayoutData(gd);

        gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gd.widthHint = 50;
        timeoutText.setLayoutData(gd);

        return group;
    }

    @Override
    public String checkForError() {
        String error = null;

        try {
            Integer.parseInt(startTimeoutStr);
        } catch (NumberFormatException e) {
            error = Messages.JUnitTestParamsLaunchTabPiece_errorTimeoutValue;
        }

        return error;
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(LaunchConstants.PROP_LAUNCH_JUNIT_KEEP_ALIVE, false);
        configuration.setAttribute(LaunchConstants.PROP_LAUNCH_JUNIT_START_TIMEOUT, LaunchConstants.DEFAULT_LAUNCH_JUNIT_START_TIMEOUT);
    }

    public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
        keepAlive = configuration.getAttribute(LaunchConstants.PROP_LAUNCH_JUNIT_KEEP_ALIVE, false);
        keepAliveButton.setSelection(keepAlive);
        timeoutText.setEnabled(!keepAlive);

        startTimeoutStr = configuration.getAttribute(LaunchConstants.PROP_LAUNCH_JUNIT_START_TIMEOUT, LaunchConstants.DEFAULT_LAUNCH_JUNIT_START_TIMEOUT);
        timeoutText.setText(startTimeoutStr);
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(LaunchConstants.PROP_LAUNCH_JUNIT_KEEP_ALIVE, keepAlive);
        configuration.setAttribute(LaunchConstants.PROP_LAUNCH_JUNIT_START_TIMEOUT, startTimeoutStr);
    }
}
