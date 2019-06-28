package aQute.lib.env;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;

import aQute.lib.converter.Converter;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.reporter.ReporterAdapter;
import aQute.libg.sed.Domain;
import aQute.libg.sed.Replacer;
import aQute.libg.sed.ReplacerAdapter;

@SuppressWarnings("deprecation")
public class Env extends ReporterAdapter implements Replacer, Domain {
	final Properties		properties;
	final ReplacerAdapter	replacer	= new ReplacerAdapter(this);
	final Env				parent;
	File					base;
	boolean					prepared;

	public Env() {
		this(new UTF8Properties(), null, null);
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
		this(new UTF8Properties(env.properties), env, null);
	}

	@Override
	public String process(String line) {
		return replacer.process(line);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, String> getMap() {
		@SuppressWarnings("rawtypes")
		Map<String, String> map = (Map) properties;
		return map;
	}

	@Override
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

	public void putAll(Map<String, String> map) {
		properties.putAll(map);
	}

	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	public void putAll(Properties map) {
		putAll((Map) map);
	}

	public void addAll(Map<String, String> map) {
		for (Entry<String, String> entry : map.entrySet()) {
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
			UTF8Properties props = new UTF8Properties();
			props.load(file, this);
			putAll(props);
		}
	}

	public void addProperties(File file, Pattern matching) throws Exception {
		if (!file.isDirectory())
			setProperties(file);
		else {
			for (File sub : file.listFiles()) {
				if (matching == null || matching.matcher(sub.getName())
					.matches()) {
					addProperties(file, matching);
				}
			}
		}
	}

	public void setProperties(URI uri) throws Exception {
		UTF8Properties props = new UTF8Properties();
		try (InputStream in = uri.toURL()
			.openStream()) {
			props.load(in, null, this);
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

	/**
	 * This method returns an interface that can be used to get and set the
	 * properties in a type safe way (as well as describing any semantics of
	 * these properties).
	 * <p/>
	 * The interface must have get and/or set methods. The name is mangled to
	 * change _ to . and to remove $ (which is used to mask keywords like new).
	 * If _ and $ are in there twice, one remains. The set methods return the
	 * proxy object itself so you can use it in a builder style.
	 * <p/>
	 * The values are always stored as strings (and can use macros). The result
	 * is converted to the desired type. Arguments in the set methods are always
	 * converted to strings using the toString methods.
	 * <p/>
	 * Example:
	 *
	 * <pre>
	 *  interface MyConfig { int level(); MyConfig level(int level);
	 * Pattern pattern(); MyConfig pattern(String p); } Env env = ... MyConfig c
	 * = env.config(MyConfig.class, "myconfig.");
	 * </pre>
	 *
	 * @param front the fronting interface
	 * @param prefix the prefix in the properties
	 * @return an interface that can be used to get and set properties
	 */
	@SuppressWarnings("unchecked")
	public <T> T config(Class<?> front, final String prefix) {
		return (T) Proxy.newProxyInstance(front.getClassLoader(), new Class[] {
			front
		}, (target, method, parameters) -> {
			String name = mangleMethodName(prefix, method.getName());
			if (parameters == null || parameters.length == 0) {
				String value = getProperty(name);
				if (value == null) {
					if (method.getReturnType()
						.isPrimitive())
						return Converter.cnv(method.getReturnType(), null);
					else
						return null;
				}
				if (method.getReturnType()
					.isInstance(value))
					return value;

				return Converter.cnv(method.getGenericReturnType(), value);
			} else if (parameters.length == 1) {
				Object arg = parameters[0].toString();
				if (arg == null)
					removeProperty(name);
				else
					setProperty(name, arg.toString());
				if (method.getReturnType()
					.isInstance(this))
					return this;
				return Converter.cnv(method.getReturnType(), null);
			}
			throw new IllegalArgumentException("Too many arguments: " + Arrays.toString(parameters));
		});
	}

	public <T> T config(Class<?> front) {
		return config(front, null);
	}

	String mangleMethodName(String prefix, String string) {
		StringBuilder sb = new StringBuilder();
		if (prefix != null) {
			sb.append(prefix);
		}
		sb.append(string);
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);
			boolean twice = i < sb.length() - 1 && sb.charAt(i + 1) == c;
			if (c == '$' || c == '_') {
				if (twice)
					sb.deleteCharAt(i + 1);
				else if (c == '$')
					sb.deleteCharAt(i--); // Remove dollars
				else
					sb.setCharAt(i, '.'); // Make _ into .
			}
		}
		return sb.toString();
	}

	public boolean isTrue(String v) {
		return v != null && v.length() > 0 && !v.equalsIgnoreCase("false");
	}
}
