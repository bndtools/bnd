package aQute.remote.agent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import aQute.lib.io.IO;
import aQute.remote.api.Agent;
import aQute.remote.api.Supervisor;
import aQute.remote.util.Link;

/**
 * The agent bundles uses an activator instead of DS to not constrain the target
 * environment in any way.
 */
public class Activator extends Thread implements BundleActivator {
	private File				cache;
	private ServerSocket		server;
	private BundleContext		context;
	private List<AgentServer>	agents	= new CopyOnWriteArrayList<>();

	@Override
	public void start(final BundleContext context) throws Exception {
		this.context = context;

		//
		// Get the specified port in the framework properties
		//

		String port = context.getProperty(Agent.AGENT_SERVER_PORT_KEY);
		if (port == null)
			port = Agent.DEFAULT_PORT + "";

		//
		// Check if it matches the specifiction of host:port
		//

		Matcher m = Agent.PORT_P.matcher(port);
		if (!m.matches())
			throw new IllegalArgumentException(
				"Invalid port specification in property aQute.agent.server.port, expects [<host>:]<port> : " + port);

		//
		// See if the host was set, otherwise use localhost
		// for security reasons
		//

		String host = m.group(1);
		if (host == null)
			host = "localhost";
		else
			port = m.group(2);

		System.err.println("Host " + host + " " + port);

		//
		// Get the SHA cache root file, which will be shared by all agents for
		// this process.
		//

		cache = context.getDataFile("shacache");

		int p = Integer.parseInt(port);
		server = "*".equals(host) ? new ServerSocket(p) : new ServerSocket(p, 3, InetAddress.getByName(host));
		start();

	}

	/**
	 * Main dispatcher loop
	 */
	@Override
	public void run() {

		try {
			while (!isInterrupted())
				try {
					Socket socket = server.accept();

					//
					// Use a time out so we get interrupts
					// and can do some checks
					//

					socket.setSoTimeout(1000);

					//
					// Create a new agent, and link it up.
					//

					final AgentServer sa = new AgentServer("<>", context, cache);
					agents.add(sa);
					Link<Agent, Supervisor> link = new Link<Agent, Supervisor>(Supervisor.class, sa, socket) {
						@Override
						public void close() throws IOException {
							agents.remove(sa);
							super.close();
						}
					};
					sa.setLink(link);
					link.run();
				} catch (SocketException e) {
					if (!isInterrupted())
						About.log.warning("accepting agent requests " + e);
				} catch (Exception e) {
					About.log.warning("accepting agent requests " + e);
				} catch (Throwable t) {
					t.printStackTrace();
				}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			throw t;
		} finally {
			IO.close(server);
		}
	}

	/**
	 * Shutdown any agents & the server socket.
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		interrupt();
		IO.close(server);

		for (AgentServer sa : agents) {
			IO.close(sa);
		}
	}

}
