package name.neilbartlett.eclipse.bndtools.editor.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
	
	private static final String LIST_SEPARATOR = ",\\\n\t";
	private static final String ISO_8859_1 = "ISO-8859-1"; //$NON-NLS-1$
	
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
	
	private interface Converter<T> {
		T convert(String string) throws IllegalArgumentException;
	}
	
	private interface Formatter<T> {
		String format(T object);
	}
	
	private final PropertyChangeSupport propChangeSupport = new PropertyChangeSupport(this);
	private final Properties properties = new Properties();;
	private final Map<String, Object> objectProperties = new HashMap<String, Object>();
	private final Map<String, String> changesToSave = new HashMap<String, String>();
	
	public void loadFrom(IDocument document) throws IOException {
		// Clear and load
		properties.clear();
		InputStream stream = new ByteArrayInputStream(document.get().getBytes(ISO_8859_1));
		properties.load(stream);
		objectProperties.clear();
		changesToSave.clear();
		
		// Fire property changes on all known property names
		for (String prop : KNOWN_PROPERTIES) {
			// null values for old and new forced the change to be fired
			propChangeSupport.firePropertyChange(prop, null, null);
		}
	}
	
	public void saveChangesTo(IDocument document) {
		for(Iterator<Entry<String,String>> iter = changesToSave.entrySet().iterator(); iter.hasNext(); ) {
			Entry<String, String> entry = iter.next();
			iter.remove();
			
			String propertyName = entry.getKey();
			String stringValue = entry.getValue();
			
			updateDocument(document, propertyName, stringValue);
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
	private String doGetString(String name) {
		String result;
		if(objectProperties.containsKey(name)) {
			result = (String) objectProperties.get(name);
		} else {
			if(properties.containsKey(name)) {
				result = properties.getProperty(name);
				objectProperties.put(name, result);
			} else {
				result = null;
			}
		}
		return result;
	}
	private <T> T doGetObject(String name, Converter<? extends T> converter) {
		T result;
		if(objectProperties.containsKey(name)) {
			@SuppressWarnings("unchecked")
			T temp = (T) objectProperties.get(name);
			result = temp;
		} else {
			if(properties.containsKey(name)) {
				result = converter.convert(properties.getProperty(name));
				objectProperties.put(name, result);
			} else {
				result = null;
			}
		}
		return result;
	}
	private void doSetObject(String name, Object oldValue, Object newValue, String formattedString) {
		objectProperties.put(name, newValue);
		changesToSave.put(name, formattedString);
		propChangeSupport.firePropertyChange(name, oldValue, newValue);
	}
	
	public String getBundleSymbolicName() {
		return doGetString(Constants.BUNDLE_SYMBOLICNAME);
	}
	
	public void setBundleSymbolicName(String bundleSymbolicName) {
		doSetObject(Constants.BUNDLE_SYMBOLICNAME, getBundleSymbolicName(), bundleSymbolicName, bundleSymbolicName); 
	}
	
	public String getBundleVersionString() {
		return doGetString(Constants.BUNDLE_VERSION); 
	}
	
	public void setBundleVersion(String bundleVersion) {
		doSetObject(Constants.BUNDLE_VERSION, getBundleVersionString(), bundleVersion, bundleVersion);
	}
	
	public String getBundleActivator() {
		return doGetString(Constants.BUNDLE_ACTIVATOR); 
	}
	
	public void setBundleActivator(String bundleActivator) {
		doSetObject(Constants.BUNDLE_ACTIVATOR, getBundleActivator(), bundleActivator, bundleActivator);
	}
	
	public void setIncludeSources(boolean includeSources) {
		boolean oldValue = isIncludeSources();
		String formattedString = includeSources ? Boolean.TRUE.toString() : null;
		doSetObject(aQute.lib.osgi.Constants.SOURCES, oldValue, includeSources, formattedString);
	}
	
	public boolean isIncludeSources() {
		Boolean objValue = doGetObject(aQute.lib.osgi.Constants.SOURCES, new Converter<Boolean>() {
			public Boolean convert(String string) throws IllegalArgumentException {
				return Boolean.valueOf(string);
			}
		});
		return objValue != null ? objValue.booleanValue() : false;
	}
	
	public VersionPolicy getVersionPolicy() throws IllegalArgumentException {
		return doGetObject(aQute.lib.osgi.Constants.VERSIONPOLICY, new Converter<VersionPolicy>() {
			public VersionPolicy convert(String string) throws IllegalArgumentException {
				return VersionPolicy.parse(string);
			}
		});
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
		return doGetObject(Constants.EXPORT_PACKAGE, new Converter<Collection<ExportedPackage>>() {
			public Collection<ExportedPackage> convert(String string) throws IllegalArgumentException {
				List<ExportedPackage> result = new LinkedList<ExportedPackage>();
				Map<String, Map<String, String>> header = OSGiHeader.parseHeader(string, null);
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
		});
	}
	public Collection<ImportPattern> getImportPatterns() {
		return doGetObject(Constants.IMPORT_PACKAGE, new Converter<Collection<ImportPattern>>() {
			public Collection<ImportPattern> convert(String string)
					throws IllegalArgumentException {
				List<ImportPattern> result = new LinkedList<ImportPattern>();
				Map<String, Map<String, String>> header = OSGiHeader.parseHeader(string);
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
		});
	}
	public Collection<String> getPrivatePackages() {
		return doGetObject(aQute.lib.osgi.Constants.PRIVATE_PACKAGE, new Converter<Collection<String>>() {
			public Collection<String> convert(String string) {
				List<String> packages = new LinkedList<String>();
				Map<String, Map<String, String>> header = OSGiHeader.parseHeader(string);
				for(Entry<String, Map<String,String>> entry : header.entrySet()) {
					String packageName = entry.getKey();
					packages.add(packageName);
				}
				return packages;
			}
		});
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
	
	public List<ServiceComponent> getServiceComponents(){
		return doGetObject(aQute.lib.osgi.Constants.SERVICE_COMPONENT, new Converter<List<ServiceComponent>>() {
			public List<ServiceComponent> convert(String string) throws IllegalArgumentException {
				List<ServiceComponent> result = new ArrayList<ServiceComponent>();
				Processor processor = new Processor(properties);
				Map<String, Map<String, String>> scHeader = processor.parseHeader(string);
				for (Entry<String, Map<String, String>> entry : scHeader.entrySet()) {
					String scName = entry.getKey();
					Map<String, String> attribsMap = entry.getValue();
					result.add(new ServiceComponent(scName, attribsMap));
				}
				return result;
			}
		});
	}
	public void setServiceComponents(Collection<? extends ServiceComponent> components) {
		List<ServiceComponent> oldValue = getServiceComponents();
		
		StringBuilder buffer = new StringBuilder();
		if(components == null || components.isEmpty()) {
			doSetObject(aQute.lib.osgi.Constants.SERVICE_COMPONENT, oldValue, null, null);
		} else {
			for(Iterator<? extends ServiceComponent> iter = components.iterator(); iter.hasNext(); ) {
				ServiceComponent component = iter.next();
				component.formatTo(buffer);
				if(iter.hasNext())
					buffer.append(LIST_SEPARATOR);
			}
			doSetObject(aQute.lib.osgi.Constants.SERVICE_COMPONENT, oldValue, components, buffer.toString());
		}
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

