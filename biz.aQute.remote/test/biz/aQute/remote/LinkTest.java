package biz.aQute.remote;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import aQute.remote.util.Link;
import junit.framework.TestCase;

public class LinkTest extends TestCase {
	private ServerSocket	server;
	private LocalImpl		localImpl;
	private RemoteImpl		remoteImpl;
	AtomicInteger			localClosed		= new AtomicInteger();
	AtomicInteger			remoteClosed	= new AtomicInteger();
	private Socket			localSocket;
	private Socket			remoteSocket;

	@Override
	public void setUp() throws IOException {
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
			link = new Link<>(type, this, in, out);
		}

		@Override
		public int bar() {
			System.out.println("bar");
			return 42;
		}

		@Override
		public void close() throws IOException {
			System.out.println("local closed");
			localClosed.incrementAndGet();
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
			link = new Link<>(type, this, in, out);
		}

		@Override
		public int foo() {
			System.out.println("foo");
			return -42;
		}

		@Override
		public void close() throws IOException {
			System.out.println("remote closed");
			remoteClosed.incrementAndGet();
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
		localImpl.link.open();
		remoteImpl.link.open();

		assertEquals(-42, localImpl.link.getRemote()
			.foo());
		localImpl.link.transfer(null);

		LocalImpl newer = new LocalImpl(Remote.class, localImpl.link.getInput(), localImpl.link.getOutput());
		newer.link.open();
		assertEquals(-42, newer.link.getRemote()
			.foo());
		newer.link.close();

		Thread.sleep(100);
		assertEquals(1, remoteClosed.get());
		assertEquals(1, localClosed.get());

		newer.close();
	}

	/**
	 * Test simple
	 *
	 * @throws InterruptedException
	 */

	public void testSimple() throws IOException, InterruptedException {
		localImpl.link.open();
		remoteImpl.link.open();

		assertEquals(-42, localImpl.link.getRemote()
			.foo());

		assertTrue(localImpl.link.isOpen());
		assertTrue(remoteImpl.link.isOpen());
		localSocket.getInputStream()
			.close();

		normalClose();
	}

	private void normalClose() throws InterruptedException {
		Thread.sleep(100);

		assertFalse(localImpl.link.isOpen());
		assertFalse(remoteImpl.link.isOpen());
		assertEquals(1, localClosed.get());
		assertEquals(1, remoteClosed.get());
	}

	/**
	 * Only close in
	 *
	 * @throws InterruptedException
	 */

	public void testCloseLocalIn() throws IOException, InterruptedException {
		localImpl.link.open();
		remoteImpl.link.open();

		assertTrue(localImpl.link.isOpen());
		assertTrue(remoteImpl.link.isOpen());
		assertEquals(-42, localImpl.link.getRemote()
			.foo());
		localSocket.getOutputStream()
			.close();
		normalClose();
	}

	public void testCloseremoteIn() throws IOException, InterruptedException {
		localImpl.link.open();
		remoteImpl.link.open();

		assertTrue(localImpl.link.isOpen());
		assertTrue(remoteImpl.link.isOpen());
		assertEquals(-42, localImpl.link.getRemote()
			.foo());
		remoteSocket.getInputStream()
			.close();
		normalClose();
	}

	public void testCloseremoteOut() throws IOException, InterruptedException {
		localImpl.link.open();
		remoteImpl.link.open();

		assertTrue(localImpl.link.isOpen());
		assertTrue(remoteImpl.link.isOpen());
		assertEquals(-42, localImpl.link.getRemote()
			.foo());
		remoteSocket.getOutputStream()
			.close();

		normalClose();
	}

	public void testCloseLocalSocket() throws IOException, InterruptedException {
		localImpl.link.open();
		remoteImpl.link.open();

		assertTrue(localImpl.link.isOpen());
		assertTrue(remoteImpl.link.isOpen());
		assertEquals(-42, localImpl.link.getRemote()
			.foo());
		localSocket.close();

		normalClose();
	}

	public void testCloseRemoteSocket() throws IOException, InterruptedException {
		localImpl.link.open();
		remoteImpl.link.open();

		assertTrue(localImpl.link.isOpen());
		assertTrue(remoteImpl.link.isOpen());
		assertEquals(-42, localImpl.link.getRemote()
			.foo());
		remoteSocket.close();

		normalClose();
	}

	public void testAbort() throws IOException, InterruptedException {
		localImpl.link.open();
		remoteImpl.link.open();
		assertEquals(-42, localImpl.link.getRemote()
			.foo());

		localImpl.link.getRemote()
			.abort();
		normalClose();
	}
}
