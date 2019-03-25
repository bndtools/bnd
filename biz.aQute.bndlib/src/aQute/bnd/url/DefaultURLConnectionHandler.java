package aQute.bnd.url;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.url.URLConnectionHandler;
import aQute.lib.converter.Converter;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import aQute.service.reporter.Reporter;

/**
 * Base class for the URL Connection handlers. This class implements some
 * convenient methods like the matching. In general you should subclass and
 * implement {@link #handle(URLConnection)}. Be aware to call the
 * {@link #matches(URLConnection)} method to verify the plugin is applicable.
 */
public class DefaultURLConnectionHandler implements URLConnectionHandler, Plugin, RegistryPlugin, Reporter {
	private final static Logger logger = LoggerFactory.getLogger(DefaultURLConnectionHandler.class);

	interface Config {
		String match();
	}

	private final Set<Glob>	matchers	= new HashSet<>();
	private Reporter		reporter;
	protected Registry		registry	= null;

	/**
	 * Not doing anything is perfect ok
	 */
	@Override
	public void handle(URLConnection connection) throws Exception {}

	/**
	 * Verify if the URL matches one of our globs. If there are no globs, we
	 * always return true.
	 */
	@Override
	public boolean matches(URL url) {
		if (matchers.isEmpty())
			return true;

		String string = url.toString();
		for (Glob g : matchers) {
			if (g.matcher(string)
				.matches())
				return true;
		}
		return false;
	}

	/**
	 * Convenience method to make it easier to verify connections
	 *
	 * @param connection The connection to match
	 * @return true if this connection should be handled.
	 */
	protected boolean matches(URLConnection connection) {
		return matches(connection.getURL());
	}

	/**
	 * We are a @link {@link RegistryPlugin} for convenience to our subclasses.
	 */
	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	/**
	 * Set the properties for this plugin. Subclasses should call this method
	 * before they handle their own properties.
	 */

	@Override
	public void setProperties(Map<String, String> map) throws Exception {
		Config config = Converter.cnv(Config.class, map);
		for (String p : Processor.split(config.match())) {
			matchers.add(new Glob(p));
		}
	}

	@Override
	public void setReporter(Reporter processor) {
		this.reporter = processor;
	}

	// Delegated reporter methods for convenience
	@Override
	public List<String> getWarnings() {
		return reporter.getWarnings();
	}

	@Override
	public List<String> getErrors() {
		return reporter.getErrors();
	}

	@Override
	public Location getLocation(String msg) {
		return reporter.getLocation(msg);
	}

	@Override
	public boolean isOk() {
		return reporter.isOk();
	}

	@Override
	public SetLocation error(String format, Object... args) {
		return reporter.error(format, args);
	}

	@Override
	public SetLocation warning(String format, Object... args) {
		return reporter.warning(format, args);
	}

	/**
	 * @deprecated Use SLF4J Logger.debug instead.
	 */
	@Override
	@Deprecated
	public void trace(String format, Object... args) {
		if (logger.isDebugEnabled()) {
			logger.debug("{}", Strings.format(format, args));
		}
	}

	/**
	 * @deprecated Use SLF4J Logger.info() instead.
	 */
	@Override
	@Deprecated
	public void progress(float progress, String format, Object... args) {
		if (logger.isInfoEnabled()) {
			String message = Strings.format(format, args);
			if (progress > 0)
				logger.info("[{}] {}", (int) progress, message);
			else
				logger.info("{}", message);
		}
	}

	@Override
	public SetLocation exception(Throwable t, String format, Object... args) {
		return reporter.exception(t, format, args);
	}

	@Override
	public boolean isPedantic() {
		return reporter.isPedantic();
	}

	public DefaultURLConnectionHandler addMatcher(String glob) {
		matchers.add(new Glob(glob));
		return this;
	}
}
