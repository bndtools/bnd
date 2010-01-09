package name.neilbartlett.eclipse.bndtools.editor.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.osgi.framework.Constants;

import aQute.lib.osgi.Processor;
import aQute.libg.header.OSGiHeader;

/**
 * A model for a Bnd file. In the first iteration, use a simple Properties
 * object; this will need to be enhanced to additionally record formatting, e.g.
 * line breaks and empty lines, and comments.
 * 
 * @author Neil Bartlett
 */
public class BndEditModel {
	
	private static final String LIST_SEPARATOR = ",\\\t";
	private static final String ISO_8859_1 = "ISO-8859-1"; //$NON-NLS-1$
	private final Set<String> touchedProperties = new HashSet<String>();
	
	private static final String[] KNOWN_PROPERTIES = new String[] {
		Constants.BUNDLE_SYMBOLICNAME,
		Constants.BUNDLE_VERSION,
		Constants.BUNDLE_ACTIVATOR,
		Constants.EXPORT_PACKAGE,
		Constants.IMPORT_PACKAGE,
		aQute.lib.osgi.Constants.PRIVATE_PACKAGE,
		aQute.lib.osgi.Constants.SOURCES,
		aQute.lib.osgi.Constants.VERSIONPOLICY,
		aQute.lib.osgi.Constants.SERVICE_COMPONENT,
	};
	
	private final PropertyChangeSupport propChangeSupport = new PropertyChangeSupport(this);
	private final Properties properties = new Properties();;
	
	public void loadFrom(IDocument document) throws IOException {
		// Save the old properties, if any
		Map<String, String> oldValues = new HashMap<String, String>();
		for (String name : KNOWN_PROPERTIES) {
			oldValues.put(name, properties.getProperty(name));
		}
		
		// Clear and load
		properties.clear();
		InputStream stream = new ByteArrayInputStream(document.get().getBytes(ISO_8859_1));
		properties.load(stream);
		touchedProperties.clear();
		
		// Fire property changes on known property names
		for(Entry<String, String> entry : oldValues.entrySet()) {
			propChangeSupport.firePropertyChange(entry.getKey(), entry.getValue(), properties.get(entry.getKey()));
		}
	}
	
	public void saveChangesTo(IDocument document) {
		for(Iterator<String> iter = touchedProperties.iterator(); iter.hasNext(); ) {
			String property = iter.next();
			iter.remove();
			
			String value = properties.getProperty(property);
			updateDocument(document, property, value);
		}
	}
	
	private static IRegion findEntry(IDocument document, String name) throws BadLocationException {
		int lineCount = document.getNumberOfLines();
		
		int entryStart = -1;
		int entryLength = 0;
		
		for(int i=0; i<lineCount; i++) {
			IRegion lineRegion = document.getLineInformation(i);
			String line = document.get(lineRegion.getOffset(), lineRegion.getLength());
			if(line.startsWith(name)) {
				entryStart = lineRegion.getOffset();
				entryLength = lineRegion.getLength();
				
				// Handle continuation lines, where the current line ends with a blackslash.
				while(document.getChar(lineRegion.getOffset() + lineRegion.getLength() - 1) == '\\') {
					if(++i >= lineCount) {
						break;
					}
					lineRegion = document.getLineInformation(i);
					entryLength += lineRegion.getLength() + 1; // Extra 1 is required for the newline
				}
				
				return new Region(entryStart, entryLength);
			}
		}
		
		return null;
	}
	
	private static void updateDocument(IDocument document, String name, String value) {
		String newEntry;
		if(value != null) {
			StringBuilder buffer = new StringBuilder();
			buffer.append(name).append(": ").append(value);
			newEntry = buffer.toString();
		} else {
			newEntry = "";
		}
		
		try {
			IRegion region = findEntry(document, name);
			if(region != null) {
				// Replace an existing entry
				int offset = region.getOffset();
				int length = region.getLength();
				
				// If the replacement is empty, remove one extra character to the left, i.e. the newline
				if(newEntry.length() == 0) {
					offset--; length++;
				}
				document.replace(offset, length, newEntry);
			} else if(newEntry.length() > 0) {
				// This is a new entry, put it at the end of the file
				if(document.getChar(document.getLength() - 1) != '\n')
					newEntry = "\n" + newEntry;
				document.replace(document.getLength(), 0, newEntry);
			}
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void doSetBoolean(String name, boolean defaultValue, boolean newValue) {
		boolean oldValue = Boolean.parseBoolean(properties.getProperty(name, Boolean.toString(defaultValue)));
		if(newValue != defaultValue) {
			properties.setProperty(name, Boolean.toString(newValue));
		} else {
			properties.remove(name);
		}
		touchedProperties.add(name);
		propChangeSupport.firePropertyChange(name, oldValue, newValue);
	}
	
	private void doSetObject(String name, Object oldValue, Object newValue, String newString) {
		if(newValue == null) {
			properties.remove(name);
		} else {
			properties.setProperty(name, newString);
		}
		touchedProperties.add(name);
		propChangeSupport.firePropertyChange(name, oldValue, newValue);
	}
	
	public String getBundleSymbolicName() {
		return properties.getProperty(Constants.BUNDLE_SYMBOLICNAME);
	}
	
	public void setBundleSymbolicName(String bundleSymbolicName) {
		doSetObject(Constants.BUNDLE_SYMBOLICNAME, getBundleSymbolicName(), bundleSymbolicName, bundleSymbolicName); 
	}
	
	public String getBundleVersionString() {
		return properties.getProperty(Constants.BUNDLE_VERSION);
	}
	
	public void setBundleVersion(String bundleVersion) {
		doSetObject(Constants.BUNDLE_VERSION, getBundleVersionString(), bundleVersion, bundleVersion);
	}
	
	public String getBundleActivator() {
		return properties.getProperty(Constants.BUNDLE_ACTIVATOR);
	}
	
	public void setBundleActivator(String bundleActivator) {
		doSetObject(Constants.BUNDLE_ACTIVATOR, getBundleActivator(), bundleActivator, bundleActivator);
	}
	
	public void setIncludeSources(boolean includeSources) {
		doSetBoolean(aQute.lib.osgi.Constants.SOURCES, false, includeSources);
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
		doSetObject(aQute.lib.osgi.Constants.VERSIONPOLICY, oldValue, versionPolicy, string); 
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
	public Collection<ImportPattern> getImportPatterns() {
		List<ImportPattern> result = new LinkedList<ImportPattern>();
		String importsStr = properties.getProperty(Constants.IMPORT_PACKAGE);
		Map<String, Map<String, String>> header = OSGiHeader.parseHeader(importsStr);
		for(Entry<String, Map<String,String>> entry : header.entrySet()) {
			String pattern = entry.getKey();
			boolean optional = false;
			Map<String, String> attribs = entry.getValue();
			if(attribs != null) {
				String resolutionDirective = attribs.remove(aQute.lib.osgi.Constants.RESOLUTION_DIRECTIVE);
				if(Constants.RESOLUTION_OPTIONAL.equals(resolutionDirective)) {
					optional = true;
				}
			}
			result.add(new ImportPattern(pattern, optional, attribs));
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
			doSetObject(Constants.EXPORT_PACKAGE, oldPackages, null, null);
		} else {
			for(Iterator<? extends ExportedPackage> iter = packages.iterator(); iter.hasNext(); ) {
				ExportedPackage pkg = iter.next();
				buffer.append(pkg.getPackageName());
				
				if(pkg.getVersion() != null)
					buffer.append(";version=\"").append(pkg.getVersion()).append("\"");
				
				if(iter.hasNext())
					buffer.append(LIST_SEPARATOR);
			}
			doSetObject(Constants.EXPORT_PACKAGE, oldPackages, packages, buffer.toString());
		}
	}
	public void setImportPatterns(Collection<? extends ImportPattern> patterns) {
		Collection<ImportPattern> oldPatterns = getImportPatterns();
		StringBuilder buffer = new StringBuilder();
		
		if(patterns == null || patterns.isEmpty()) {
			doSetObject(Constants.IMPORT_PACKAGE, oldPatterns, null, null);
		} else {
			for(Iterator<? extends ImportPattern> iter = patterns.iterator(); iter.hasNext(); ) {
				ImportPattern pattern = iter.next();
				buffer.append(pattern.getPattern());
				if(pattern.isOptional()) {
					buffer.append(';').append(aQute.lib.osgi.Constants.RESOLUTION_DIRECTIVE).append('=').append(Constants.RESOLUTION_OPTIONAL);
				}
				for (Entry <String,String> attribEntry : pattern.getAttributes().entrySet()) {
					buffer.append(';').append(attribEntry.getKey()).append('=').append(attribEntry.getValue());
				}
				if(iter.hasNext())
					buffer.append(LIST_SEPARATOR);
			}
			doSetObject(Constants.IMPORT_PACKAGE, oldPatterns, patterns, buffer.toString());
		}
	}
	public void setPrivatePackages(Collection<? extends String> packages) {
		Collection<String> oldPackages = getPrivatePackages();
		
		StringBuilder buffer = new StringBuilder();
		if(packages == null || packages.isEmpty()) {
			doSetObject(aQute.lib.osgi.Constants.PRIVATE_PACKAGE, oldPackages, null, null);
		} else {
			for(Iterator<? extends String> iter = packages.iterator(); iter.hasNext(); ) {
				String pkg = iter.next();
				buffer.append(pkg);
				if(iter.hasNext())
					buffer.append(LIST_SEPARATOR);
			}
			doSetObject(aQute.lib.osgi.Constants.PRIVATE_PACKAGE, oldPackages, packages, buffer.toString());
		}
	}
	
	public Collection<ServiceComponent> getServiceComponents(){
		List<ServiceComponent> result = new ArrayList<ServiceComponent>();
		
		Processor processor = new Processor(properties);
		String scHeaderStr = processor.getProperty(aQute.lib.osgi.Constants.SERVICE_COMPONENT);
		Map<String, Map<String, String>> scHeader = processor.parseHeader(scHeaderStr);
		for (Entry<String, Map<String, String>> entry : scHeader.entrySet()) {
			String scName = entry.getKey();
			Map<String, String> attribsMap = entry.getValue();
			ServiceComponentAttribs scAttribs = ServiceComponentAttribs.loadFrom(attribsMap);
			result.add(new ServiceComponent(scName, scAttribs));
		}
		
		return result;
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

