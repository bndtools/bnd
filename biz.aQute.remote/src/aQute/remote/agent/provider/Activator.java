package aQute.remote.agent.provider;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import aQute.remote.api.Agent;
import aQute.remote.api.Supervisor;

public class Activator implements BundleActivator,
		Callable<Linkable<Agent, Supervisor>> {
	static Pattern PORT_P = Pattern.compile("(?:([^:]+):)?(\\d+)");
	private Dispatcher<Agent, Supervisor> dispatcher;
	private File cache;
	private BundleContext context;

	@Override
	public void start(final BundleContext context) throws Exception {
		this.context = context;
		String port = context.getProperty("aQute.agent.server.port");

		if (port == null)
			port = Agent.DEFAULT_PORT + "";

		Matcher m = PORT_P.matcher(port);
		if (!m.matches())
			throw new IllegalArgumentException(
					"Invalid port specification in property aQute.agent.server.port, expects [<host>:]<port> : "
							+ port);

		String host = m.group(1);
		if (host == null)
			host = "localhost";

		cache = context.getDataFile("shacache");
		dispatcher = new Dispatcher<Agent, Supervisor>(Supervisor.class, this,
				host, Integer.parseInt(m.group(2)));
		dispatcher.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		dispatcher.close();
	}

	@Override
	public Linkable<Agent, Supervisor> call() throws Exception {
		return new AgentServer(context, cache);
	}

}
