package aQute.lib.env;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import aQute.lib.io.*;
import aQute.lib.properties.*;
import aQute.libg.reporter.*;
import aQute.libg.sed.*;

public class Env extends ReporterAdapter implements Replacer, Domain {
	final Properties		properties;
	final ReplacerAdapter	replacer	= new ReplacerAdapter(this);
	final Env				parent;
	File					base;
	boolean					prepared;

	public Env() {
		this(new Properties(), null, null);
	}

	public Env(Properties properties, Env parent, File base) {
		this.properties = properties;
		this.parent = parent;
		this.base = base;
		if (parent != null) {
			setTrace(parent.isTrace());
			setExceptions(parent.isExceptions());
			setPedantic(parent.isPedantic());
		}
	}

	public Env(Env env) {
		this(new Properties(env.properties), env, null);
	}

	public String process(String line) {
		return replacer.process(line);
	}

	@SuppressWarnings("unchecked")
	public Map<String,String> getMap() {
		@SuppressWarnings("rawtypes")
		Map<String,String> map = (Map) properties;
		return map;
	}

	public Domain getParent() {
		return parent;
	}

	public String getProperty(String key) {
		return getProperty(key, null);
	}

	public String getProperty(String key, String deflt) {
		String value = properties.getProperty(key);
		if (value == null)
			value = deflt;
		if (value == null)
			return null;
		return process(value);
	}

	public void setProperty(String key, String value) {
		properties.put(key, value);
	}

	public void addProperty(String key, String value) {
		String old = properties.getProperty(key);
		if (old == null)
			old = value;
		else
			old = old + "," + value;

		properties.put(key, value);
	}

	public void removeProperty(String key) {
		properties.remove(key);
	}

	public void putAll(Map<String,String> map) {
		properties.putAll(map);
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	public void putAll(Properties map) {
		putAll((Map) map);
	}

	public void addAll(Map<String,String> map) {
		for (Entry<String,String> entry : map.entrySet()) {
			addProperty(entry.getKey(), entry.getValue());
		}
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	public void addAll(Properties map) {
		addAll((Map) map);
	}

	public void setProperties(File file) throws Exception {

		if (!file.isFile())
			error("No such file %s", file);
		else {
			setProperties(file.toURI());
		}
	}
	public void addProperties(File file, Pattern matching) throws Exception {

		if (!file.isFile())
			error("No such file %s", file);
		else {
			if ( file.isFile())
				setProperties(file.toURI());
			else {
				for ( File sub : file.listFiles()) {
					if ( matching.matcher(sub.getName()).matches()) {
						addProperties(file, matching);
					}
				}
			}
		}
	}

	public void setProperties(URI uri) throws Exception {
		Properties props = PropertiesParser.parse(uri);
		String errors = (String) props.remove(PropertiesParser.$$$ERRORS);
		if (errors != null) {
			for (String error : errors.split("\n")) {
				error("%s: %s", uri.toString(), error);
			}
		}
		putAll(props);
	}

	public Header getHeader(String header) {
		return new Header(getProperty(header));
	}

	public Header getHeader(String header, String deflt) {
		return new Header(getProperty(header, deflt));
	}

	public File getBase() {
		if (base == null)
			if (parent != null)
				return parent.getBase();
			else
				return IO.work;
		else
			return base;
	}

	public void setBase(File file) {
		this.base = file;
	}

	public File getFile(String file) {
		return IO.getFile(getBase(), file);
	}

	public void addTarget(Object domain) {
		replacer.addTarget(domain);
	}

	public void removeTarget(Object domain) {
		replacer.removeTarget(domain);
	}

	protected boolean prepare() throws Exception {
		boolean old = prepared;
		prepared = true;
		return old;
	}

	protected boolean isPrepared() {
		return prepared;
	}

	protected boolean clear() {
		boolean old = prepared;
		prepared = false;
		return old;
	}

	protected Properties getProperties() {
		return properties;
	}
	/**
	 * Return a file relative to the base.
	 */


	public File getFile(String file, String notfound) {
		File f = IO.getFile(getBase(), file);
		if (!f.isFile() && notfound != null) {
			error(notfound, f.getAbsolutePath());
			f = null;
		}
		return f;
	}
	public File getDir(String file, String notfound) {
		File f = IO.getFile(base, file);
		if (!f.isDirectory() && notfound != null) {
			error(notfound, f.getAbsolutePath());
			f = null;
		}
		return f;
	}
}
