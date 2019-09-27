package aQute.lib.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public class LinkTest extends TestCase {
	private ServerSocket	server;
	private LocalImpl		localImpl;
	private RemoteImpl		remoteImpl;
	CountDownLatch			localClosed;
	CountDownLatch			remoteClosed;
	private Socket			localSocket;
	private Socket			remoteSocket;
	ExecutorService			es;

	@Override
	public void setUp() throws IOException {
		es = Executors.newCachedThreadPool();
		localClosed = new CountDownLatch(1);
		remoteClosed = new CountDownLatch(1);
		server = new ServerSocket(4567);
		remoteSocket = new Socket(InetAddress.getByName(null), server.getLocalPort());
		remoteSocket.setSoTimeout(500);
		localSocket = server.accept();
		localSocket.setSoTimeout(500);
		localImpl = new LocalImpl(Remote.class, localSocket.getInputStream(), localSocket.getOutputStream());
		remoteImpl = new RemoteImpl(Local.class, remoteSocket.getInputStream(), remoteSocket.getOutputStream());
	}

	@Override
	public void tearDown() throws IOException {
		server.close();
		localSocket.close();
		remoteSocket.close();
		es.shutdown();
	}

	interface Remote {
		int foo();

		void abort() throws IOException;
	}

	interface Local {
		int bar();

		void aborted() throws IOException;
	}

	public class LocalImpl implements Local, Closeable {
		Link<Local, Remote> link;

		public LocalImpl(Class<Remote> type, InputStream in, OutputStream out) {
			link = new Link<>(type, in, out, es);
			link.open(this);
		}

		@Override
		public int bar() {
			System.out.println("bar");
			return 42;
		}

		@Override
		public void close() throws IOException {
			System.out.println("local closed");
			localClosed.countDown();
		}

		@Override
		public void aborted() throws IOException {
			System.out.println("bar");
			link.close();
		}
	}

	public class RemoteImpl implements Remote, Closeable {
		Link<Remote, Local> link;

		public RemoteImpl(Class<Local> type, InputStream in, OutputStream out) {
			link = new Link<>(type, in, out, es);
			link.open(this);
		}

		@Override
		public int foo() {
			System.out.println("foo");
			return -42;
		}

		@Override
		public void close() throws IOException {
			System.out.println("remote closed");
			remoteClosed.countDown();
		}

		@Override
		public void abort() throws IOException {
			link.getRemote()
				.aborted();
		}
	}

	/**
	 * Test transfer
	 *
	 * @throws Exception
	 */

	public void testTransfer() throws Exception {

		assertEquals(-42, localImpl.link.getRemote()
			.foo());
		localImpl.link.transfer(null);

		LocalImpl newer = new LocalImpl(Remote.class, localImpl.link.getInput(), localImpl.link.getOutput());

		assertEquals(-42, newer.link.getRemote()
			.foo());
		newer.link.close();

		assertThat(remoteClosed.await(1000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(localClosed.await(1000, TimeUnit.MILLISECONDS)).isTrue();

		newer.close();
	}

	/**
	 * Test simple
	 *
	 * @throws InterruptedException
	 */

	public void testSimple() throws IOException, InterruptedException {
		assertEquals(-42, localImpl.link.getRemote()
			.foo());

		assertTrue(localImpl.link.isOpen());
		assertTrue(remoteImpl.link.isOpen());
		localSocket.getInputStream()
			.close();

		normalClose();
	}

	private void normalClose() throws InterruptedException {
		assertThat(remoteClosed.await(1000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(localClosed.await(1000, TimeUnit.MILLISECONDS)).isTrue();
		assertFalse(localImpl.link.isOpen());
		assertFalse(remoteImpl.link.isOpen());
	}

	/**
	 * Only close in
	 *
	 * @throws InterruptedException
	 */

	public void testCloseLocalIn() throws IOException, InterruptedException {

		assertTrue(localImpl.link.isOpen());
		assertTrue(remoteImpl.link.isOpen());
		assertEquals(-42, localImpl.link.getRemote()
			.foo());
		localSocket.getOutputStream()
			.close();
		normalClose();
	}

	public void testCloseremoteIn() throws IOException, InterruptedException {

		assertTrue(localImpl.link.isOpen());
		assertTrue(remoteImpl.link.isOpen());
		assertEquals(-42, localImpl.link.getRemote()
			.foo());
		remoteSocket.getInputStream()
			.close();
		normalClose();
	}

	public void testCloseremoteOut() throws IOException, InterruptedException {

		assertTrue(localImpl.link.isOpen());
		assertTrue(remoteImpl.link.isOpen());
		assertEquals(-42, localImpl.link.getRemote()
			.foo());
		remoteSocket.getOutputStream()
			.close();

		normalClose();
	}

	public void testCloseLocalSocket() throws IOException, InterruptedException {

		assertTrue(localImpl.link.isOpen());
		assertTrue(remoteImpl.link.isOpen());
		assertEquals(-42, localImpl.link.getRemote()
			.foo());
		localSocket.close();

		normalClose();
	}

	public void testCloseRemoteSocket() throws IOException, InterruptedException {

		assertTrue(localImpl.link.isOpen());
		assertTrue(remoteImpl.link.isOpen());
		assertEquals(-42, localImpl.link.getRemote()
			.foo());
		remoteSocket.close();

		normalClose();
	}

	public void testAbort() throws IOException, InterruptedException {

		assertEquals(-42, localImpl.link.getRemote()
			.foo());

		localImpl.link.getRemote()
			.abort();
		normalClose();
	}
}
