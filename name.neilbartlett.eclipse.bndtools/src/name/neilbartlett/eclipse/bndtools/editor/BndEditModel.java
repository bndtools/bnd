package name.neilbartlett.eclipse.bndtools.editor;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.osgi.framework.Constants;

import aQute.libg.header.OSGiHeader;

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
		Constants.BUNDLE_VERSION,
		Constants.BUNDLE_ACTIVATOR,
		Constants.EXPORT_PACKAGE,
		aQute.lib.osgi.Constants.PRIVATE_PACKAGE,
		aQute.lib.osgi.Constants.SOURCES,
		aQute.lib.osgi.Constants.VERSIONPOLICY
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
	private void genericSet(String name, Object oldValue, Object newValue, String newString) {
		if(newValue == null) {
			properties.remove(name);
		} else {
			properties.setProperty(name, newString);
		}
		propChangeSupport.firePropertyChange(name, oldValue, newValue);
	}
	
	public String getBundleSymbolicName() {
		return properties.getProperty(Constants.BUNDLE_SYMBOLICNAME);
	}
	
	public void setBundleSymbolicName(String bundleSymbolicName) {
		genericSet(Constants.BUNDLE_SYMBOLICNAME, getBundleSymbolicName(), bundleSymbolicName, bundleSymbolicName); 
	}
	
	public String getBundleVersionString() {
		return properties.getProperty(Constants.BUNDLE_VERSION);
	}
	
	public void setBundleVersion(String bundleVersion) {
		genericSet(Constants.BUNDLE_VERSION, getBundleVersionString(), bundleVersion, bundleVersion);
	}
	
	public String getBundleActivator() {
		return properties.getProperty(Constants.BUNDLE_ACTIVATOR);
	}
	
	public void setBundleActivator(String bundleActivator) {
		genericSet(Constants.BUNDLE_ACTIVATOR, getBundleActivator(), bundleActivator, bundleActivator);
	}
	
	public void setIncludeSources(boolean includeSources) {
		boolean oldValue = isIncludeSources();
		if(includeSources) {
			properties.setProperty(aQute.lib.osgi.Constants.SOURCES, Boolean.TRUE.toString());
		} else {
			properties.remove(aQute.lib.osgi.Constants.SOURCES);
		}
		propChangeSupport.firePropertyChange(aQute.lib.osgi.Constants.SOURCES, oldValue, includeSources);
	}
	
	public boolean isIncludeSources() {
		return Boolean.parseBoolean(properties.getProperty(aQute.lib.osgi.Constants.SOURCES));
	}
	
	public VersionPolicy getVersionPolicy() throws IllegalArgumentException {
		String string = properties.getProperty(aQute.lib.osgi.Constants.VERSIONPOLICY);
		return string != null ? VersionPolicy.parse(string) : null;
	}
	
	public void setVersionPolicy(VersionPolicy versionPolicy) {
		String string = versionPolicy != null ? versionPolicy.toString() : null;
		VersionPolicy oldValue;
		try {
			oldValue = getVersionPolicy();
		} catch (IllegalArgumentException e) {
			oldValue = null;
		}
		genericSet(aQute.lib.osgi.Constants.VERSIONPOLICY, oldValue, versionPolicy, string); 
	}
	/**
	 * Get the exported packages; the returned collection will have been newly
	 * allocated, and may be manipulated by clients without fear of affecting
	 * other clients.
	 * 
	 * @return A new collection containing the exported packages of the model.
	 */
	public Collection<ExportedPackage> getExportedPackages() {
		List<ExportedPackage> result = new LinkedList<ExportedPackage>();
		
		String exportsStr = properties.getProperty(Constants.EXPORT_PACKAGE);
		Map<String, Map<String, String>> header = OSGiHeader.parseHeader(exportsStr, null);
		
		for (Entry<String, Map<String,String>> entry : header.entrySet()) {
			String packageName = entry.getKey();
			String version = null;
			Map<String, String> attribs = entry.getValue();
			if(attribs != null) {
				version = attribs.get(Constants.VERSION_ATTRIBUTE);
			}
			
			result.add(new ExportedPackage(packageName, version));
		}
		return result;
	}
	public Collection<String> getPrivatePackages() {
		List<String> packages = new LinkedList<String>();
		
		String pkgsStr = properties.getProperty(aQute.lib.osgi.Constants.PRIVATE_PACKAGE);
		Map<String, Map<String, String>> header = OSGiHeader.parseHeader(pkgsStr);
		
		for(Entry<String, Map<String,String>> entry : header.entrySet()) {
			String packageName = entry.getKey();
			packages.add(packageName);
		}
		return packages;
	}
	public void setExportedPackages(Collection<? extends ExportedPackage> packages) {
		Collection<ExportedPackage> oldPackages = getExportedPackages();
		StringBuilder buffer = new StringBuilder();
		
		if(packages == null || packages.isEmpty()) {
			properties.remove(Constants.EXPORT_PACKAGE);
		} else {
			for(Iterator<? extends ExportedPackage> iter = packages.iterator(); iter.hasNext(); ) {
				ExportedPackage pkg = iter.next();
				buffer.append(pkg.getPackageName());
				
				if(pkg.getVersion() != null)
					buffer.append(";version=\"").append(pkg.getVersion()).append("\"");
				
				if(iter.hasNext())
					buffer.append(',');
			}
			properties.setProperty(Constants.EXPORT_PACKAGE, buffer.toString());
		}
		propChangeSupport.firePropertyChange(Constants.EXPORT_PACKAGE, oldPackages, packages);
	}
	public void setPrivatePackages(Collection<? extends String> packages) {
		Collection<String> oldPackages = getPrivatePackages();
		
		StringBuilder buffer = new StringBuilder();
		if(packages == null || packages.isEmpty()) {
			properties.remove(aQute.lib.osgi.Constants.PRIVATE_PACKAGE);
		} else {
			for(Iterator<? extends String> iter = packages.iterator(); iter.hasNext(); ) {
				String pkg = iter.next();
				buffer.append(pkg);
				if(iter.hasNext())
					buffer.append(',');
			}
			properties.setProperty(aQute.lib.osgi.Constants.PRIVATE_PACKAGE, buffer.toString());
		}
		propChangeSupport.firePropertyChange(aQute.lib.osgi.Constants.PRIVATE_PACKAGE, oldPackages, packages);
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
