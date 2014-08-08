package aQute.bnd.url;

import java.net.*;
import java.util.*;

import aQute.bnd.service.*;
import aQute.bnd.service.url.*;
import aQute.libg.glob.*;
import aQute.service.reporter.*;

/**
 * Base class for the URL Connection handlers. This class implements some
 * convenient methods like the matching. In general you should subclass and
 * implement {@link #handle(URLConnection)}. Be aware to call the
 * {@link #matches(URLConnection)} method to verify the plugin is applicable.
 */
public class DefaultURLConnectionHandler implements URLConnectionHandler, Plugin, RegistryPlugin, Reporter {
	
	
	interface Config {
		String match();
	}
	private final Set<Glob>	matchers	= new HashSet<Glob>();
	private Reporter		reporter;
	protected Registry		registry	= null;

	/**
	 * Not doing anything is perfect ok
	 */
	public void handle(URLConnection connection) throws Exception {}

	/**
	 * Verify if the URL matches one of our globs. If there are no globs, we
	 * always return true.
	 */
	public boolean matches(URL url) {
		if (matchers.isEmpty())
			return true;

		String string = url.toString();
		for (Glob g : matchers) {
			if (g.matcher(string).matches())
				return true;
		}
		return true;
	}

	/**
	 * Convenience method to make it easier to verify connections
	 * 
	 * @param connection
	 *            The connection to match
	 * @return true if this connection should be handled.
	 */
	protected boolean matches(URLConnection connection) {
		return matches(connection.getURL());
	}

	/**
	 * We are a @link {@link RegistryPlugin} for convenience to our subclasses.
	 */
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}
	
	/**
	 * Set the properties for this plugin. Subclasses should call this method
	 * before they handle their own properties.
	 */

	public void setProperties(Map<String,String> map) throws Exception {
		String matches = map.get(MATCH);
		if (matches != null) {
			for (String p : matches.split("\\s*,\\s*")) {
				matchers.add(new Glob(p));
			}
		}
	}

	public void setReporter(Reporter processor) {
		this.reporter = processor;
	}

	// Delegated reporter methods for convenience
	public List<String> getWarnings() {
		return reporter.getWarnings();
	}

	public List<String> getErrors() {
		return reporter.getErrors();
	}

	public Location getLocation(String msg) {
		return reporter.getLocation(msg);
	}

	public boolean isOk() {
		return reporter.isOk();
	}

	public SetLocation error(String format, Object... args) {
		return reporter.error(format, args);
	}

	public SetLocation warning(String format, Object... args) {
		return reporter.warning(format, args);
	}

	public void trace(String format, Object... args) {
		reporter.trace(format, args);
	}

	public void progress(float progress, String format, Object... args) {
		reporter.progress(progress, format, args);
	}

	public SetLocation exception(Throwable t, String format, Object... args) {
		return reporter.exception(t, format, args);
	}

	public boolean isPedantic() {
		return reporter.isPedantic();
	}
}
