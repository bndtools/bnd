package aQute.remote.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketRedirector implements Redirector {
	private Socket socket;
	private PrintStream in;
	private Thread out;
	private boolean quit;

	public SocketRedirector(final AgentServer agentServer, int port)
			throws UnknownHostException, IOException {
		this.socket = new Socket(InetAddress.getLocalHost(), port);
		this.in = new PrintStream(socket.getOutputStream());
		this.out = new Thread() {
			@Override
			public void run() {
				try {
					InputStream out = socket.getInputStream();
					InputStreamReader ir = new InputStreamReader(out);
					BufferedReader br = new BufferedReader(ir);

					while (!isInterrupted() && !quit)
						try {
							String line = br.readLine();
							if (line == null)
								break;
							agentServer.getSupervisor().stdout(line);
						} catch (Exception e) {
							break;
						}
				} catch (IOException e1) {
					// ignore, we just exit
				}
			}
		};
		this.out.start();
	}

	@Override
	public void close() throws IOException {
		quit = true;
		out.interrupt();
		try {
			out.join(500);
		} catch (InterruptedException e) {
			// ignore, best effort
		}
		socket.close();
	}

	@Override
	public int getPort() {
		return socket.getPort();
	}

	@Override
	public void stdin(String s) throws Exception {
		this.in.print(s);
		this.in.flush();
	}

	@Override
	public PrintStream getOut() throws Exception {
		return this.in;
	}

}
