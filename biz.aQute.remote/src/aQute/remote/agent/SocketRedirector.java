package aQute.remote.agent;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketRedirector implements Redirector {
	private static final String OSGI_SHELL_TELNET_IP = "osgi.shell.telnet.ip";
	private Socket socket;
	private PrintStream in;
	private Thread out;
	private boolean quit;

	public SocketRedirector(final AgentServer agentServer, final int port)
			throws Exception {

		this.out = new Thread() {
			@Override
			public void run() {
				try {

					while (!isInterrupted() && !quit) {
						socket = findSocket(agentServer, port);
						if (socket != null)
							break;

						Thread.sleep(1000);
					}

					SocketRedirector.this.in = new PrintStream(
							socket.getOutputStream());
					InputStream out = socket.getInputStream();

					byte[] buffer = new byte[1000];
					while (!isInterrupted() && !quit)
						try {
							int size = out.read(buffer);
							agentServer.getSupervisor().stdout( new String(buffer,0, size));
						} catch (Exception e) {
							break;
						}
				} catch (Exception e1) {
					// ignore, we just exit
				} finally {
					try {
						if ( socket != null && !quit)
							socket.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
		};
		this.out.setDaemon(true);
		this.out.start();
	}

	private Socket findSocket(AgentServer agent, int port)
			throws UnknownHostException {
		try {
			String ip = agent.getContext().getProperty(OSGI_SHELL_TELNET_IP);
			if (ip != null) {
				InetAddress gogoHost = InetAddress.getByName(ip);
				return new Socket(gogoHost, port);
			}
		} catch (Exception e) {
			// ignore
		}

		try {

			//
			// Some Unix's use 127.0.1.1 for some unknown reason
			// but the Gogo shell delivers at 127.0.0.1
			//

			InetAddress oldStyle = InetAddress.getByName("127.0.0.1");
			return new Socket(oldStyle, port);
		} catch (Exception e) {
			// ignore
		}

		//
		// Ah well, maybe they did change their mind, so let's
		// look at localhost as well.
		//
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			return new Socket(localhost, port);
		} catch (Exception e) {
			// ignore
		}

		return null;
	}

	@Override
	public void close() throws IOException {
		quit = true;
		out.interrupt();
		socket.close();
		try {
			out.join(500);
		} catch (InterruptedException e) {
			// ignore, best effort
		}
	}

	@Override
	public int getPort() {
		return socket.getPort();
	}

	@Override
	public void stdin(String s) throws Exception {
		if (this.in != null) {
			this.in.print(s);
			this.in.flush();
		}
	}

	@Override
	public PrintStream getOut() throws Exception {
		if (this.in != null)
			return this.in;

		return System.out;
	}

}
