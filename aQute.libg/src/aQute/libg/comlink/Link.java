package aQute.libg.comlink;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import aQute.lib.json.*;

/**
 * This is a simple RPC module that has a R and L interface. The R interface is
 * implemented on the remote side. The methods on this subclass are then
 * available remotely. I.e. this is a two way street. Void messages are
 * asynchronous, other messages block to a reply.
 * 
 * @param <R>
 */
public class Link<L, R> extends Thread {
	private static final String[]	EMPTY	= new String[] {};

	static JSONCodec					codec		= new JSONCodec();

	final DataInputStream				in;
	final DataOutputStream				out;
	final Class<R>						remoteClass;
	final AtomicInteger					id			= new AtomicInteger(10000);
	final ConcurrentMap<Integer,Result>	promises	= new ConcurrentHashMap<Integer,Result>();

	R									remote;
	L									local;
	volatile boolean					quit;

	static class Result {
		boolean			resolved;
		byte[]			value;
		public boolean	exception;
	}

	@SuppressWarnings("unchecked")
	public Link(Class<R> remoteType, L local, InputStream in, OutputStream out) {
		super("link::" + remoteType.getName());
		setDaemon(true);
		this.remoteClass = remoteType;
		this.local = local == null ? (L) this : local;
		this.in = new DataInputStream(in);
		this.out = new DataOutputStream(out);
	}

	public Link(Class<R> type, L local, Socket socket) throws IOException {
		super("link::" + type.getName());
		this.remoteClass = type;
		this.local = local;
		this.in = new DataInputStream(socket.getInputStream());
		this.out = new DataOutputStream(socket.getOutputStream());
	}

	public void open() {
		start();
	}

	public void close() throws IOException {
		quit = true;
		in.close();
		out.close();
	}

	@SuppressWarnings("unchecked")
	public synchronized R getRemote() {
		if (remote == null)
			remote = (R) Proxy.newProxyInstance(remoteClass.getClassLoader(), new Class< ? >[] {
				remoteClass
			}, new InvocationHandler() {

				public Object invoke(Object target, Method method, Object[] args) throws Throwable {

					int msgId = send(id.getAndIncrement(), method, args);
					if (method.getReturnType() == void.class) {
						promises.remove(msgId);
						return null;
					}

					return waitForResult(msgId, method.getGenericReturnType());
				}
			});
		return remote;
	}

	public void run() {
		while (true)
			try {
				String cmd = in.readUTF();
				int id = in.readInt();

				int count = in.readShort();
				List<byte[]> args = new ArrayList<byte[]>(count);
				for (int i = 0; i < count; i++) {
					int length = in.readInt();
					byte[] data = new byte[length];
					in.readFully(data);
					args.add(data);
				}

				if (cmd.isEmpty())
					response(id, args.get(0));
				else {

					Method m = getMethod(cmd, count);
					if (m == null) {
						System.err.println("Unknown message received " + cmd);
						continue;
					}

					Object parameters[] = new Object[count];
					for (int i = 0; i < count; i++) {
						Class< ? > type = m.getParameterTypes()[i];
						if (type == byte[].class)
							parameters[i] = args.get(i);
						else {
							parameters[i] = codec.dec().from(args.get(i)).get(m.getParameterTypes()[i]);
						}
					}

					try {
						Object result = m.invoke(local, parameters);

						if (m.getReturnType() == void.class)
							continue;

						send(id, null, new Object[] {
							result
						});
					}
					catch (Throwable t) {
						send(-id, null, new Object[] {
							t.toString()
						});
					}
				}
			}
			catch (EOFException e) {
				// It is over and out
				return;
			}
			catch (IOException e) {
				if (quit)
					return;

				e.printStackTrace();

				try {
					Thread.sleep(500);
				}
				catch (InterruptedException e1) {}
			}
			catch (Exception ee) {
				if (quit)
					return;
				ee.printStackTrace();
			}
	}

	private Method getMethod(String cmd, int count) {

		for (Method m : local.getClass().getMethods()) {
			if (m.getDeclaringClass() == Link.class)
				continue;

			if (m.getName().equals(cmd) && m.getParameterTypes().length == count) {
				return m;
			}
		}
		return null;
	}

	int send(int msgId, Method m, Object args[]) throws Exception {
		if (m != null)
			promises.put(msgId, new Result());

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
					codec.enc().to(bout).put(arg);
					byte[] data = bout.toByteArray();
					out.writeInt(data.length);
					out.write(data);
				}
			}
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

						if (result.exception)
							throw new RuntimeException(codec.dec().from(result.value).get(String.class));

						if (type == byte[].class)
							return (T) result.value;

						T value = (T) codec.dec().from(result.value).get(type);
						return value;
					}

					long delay = deadline - System.currentTimeMillis();
					if (delay <= 0) {
						System.err.println("Timeout");
						return null;
					}
					result.wait(delay);
				}
			} while (true);
		}
		finally {
			promises.remove(id);
		}
	}

}
