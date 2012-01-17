package aQute.lib.osgi;

import static aQute.lib.osgi.Constants.*;

import java.util.*;
import java.util.jar.*;

import aQute.libg.header.*;
import aQute.libg.reporter.*;

/**
 * This class abstracts domains that have properties holding OSGi meta data. It
 * provides access to the keys, the set method and the get method. It then provides
 * convenient methods to access these properties via semantic methods.
 * 
 */
public abstract class Domain implements Iterable<String>{

	public abstract String get(String key);
	public  String get(String key,String deflt) {
		String result = get(key);
		if ( result != null)
			return result;
		return deflt;
	}

	public abstract void set(String key, String value);
	public abstract Iterator<String> iterator();

	public static Domain domain(final Manifest manifest) {
		Attributes attrs = manifest.getMainAttributes();
		return domain(attrs);
	}
	
	public static Domain domain(final Attributes attrs) {
		return new Domain() {

			@Override public String get(String key) {
				return attrs.getValue(key);
			}

			@Override public void set(String key, String value) {
				attrs.putValue(key, value);
			}

			@Override public Iterator<String> iterator() {
				final Iterator<Object> it = attrs.keySet().iterator();
				
				return new Iterator<String>() {

					public boolean hasNext() {
						return it.hasNext();
					}

					public String next() {
						return it.next().toString();
					}

					public void remove() {
						it.remove();
					}
				};
			}
		};
	}

	
	public static Domain domain(final Processor processor ) {
		return new Domain() {

			@Override public String get(String key) {
				return processor.getProperty(key);
			}

			@Override public String get(String key, String deflt) {
				return processor.getProperty(key, deflt);
			}

			@Override public void set(String key, String value) {
				processor.setProperty(key, value);
			}

			@Override public Iterator<String> iterator() {
				final Iterator<String> it =processor.getPropertyKeys(true).iterator();
				
				return new Iterator<String>() {
					String current;
					
					public boolean hasNext() {
						return it.hasNext();
					}

					public String next() {
						return current=it.next().toString();
					}

					public void remove() {
						processor.getProperties().remove(current);
					}
				};
			}
		};		
	}
	
	public static Domain domain(final Map<String,String> map ) {
		return new Domain() {

			@Override public String get(String key) {
				return map.get(key);
			}

			@Override public void set(String key, String value) {
				map.put(key, value);
			}

			@Override public Iterator<String> iterator() {
				return map.keySet().iterator();
			}
		};		
	}
	
	public Parameters getParameters(String key, Reporter reporter) {
		return new Parameters( get(key), reporter);
	}
	
	public Parameters getParameters(String key) {
		return new Parameters( get(key));
	}
	public Parameters getParameters(String key, String deflt) {
		return new Parameters( get(key,deflt));
	}
	public Parameters getParameters(String key, String deflt, Reporter reporter) {
		return new Parameters( get(key,deflt), reporter);
	}
	
	public Parameters getImportPackage() {
		return getParameters(IMPORT_PACKAGE);
	}
	public Parameters getExportPackage() {
		return getParameters(EXPORT_PACKAGE);
	}
	public Parameters getBundleClassPath() {
		return getParameters(BUNDLE_CLASSPATH);
	}

	public Parameters getPrivatePackage() {
		return getParameters(PRIVATE_PACKAGE);
	}
	public Parameters getDynamicImportPackage() {
		return getParameters(DYNAMICIMPORT_PACKAGE);
	}

	public Parameters getExportContents() {
		return getParameters(EXPORT_CONTENTS);
	}

	public String getBundleActivator() {
		return get(BUNDLE_ACTIVATOR);
	}

}
