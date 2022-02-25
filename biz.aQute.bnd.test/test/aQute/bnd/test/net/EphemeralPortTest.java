package aQute.bnd.test.net;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import java.util.function.IntSupplier;

import org.junit.jupiter.api.Test;

class EphemeralPortTest {

	@Test
	void automatic_supplier() throws Exception {
		IntSupplier ports = EphemeralPort.AUTOMATIC;
		for (int i = 0; i < 100; i++) {
			int port = ports.getAsInt();
			try (ServerSocket serverSocket = new ServerSocket(port)) {
				assertThat(serverSocket.getLocalPort()).isEqualTo(port);
			}
		}
	}

}
