package aQute.lib.link;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;

/**
 * This is a simple RPC module that has a R(remote) and L(ocal) interface. The R
 * interface is implemented on the remote side. The methods on this subclass are
 * then available remotely. I.e. this is a two way street. Void messages are
 * asynchronous, other messages block to a reply.
 *
 * @param <L> The type of the local server
 * @param <R> The type of the remote server
 */
public class Link<L, R> extends Thread implements Closeable {

	final static Logger						logger		= LoggerFactory.getLogger(Link.class);
	final static String[]					EMPTY		= new String[] {};
	final static JSONCodec					codec		= new JSONCodec();

	final DataInputStream					in;
	final DataOutputStream					out;
	final Class<R>							remoteClass;
	final AtomicInteger						id			= new AtomicInteger(10000);
	final ConcurrentMap<Integer, Result>	promises	= new ConcurrentHashMap<>();
	final AtomicBoolean						quit		= new AtomicBoolean(false);
	final AtomicBoolean						started		= new AtomicBoolean(false);
	final Executor							executor;
	volatile boolean						transfer	= false;
	private ThreadLocal<Integer>			msgid		= new ThreadLocal<>();

	R										remote;
	L										local;

	static class Result {
		boolean			resolved;
		byte[]			value;
		public boolean	exception;
	}

	/**
	 * Create a new link based on an in/output stream. This link is still
	 * closed. Call open to activate the link.
	 *
	 * @param remoteType the remote type
	 * @param in the incoming messages stream
	 * @param out where messages are send to
	 */
	public Link(Class<R> remoteType, InputStream in, OutputStream out, Executor es) {
		this(remoteType, new DataInputStream(in), new DataOutputStream(out), es);
	}

	/**
	 * Create a new link based on an Data in/output stream. This link is still
	 * closed. Call open to activate the link.
	 *
	 * @param remoteType the remote type
	 * @param in the incoming messages stream
	 * @param out where messages are send to
	 */
	public Link(Class<R> remoteType, DataInputStream in, DataOutputStream out, Executor es) {
		super("link::" + remoteType.getName());
		setDaemon(true);
		this.remoteClass = remoteType;
		this.in = new DataInputStream(in);
		this.out = new DataOutputStream(out);
		this.executor = es;
	}

	/**
	 * Create a new link based on a socket. This link is still closed. Call open
	 * to activate the link.
	 *
	 * @param type the type of the remote
	 * @param socket the socket
	 */
	public Link(Class<R> type, Socket socket, Executor es) throws IOException {
		this(type, socket.getInputStream(), socket.getOutputStream(), es);
	}

	/**
	 * Open the stream by providing the local interface to use
	 *
	 * @param local the local server
	 */
	@SuppressWarnings("unchecked")
	public void open(L local) {

		if (started.getAndSet(true) == true) {
			throw new IllegalStateException("Already running");
		}

		this.local = local;
		start();

	}

	/**
	 * Close this link. This will also close the peer link. If local implements
	 * Closeable then the local server will also be notified by calling close.
	 * Since we also close the remote peer link we also try to call close on the
	 * remote peer.
	 */
	@Override
	public void close() throws IOException {
		if (quit.getAndSet(true) == true)
			return; // already closed

		if (local instanceof Closeable)
			try {
				((Closeable) local).close();
			} catch (Exception e) {}

		if (!transfer) {
			if (in != null)
				try {
					in.close();
				} catch (Exception e) {}
			if (out != null)
				try {
					out.close();
				} catch (Exception e) {}
		}
	}

	/**
	 * Get the proxy to the remote peer.
	 *
	 * @return the remote peer
	 */
	@SuppressWarnings("unchecked")
	public synchronized R getRemote() {
		if (quit.get())
			return null;

		if (remote == null)
			remote = (R) Proxy.newProxyInstance(remoteClass.getClassLoader(), new Class<?>[] {
				remoteClass
			}, (target, method, args) -> {

				if (quit.get())
					throw new IllegalStateException("Already closed");

				Object hash = new Object();

				try {
					if (method.getDeclaringClass() == Object.class)
						return method.invoke(hash, args);

					int msgId;
					try {
						msgId = send(id.getAndIncrement(), method, args);
						if (method.getReturnType() == void.class) {
							promises.remove(msgId);
							return null;
						}
					} catch (Exception e1) {
						terminate(e1);
						throw e1;
					}

					return waitForResult(msgId, method.getGenericReturnType());
				} catch (InvocationTargetException e2) {
					throw Exceptions.unrollCause(e2, InvocationTargetException.class);
				} catch (InterruptedException e3) {
					interrupt();
					throw e3;
				} catch (Exception e4) {
					throw e4;
				}
			});
		return remote;
	}

	/**
	 * The thread method that receives the messages from the input stream
	 */
	@Override
	public void run() {
		while (!isInterrupted() && !transfer && !quit.get())
			try {
				final String cmd = in.readUTF();
				trace("rx " + cmd);
				final int id = in.readInt();

				int count = in.readShort();
				final List<byte[]> args = new ArrayList<>(count);
				for (int i = 0; i < count; i++) {
					int length = in.readInt();
					byte[] data = new byte[length];
					in.readFully(data);
					args.add(data);
				}

				Runnable r = () -> {
					try {
						msgid.set(id);
						executeCommand(cmd, id, args);
					} catch (Exception e) {
						e.printStackTrace();
					}
					msgid.set(-1);
				};
				executor.execute(r);
			} catch (SocketTimeoutException ee) {
				// Ignore, just to allow polling the actors again
			} catch (Exception ee) {

				terminate(ee);
				return;
			}
	}

	/**
	 * Create a server. This server will create instances when it is contacted.
	 *
	 * @param name the name of the server
	 * @param type the remote type
	 * @param port the local port
	 * @param host on which host to register
	 * @param local the local's peer interface
	 * @param localOnly only accept calls from the local host
	 * @return a closeable to close the link
	 */
	public static <L, R> Closeable server(String name, Class<R> type, int port, String host,
		Function<Link<L, R>, L> local, boolean localOnly, ExecutorService es) throws IOException {
		InetAddress addr = host == null ? InetAddress.getLocalHost()
			: host.equals("*") ? null : InetAddress.getByName(host);
		ServerSocket server = host == null ? new ServerSocket(port) : new ServerSocket(port, 50, addr);
		return server(name, type, server, local, localOnly, es);
	}

	/**
	 * Create a server. This server will create instances when it is contacted.
	 *
	 * @param name the name of the server
	 * @param type the remote type
	 * @param server the Socket Server
	 * @param local the local's peer interface
	 * @param localOnly only accept calls from the local host
	 * @return a closeable to close the link
	 */
	public static <L, R> Closeable server(String name, Class<R> type, ServerSocket server,
		Function<Link<L, R>, L> local, boolean localOnly, Executor es) {
		try {
			List<Link<L, R>> links = new ArrayList<>();
			Thread t = new Thread(name) {
				@Override
				public void run() {
					try {
						while (!isInterrupted())
							try {
								Socket socket = server.accept();
								InetAddress remoteSocketAddress = socket.getInetAddress();
								if (localOnly) {
									if (!(remoteSocketAddress.isLoopbackAddress()
										|| remoteSocketAddress.equals(InetAddress.getLocalHost()))) {
										logger.error("Warning remote address is requested to be local but is {}",
											remoteSocketAddress);
										IO.close(socket);
										continue;
									}
								}

								Link<L, R> link = new Link<>(type, socket, es);
								links.add(link);
								logger.info("Created link {}", name);
								link.open(local.apply(link));
							} catch (Exception e) {

								if (isInterrupted())
									return;

								logger.error("Error setting up link {}", name, e);
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e1) {
									interrupt();
									return;
								}
							}
					} finally {
						logger.info("Exiting link {}", name);
						IO.close(server);
					}
				}
			};
			t.start();
			return () -> {
				logger.info("Closing link {}", name);
				t.interrupt();
				IO.close(server);
				try {
					t.join();
				} catch (InterruptedException e) {}
				links.forEach(IO::close);
			};
		} catch (Exception e) {
			logger.error("Error setting up server socket link {}", name, e);
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Answer if this link is open
	 *
	 * @return true if this link is open
	 */
	public boolean isOpen() {
		return !quit.get();
	}

	/**
	 * Get the output stream
	 *
	 * @return the output stream
	 */
	public DataOutputStream getOutput() {
		assert transfer && !isOpen();
		return out;
	}

	/**
	 * Get the input stream
	 *
	 * @return the input stream
	 */
	public DataInputStream getInput() {
		assert transfer && !isOpen();
		return in;
	}

	/**
	 * Change the object used for the remote.
	 *
	 * @param remote peer
	 */
	@SuppressWarnings("unchecked")
	public void setRemote(Object remote) {
		this.remote = (R) remote;
	}

	/**
	 * Transfer the link to another and close this link object
	 *
	 * @param result the result of the call that caused the transfer
	 * @throws Exception
	 */
	public void transfer(Object result) throws Exception {
		transfer = true;
		quit.set(true);
		interrupt();
		join();
		if (result != null)
			send(msgid.get(), null, new Object[] {
				result
			});
		close();
	}

	/*
	 * Signalling function /
	 */

	protected void terminate(Exception t) {
		try {
			close();
		} catch (IOException e) {}
	}

	Method getMethod(String cmd, int count) {

		for (Method m : local.getClass()
			.getMethods()) {
			if (m.getDeclaringClass() == Link.class)
				continue;

			if (m.getName()
				.equals(cmd) && m.getParameterTypes().length == count) {
				return m;
			}
		}
		return null;
	}

	int send(int msgId, Method m, Object args[]) throws Exception {
		if (m != null)
			promises.put(msgId, new Result());
		trace("send");
		synchronized (out) {
			out.writeUTF(m != null ? m.getName() : "");
			out.writeInt(msgId);
			if (args == null)
				args = EMPTY;

			out.writeShort(args.length);
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];

				if (arg instanceof byte[]) {
					byte[] data = (byte[]) arg;
					out.writeInt(data.length);
					out.write(data);
				} else {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					codec.enc()
						.to(bout)
						.put(arg);
					byte[] data = bout.toByteArray();
					out.writeInt(data.length);
					out.write(data);
				}
			}
			out.flush();
			trace("sent");
		}
		return msgId;
	}

	void response(int msgId, byte[] data) {
		boolean exception = false;
		if (msgId < 0) {
			msgId = -msgId;
			exception = true;
		}

		Result o = promises.get(msgId);
		if (o != null) {
			synchronized (o) {
				trace("resolved");
				o.value = data;
				o.exception = exception;
				o.resolved = true;
				o.notifyAll();
			}
		}
	}

	@SuppressWarnings("unchecked")
	<T> T waitForResult(int id, Type type) throws Exception {
		long deadline = System.currentTimeMillis() + 300000;
		Result result = promises.get(id);

		try {
			do {
				synchronized (result) {
					if (result.resolved) {

						if (result.value == null)
							return null;

						if (result.exception) {
							String msg = codec.dec()
								.from(result.value)
								.get(String.class);
							System.out.println("Exception " + msg);
							throw new RuntimeException(msg);
						}

						if (type == byte[].class)
							return (T) result.value;

						T value = (T) codec.dec()
							.from(result.value)
							.get(type);
						return value;
					}

					long delay = deadline - System.currentTimeMillis();
					if (delay <= 0) {
						return null;
					}
					trace("start delay " + delay);
					result.wait(delay);
					trace("end delay " + (delay - (deadline - System.currentTimeMillis())));
				}
			} while (true);
		} finally {
			promises.remove(id);
		}
	}

	private void trace(String string) {
		logger.trace("{}", string);
	}

	/*
	 * Execute a command in a background thread
	 */

	void executeCommand(final String cmd, final int id, final List<byte[]> args) throws Exception {
		if (cmd.isEmpty())
			response(id, args.get(0));
		else {

			Method m = getMethod(cmd, args.size());
			if (m == null) {
				return;
			}

			Object parameters[] = new Object[args.size()];
			for (int i = 0; i < args.size(); i++) {
				Class<?> type = m.getParameterTypes()[i];
				if (type == byte[].class)
					parameters[i] = args.get(i);
				else {
					parameters[i] = codec.dec()
						.from(args.get(i))
						.get(m.getGenericParameterTypes()[i]);
				}
			}

			try {
				Object result = m.invoke(local, parameters);

				if (transfer || m.getReturnType() == void.class)
					return;

				try {
					send(id, null, new Object[] {
						result
					});
				} catch (Exception e) {
					terminate(e);
					throw e;
				}
			} catch (Throwable t) {
				t = Exceptions.unrollCause(t, InvocationTargetException.class);
				// t.printStackTrace();
				try {
					send(-id, null, new Object[] {
						t + ""
					});
				} catch (Exception e) {
					terminate(e);
					throw e;
				}
			}
		}
	}

}
