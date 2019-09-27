package aQute.remote.util;

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
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.json.JSONCodec;

/**
 * This is a simple RPC module that has a R and L interface. The R interface is
 * implemented on the remote side. The methods on this subclass are then
 * available remotely. I.e. this is a two way street. Void messages are
 * asynchronous, other messages block to a reply.
 *
 * @param <R>
 */
public class Link<L, R> extends Thread implements Closeable {
	private static final String[]			EMPTY		= new String[] {};
	static JSONCodec						codec		= new JSONCodec();

	final DataInputStream					in;
	final DataOutputStream					out;
	final Class<R>							remoteClass;
	final AtomicInteger						id			= new AtomicInteger(10000);
	final ConcurrentMap<Integer, Result>	promises	= new ConcurrentHashMap<>();
	final AtomicBoolean						quit		= new AtomicBoolean(false);
	volatile boolean						transfer	= false;
	private ThreadLocal<Integer>			msgid		= new ThreadLocal<>();

	R										remote;
	L										local;
	ExecutorService							executor	= Executors.newFixedThreadPool(4);

	static class Result {
		boolean			resolved;
		byte[]			value;
		public boolean	exception;
	}

	public Link(Class<R> remoteType, L local, InputStream in, OutputStream out) {
		this(remoteType, local, new DataInputStream(in), new DataOutputStream(out));
	}

	@SuppressWarnings("unchecked")
	public Link(Class<R> remoteType, L local, DataInputStream in, DataOutputStream out) {
		super("link::" + remoteType.getName());
		setDaemon(true);
		this.remoteClass = remoteType;
		this.local = local == null ? (L) this : local;
		this.in = new DataInputStream(in);
		this.out = new DataOutputStream(out);
	}

	public Link(Class<R> type, L local, Socket socket) throws IOException {
		this(type, local, socket.getInputStream(), socket.getOutputStream());
	}

	public void open() {
		if (isAlive())
			throw new IllegalStateException("Already running");

		if (in != null)
			start();
	}

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
		executor.shutdownNow();
	}

	@SuppressWarnings("unchecked")
	public synchronized R getRemote() {
		if (quit.get())
			return null;

		if (remote == null)
			remote = (R) Proxy.newProxyInstance(remoteClass.getClassLoader(), new Class<?>[] {
				remoteClass
			}, (target, method, args) -> {
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
						return null;
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
						// e.printStackTrace();
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
		// TODO Auto-generated method stub

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
					return;
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
					return;
				}
			}
		}
	}

	public boolean isOpen() {
		return !quit.get();
	}

	public DataOutputStream getOutput() {
		assert transfer && !isOpen();
		return out;
	}

	public DataInputStream getInput() {
		assert transfer && !isOpen();
		return in;
	}

	@SuppressWarnings("unchecked")
	public void setRemote(Object remote) {
		this.remote = (R) remote;
	}

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

}
