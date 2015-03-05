package aQute.remote.agent.provider;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import aQute.libg.comlink.Link;

public class Dispatcher<L,R> extends Thread {

	private final String host;
	private final int port;
	private final Callable<Linkable<L, R>> factory;
	private final List<Closeable> closeables = new CopyOnWriteArrayList<Closeable>();
	private ServerSocket server;
	private Class<R> remoteClass;

	public Dispatcher(Class<R> remoteClass, Callable<Linkable<L,R>> factory, String host, int port) {
		super("aQute.agent.server::" + (host == null ? "localhost" : host)
				+ ":" + port);
		this.remoteClass=remoteClass;
		this.factory = factory;
		this.host = host;
		this.port = port;
	}


	public void run() {
		while (!isInterrupted())
			try {

				while (!isInterrupted()) {
					final Socket connection = server.accept();

					
					Linkable<L, R> local = factory.call();
					final Link<L, R> link = new Link<L, R>(
							remoteClass, local.get(), connection);
					local.setRemote(link.getRemote());
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


	public int getPort() {
		return server.getLocalPort();
	}
	
}
