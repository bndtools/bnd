package aQute.agent.server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;

import aQute.libg.comlink.Link;
import aQute.service.agent.Agent;
import aQute.service.agent.Supervisor;

public class Dispatcher extends Thread {

	private final String host;
	private final int port;
	private final BundleContext context;
	private final List<Link<Agent, Supervisor>> links = new CopyOnWriteArrayList<Link<Agent, Supervisor>>();
	private final File cache;

	public Dispatcher(File cache, BundleContext context, String host, int port) {
		super("aQute.agent.server::" + (host == null ? "localhost" : host)
				+ ":" + port);
		this.cache = cache;
		this.context = context;
		this.host = host;
		this.port = port;
	}

	public void run() {
		while (!isInterrupted())
			try {

				ServerSocket server = host.equals("*") ? new ServerSocket(port)
						: new ServerSocket(port, 3, InetAddress.getByName(host));

				while (!isInterrupted()) {
					final Socket connection = server.accept();

					AgentServer ma = new AgentServer(
							context, cache);
					final Link<Agent, Supervisor> link = new Link<Agent, Supervisor>(
							Supervisor.class, ma, connection);
					ma.setSupervisor(link.getRemote());
					links.add(link);
					Thread agent = new Thread(link, "aQute.agent.connection::"
							+ connection.getInetAddress()) {
						public void run() {
							try {
								link.run();
							} finally {
								links.remove(link);
								try {
									link.close();
								} catch (IOException e) {
									// ok for now
								}
							}
						}
					};
					agent.setDaemon(true);
					agent.start();
				}

				server.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	public void open() {
		start();
	}

	public void close() {
		this.interrupt();
		for (Link<Agent, Supervisor> link : links) {
			try {
				link.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
