package aQute.remote.agent.provider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;

import aQute.libg.comlink.Link;
import aQute.remote.api.Agent;
import aQute.remote.api.Supervisor;

public class Dispatcher extends Thread {

	private final String host;
	private final int port;
	private final BundleContext context;
	private final List<Closeable> closeables = new CopyOnWriteArrayList<Closeable>();
	private final File cache;
	private ServerSocket server;

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

				while (!isInterrupted()) {
					final Socket connection = server.accept();

					AgentServer ma = new AgentServer(context, cache);
					final Link<Agent, Supervisor> link = new Link<Agent, Supervisor>(
							Supervisor.class, ma, connection);
					ma.setSupervisor(link.getRemote());
					closeables.add(link);
					link.open();
				}

				server.close();
			} catch (SocketException e) {
				// ignore
			}catch (Exception e) {
				e.printStackTrace();
			}
	}

	public void open() throws Exception {
		server = host.equals("*") ? new ServerSocket(port) : new ServerSocket(
				port, 3, InetAddress.getByName(host));
		start();
	}

	public void close() throws IOException {
		server.close();
		this.interrupt();
		for (Closeable link : closeables) {
			try {
				link.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
