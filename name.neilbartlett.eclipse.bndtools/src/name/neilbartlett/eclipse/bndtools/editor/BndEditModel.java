package name.neilbartlett.eclipse.bndtools.editor;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.osgi.framework.Constants;

/**
 * A model for a Bnd file. In the first iteration, use a simple Properties
 * object; this will need to be enhanced to additionally record formatting, e.g.
 * line breaks and empty lines, and comments.
 * 
 * @author Neil Bartlett
 */
public class BndEditModel {
	
	private static final String[] KNOWN_PROPERTIES = new String[] {
		Constants.BUNDLE_SYMBOLICNAME,
		Constants.BUNDLE_VERSION
	};
	
	private final Properties properties;
	private final PropertyChangeSupport propChangeSupport = new PropertyChangeSupport(this);
	
	BndEditModel() {
		this(new Properties());
	}
	
	BndEditModel(Properties properties) {
		this.properties = properties;
	}
	
	void loadFrom(InputStream stream) throws IOException {
		// Save the old properties, if any
		Map<String, String> oldValues = new HashMap<String, String>();
		for (String name : KNOWN_PROPERTIES) {
			oldValues.put(name, properties.getProperty(name));
		}
		
		// Clear and load
		properties.clear();
		properties.load(stream);
		
		// Fire property changes on known property names
		for(Entry<String, String> entry : oldValues.entrySet()) {
			propChangeSupport.firePropertyChange(entry.getKey(), entry.getValue(), properties.get(entry.getKey()));
		}
	}
	
	void saveTo(OutputStream stream) throws IOException {
		properties.store(stream, null);
	}
	
	private void genericSet(String name, String value) {
		String oldValue = properties.getProperty(name);
		if(value == null) {
			properties.remove(name);
		} else {
			properties.setProperty(name, value);
		}
		propChangeSupport.firePropertyChange(name, oldValue, value);
	}
	
	public String getBundleSymbolicName() {
		return properties.getProperty(Constants.BUNDLE_SYMBOLICNAME);
	}
	
	public void setBundleSymbolicName(String bundleSymbolicName) {
		genericSet(Constants.BUNDLE_SYMBOLICNAME, bundleSymbolicName);
	}
	
	public String getBundleVersionString() {
		return properties.getProperty(Constants.BUNDLE_VERSION);
	}
	
	public void setBundleVersion(String bundleVersion) {
		genericSet(Constants.BUNDLE_VERSION, bundleVersion);
	}
	
	// BEGIN: PropertyChangeSupport delegate methods

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propChangeSupport.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propChangeSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propChangeSupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propChangeSupport.removePropertyChangeListener(propertyName, listener);
	}

	// END: PropertyChangeSupport delegate methods
	
}
