package bndtools.launch.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public abstract class AbstractLaunchTabPiece implements ILaunchTabPiece {

	private final PropertyChangeSupport	propSupport	= new PropertyChangeSupport(this);

	private boolean						dirty		= false;

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propSupport.removePropertyChangeListener(listener);
	}

	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		propSupport.firePropertyChange(propertyName, oldValue, newValue);
	}

	protected void firePropertyChange(String propertyName, int oldValue, int newValue) {
		propSupport.firePropertyChange(propertyName, oldValue, newValue);
	}

	protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
		propSupport.firePropertyChange(propertyName, oldValue, newValue);
	}

	protected void firePropertyChange(PropertyChangeEvent evt) {
		propSupport.firePropertyChange(evt);
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	protected void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	@Override
	public String checkForError() {
		return null;
	}
}
