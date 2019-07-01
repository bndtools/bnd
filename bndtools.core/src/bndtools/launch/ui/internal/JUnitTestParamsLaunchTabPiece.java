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

	private boolean	keepAlive	= false;
	private boolean	rerunIDE	= false;

	private Button	keepAliveButton;
	private Button	rerunIDEButton;

	@Override
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
				rerunIDEButton.setEnabled(keepAlive);
				firePropertyChange("keepAlive", old, keepAlive); //$NON-NLS-1$
			}
		});

		rerunIDEButton = new Button(group, SWT.CHECK);
		rerunIDEButton.setText(Messages.JUnitTestParamsLaunchTabPiece_labelRerunIDE);

		// Listeners
		rerunIDEButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
				boolean old = rerunIDE;
				rerunIDE = rerunIDEButton.getSelection();
				firePropertyChange("rerunIDE", old, rerunIDE); //$NON-NLS-1$
			}
		});

		// LAYOUT
		GridLayout layout = new GridLayout(1, false);
		group.setLayout(layout);

		keepAliveButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		rerunIDEButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		return group;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(LaunchConstants.ATTR_JUNIT_KEEP_ALIVE, LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE);
		configuration.setAttribute(LaunchConstants.ATTR_RERUN_IDE, LaunchConstants.DEFAULT_RERUN_IDE);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		keepAlive = configuration.getAttribute(LaunchConstants.ATTR_JUNIT_KEEP_ALIVE,
			LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE);
		if (keepAlive == LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE) {
			keepAlive = configuration.getAttribute(LaunchConstants.ATTR_OLD_JUNIT_KEEP_ALIVE,
				LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE);
		}
		keepAliveButton.setSelection(keepAlive);

		rerunIDE = configuration.getAttribute(LaunchConstants.ATTR_RERUN_IDE, LaunchConstants.DEFAULT_RERUN_IDE);
		rerunIDEButton.setSelection(rerunIDE);
		rerunIDEButton.setEnabled(keepAlive);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(LaunchConstants.ATTR_JUNIT_KEEP_ALIVE, keepAlive);
		configuration.removeAttribute(LaunchConstants.ATTR_OLD_JUNIT_KEEP_ALIVE);
		configuration.setAttribute(LaunchConstants.ATTR_RERUN_IDE, rerunIDE);
	}
}
