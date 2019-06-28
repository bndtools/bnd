package aQute.remote.agent;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * A redirector that will open a connection to a socket, assuming there is a
 * shell running there.
 */
public class SocketRedirector implements Redirector {

	//
	// The key used by the Apache Felix shell to define the host the server
	// socket is registered on
	//

	private static final String	OSGI_SHELL_TELNET_IP	= "osgi.shell.telnet.ip";

	//
	// Telnet sends some options in the beginning that need to be
	// skipped. The IAC is the first byte that then is followed
	// by some commands.
	//

	private static final int	IAC						= 255;
	private Socket				socket;
	private PrintStream			in;
	private Thread				out;
	private boolean				quit;

	/**
	 * Constructor
	 *
	 * @param agentServer the agent we're working for
	 * @param port the shell port
	 */
	public SocketRedirector(final AgentServer agentServer, final int port) throws Exception {

		//
		// We need a thread to read any output from the shell processor
		// which is then forwarded to the supervisor
		//

		this.out = new Thread() {
			@Override
			public void run() {
				try {

					//
					// Connect to the server. We keep on trying this.
					//

					while (!isInterrupted() && !quit) {
						socket = findSocket(agentServer, port);
						if (socket != null)
							break;

						Thread.sleep(1000);
					}

					//
					// Create a printstream on the socket output (the system's
					// input).
					//

					SocketRedirector.this.in = new PrintStream(socket.getOutputStream());

					//
					// Start reading the input
					//
					InputStream out = socket.getInputStream();

					byte[] buffer = new byte[1000];
					while (!isInterrupted() && !quit)
						try {
							int size = out.read(buffer);
							StringBuilder sb = new StringBuilder();
							for (int i = 0; i < size; i++) {
								int b = 0xFF & buffer[i];

								// Since we have a telnet protocol, there
								// are some special characters we should
								// ignore. We assume that all parts of the
								// command are in the same buffer, which is
								// highly likely since they are usually
								// tramsmitted in a single go

								if (b == IAC) {

									// Next is the command type

									b = buffer[++i];

									// DO, DONT, WILL, WONT have an extra byte
									if (b == 251 || b == 252 || b == 253 || b == 254)
										++i;
								} else {
									sb.append((char) b);
								}
							}
							if (sb.length() > 0)
								agentServer.getSupervisor()
									.stdout(sb.toString());
						} catch (Exception e) {
							break;
						}
				} catch (Exception e1) {
					// ignore, we just exit
				} finally {
					try {
						if (socket != null && !quit)
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

	Socket findSocket(AgentServer agent, int port) throws UnknownHostException {
		try {
			String ip = agent.getContext()
				.getProperty(OSGI_SHELL_TELNET_IP);
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

			InetAddress oldStyle = InetAddress.getByName(null);
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
