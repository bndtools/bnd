package bndtools.launch.ui.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import bndtools.launch.LaunchConstants;
import bndtools.launch.ui.AbstractLaunchTabPiece;

public class JUnitTestParamsLaunchTabPiece extends AbstractLaunchTabPiece {

    private boolean keepAlive = false;

    private Button keepAliveButton;

    public Control createControl(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText(Messages.JUnitTestParamsLaunchTabPiece_title);

        keepAliveButton = new Button(group, SWT.CHECK);
        keepAliveButton.setText(Messages.JUnitTestParamsLaunchTabPiece_labelKeepAlive);

        // Listeners
        keepAliveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setDirty(true);
                boolean old = keepAlive;
                keepAlive = keepAliveButton.getSelection();
                firePropertyChange("keepAlive", old, keepAlive); //$NON-NLS-1$
            }
        });

        // Layout
        GridData gd;

        GridLayout layout = new GridLayout(1, false);
        group.setLayout(layout);

        gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        keepAliveButton.setLayoutData(gd);

        return group;
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(LaunchConstants.ATTR_JUNIT_KEEP_ALIVE, LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE);
    }

    @SuppressWarnings("deprecation")
    public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
        keepAlive = configuration.getAttribute(LaunchConstants.ATTR_JUNIT_KEEP_ALIVE, LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE);
        if (keepAlive == LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE) {
            keepAlive = configuration.getAttribute(LaunchConstants.ATTR_OLD_JUNIT_KEEP_ALIVE, LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE);
        }
        keepAliveButton.setSelection(keepAlive);
    }

    @SuppressWarnings("deprecation")
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(LaunchConstants.ATTR_JUNIT_KEEP_ALIVE, keepAlive);
        configuration.removeAttribute(LaunchConstants.ATTR_OLD_JUNIT_KEEP_ALIVE);
    }
}
