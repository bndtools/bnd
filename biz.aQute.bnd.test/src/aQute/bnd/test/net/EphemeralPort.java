package aQute.bnd.test.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.function.IntSupplier;

public enum EphemeralPort implements IntSupplier {
	AUTOMATIC {
		@Override
		public int getAsInt() {
			while (true) {
				try (ServerSocket serverSocket = new ServerSocket()) {
					serverSocket.setReuseAddress(true);
					serverSocket.bind(new InetSocketAddress(0));
					int port = serverSocket.getLocalPort();
					if (port > 0) {
						return port;
					}
				} catch (IOException e) {
					// try another port
				}
			}
		}
	};
}
