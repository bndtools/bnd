package aQute.remote.agent;

import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import aQute.remote.api.Agent;
import aQute.remote.api.Supervisor;
import aQute.remote.util.Link;

public class Activator extends Thread implements BundleActivator {
	static Pattern PORT_P = Pattern.compile("(?:([^:]+):)?(\\d+)");
	private File cache;
	private ServerSocket server;
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
		
		int p = Integer.parseInt(port);
		server = "*".equals(host) ? new ServerSocket(p) : new ServerSocket(p, 3, InetAddress.getByName(host));
		start();
		
	}

	public void run() {
		while ( !isInterrupted()) try {
			Socket socket = server.accept();
			socket.setSoTimeout(1000);
			AgentServer sa = new AgentServer("<>",context,cache);
			Link<Agent, Supervisor> link = new Link<Agent, Supervisor>(Supervisor.class, sa, socket);
			sa.setLink(link);
			link.run();
		} catch( Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void stop(BundleContext context) throws Exception {
		interrupt();
		server.close();
	}

}
