package bndtools.launch.ui;

import java.beans.PropertyChangeListener;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public interface ILaunchTabPiece {
	Control createControl(Composite parent);

	void addPropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	boolean isDirty();

	void setDefaults(ILaunchConfigurationWorkingCopy configuration);

	void initializeFrom(ILaunchConfiguration configuration) throws CoreException;

	void performApply(ILaunchConfigurationWorkingCopy configuration);

	String checkForError();
}
