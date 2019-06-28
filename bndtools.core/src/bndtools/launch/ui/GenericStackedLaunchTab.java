package bndtools.launch.ui;

import java.beans.PropertyChangeListener;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import bndtools.Plugin;

public abstract class GenericStackedLaunchTab extends AbstractLaunchConfigurationTab {

	private ILaunchTabPiece[] stack = null;

	protected abstract ILaunchTabPiece[] createStack();

	private synchronized ILaunchTabPiece[] getStack() {
		if (stack == null) {
			stack = createStack();
		}
		return stack;
	}

	private final PropertyChangeListener updateListener = evt -> {
		checkValid();
		updateLaunchConfigurationDialog();
	};

	@Override
	protected boolean isDirty() {
		for (ILaunchTabPiece piece : getStack()) {
			if (piece.isDirty())
				return true;
		}
		return false;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		setControl(composite);
		composite.setLayout(new GridLayout(1, false));

		for (ILaunchTabPiece piece : getStack()) {
			Control control = piece.createControl(composite);
			control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			piece.addPropertyChangeListener(updateListener);
		}
	}

	void checkValid() {
		for (ILaunchTabPiece piece : getStack()) {
			String error = piece.checkForError();
			if (error != null) {
				setErrorMessage(error);
				return;
			}
		}
		setErrorMessage(null);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		for (ILaunchTabPiece piece : getStack()) {
			piece.setDefaults(configuration);
		}
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			for (ILaunchTabPiece piece : getStack()) {
				piece.initializeFrom(configuration);
			}
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error reading launch configuration.", e));
		}

		checkValid();
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		for (ILaunchTabPiece piece : getStack()) {
			piece.performApply(configuration);
		}
	}
}
