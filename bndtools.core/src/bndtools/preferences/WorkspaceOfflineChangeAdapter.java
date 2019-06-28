package bndtools.preferences;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

public abstract class WorkspaceOfflineChangeAdapter implements IPropertyChangeListener, WorkspaceOfflineChangeListener {
	@Override
	public final void propertyChange(PropertyChangeEvent event) {
		if (!event.getProperty()
			.equals(BndPreferences.PREF_WORKSPACE_OFFLINE)) {
			return;
		}
		workspaceOfflineChanged((boolean) event.getNewValue());
	}
}
