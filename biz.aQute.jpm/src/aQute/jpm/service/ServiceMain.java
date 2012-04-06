package aQute.jpm.service;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

public class ServiceMain extends Thread {
	static File				lock;
	static Date				last	= new Date();
	static String			message	= "<>";
	static DatagramSocket	socket;
	static Class<?>			mainClass;
	static Method			serviceMethod;
	static Thread			mainThread;

	public static void main(String args[]) throws Exception, SecurityException,
			NoSuchMethodException {
		lock = new File(args[0]).getAbsoluteFile();
		lock.deleteOnExit(); // we want it automatically be gone when we die
		if (!lock.exists())
			throw new IllegalArgumentException("Must start with a valid lock file " + lock);

		socket = new DatagramSocket();
	
		write(lock, socket.getLocalPort() + ":" + System.getProperty("pid"));

		ServiceMain main = new ServiceMain();
		main.start();

		mainClass = ServiceMain.class.getClassLoader().loadClass(args[1]);

		try {
			serviceMethod = mainClass.getDeclaredMethod("daemon", boolean.class);
			serviceMethod.invoke(null, true);
		} catch (NoSuchMethodException e) {
			String[] args2 = new String[args.length - 2];
			System.arraycopy(args, 2, args2, 0, args2.length);

			Method m = mainClass.getDeclaredMethod("main", String[].class);
			m.invoke(null, (Object) args2);
		}
	}

	ServiceMain() {
		super("jpm main service thread");
	}

	public void run() {
		try {
			byte[] buffer = new byte[1000];
			DatagramPacket dp = new DatagramPacket(buffer, 1000);

			boolean stopped = false;
			socket.setSoTimeout(5000);

			while (!isInterrupted() && !stopped) {
				try {
					socket.receive(dp);
					if (dp.getAddress().isLoopbackAddress()) {
						String s = new String(dp.getData(), dp.getOffset(), dp.getLength());
						String reply;

						if (s.startsWith("STOP")) {
							stopped = true;
							reply = "200 STOPPING";

							if (serviceMethod != null) {
								try {
									serviceMethod.invoke(null, false);
								} catch (Exception e) {
									// Ignore
								}
								mainThread.interrupt();
								mainThread.join(2000);
							}

						} else if (s.startsWith("STATUS")) {
							reply = "200 OK " + last + " " + message;
						} else
							reply = "404 UNKNOWN REQUEST " + s;

						byte data[] = reply.getBytes();
						DatagramPacket p = new DatagramPacket(data, 0, data.length,
								dp.getAddress(), dp.getPort());
						socket.send(p);
					} else 
						System.err.println("Received UDP from external source");
				} catch (SocketTimeoutException stoe) {
					System.out.println("checking lock " + lock + " " + lock.exists());
					if (!lock.exists())
						break;
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			try {
				socket.close();
			} finally {
				try {
					lock.delete();
				} finally {
					System.exit(1);
				}
			}
		}
	}

	private static void write(File f, String response) throws IOException {
		FileWriter fw = new FileWriter(f);
		try {
			fw.append(response);
		} finally {
			fw.close();
		}
	}

	public synchronized static void setMessage(String m) {
		last = new Date();
		message = m;
	}
}
