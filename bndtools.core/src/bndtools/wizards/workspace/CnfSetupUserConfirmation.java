package bndtools.wizards.workspace;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

class CnfSetupUserConfirmation {

    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    enum Decision {
        SETUP, SKIP, NEVER
    }

    private Decision decision = Decision.SETUP;

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        Decision old = this.decision;
        this.decision = decision;
        propSupport.firePropertyChange("decision", old, decision);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propSupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propSupport.removePropertyChangeListener(propertyName, listener);
    }

}
