package aQute.agent.server;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	static Pattern PORT_P = Pattern.compile("([^:]+:)(\\d+)");
	private static final String DEFAULT_PORT = "29999";
	private Dispatcher dispatcher;
	private File cache;

	@Override
	public void start(BundleContext context) throws Exception {
		String port = context.getProperty("aQute.agent.server.port");

		if (port == null)
			port = DEFAULT_PORT;

		Matcher m = PORT_P.matcher(port);
		if (!m.matches())
			throw new IllegalArgumentException(
					"Invalid port specification in property aQute.agent.server.port, expects [<host>:]<port> : "
							+ port);

		String host = m.group(1);
		if (host == null)
			host = "localhost";

		cache = context.getDataFile("shacache");
		dispatcher = new Dispatcher(this.cache, context, host,
				Integer.parseInt(m.group(2)));
		dispatcher.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		dispatcher.close();
	}

}
