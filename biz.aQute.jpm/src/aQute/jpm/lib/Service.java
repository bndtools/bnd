package aQute.jpm.lib;

import static aQute.lib.io.IO.collect;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;
public class Service {
	static final int				BUFFER_SIZE	= IOConstants.PAGE_SIZE * 16;

	final ServiceData				data;
	final JustAnotherPackageManager	jpm;
	final File						lock;

	Service(JustAnotherPackageManager jpm, ServiceData data) throws Exception {
		this.jpm = jpm;
		this.data = data;
		this.lock = new File(data.lock);
	}

	public String start() throws Exception {
		return start(false);
	}

	public String start(boolean force) throws Exception {
		if (lock.exists()) {
			if (!force)
				return "Already running";

			IO.delete(lock);
			Thread.sleep(2000);
		}

		if (lock.createNewFile()) {

			jpm.platform.chown(data.user, false, lock);
			try {
				Thread.sleep(1000);
				int result = jpm.platform.launchService(data);
				if (result != 0)
					return "Could not launch service " + data.name + " return value " + result;

				long start = System.currentTimeMillis();
				while (System.currentTimeMillis() - start < 10000) {
					Thread.sleep(100);
					if (getPort() != -1)
						return null;

				}
				IO.delete(lock);
				return "Could not establish a link to the service, likely failed to start";
			} catch (Throwable t) {
				IO.delete(lock);
				return String.format("Failed to start %s for %s", data.name, t);
			}
		}
		return "Could not create lock file";
	}

	public String stop() throws Exception {
		if (lock.exists()) {
			if (!lock.canWrite()) {
				return String.format("Cannot write lock %s", data.lock);
			}
			try {
				send(getPort(), "STOP");
				for (int i = 0; i < 20; i++) {
					if (!lock.exists())
						return null;

					Thread.sleep(500);
				}
				return "Lock was not deleted by service in time (waited 10 secs)";
			} finally {
				IO.delete(lock);
			}
		}
		return "Not running";
	}

	public String status() throws Exception {
		if (lock.canWrite() && lock.exists())
			return send(getPort(), "STATUS");

		return null;
	}

	private String send(int port, String m) throws Exception {
		if (port == -1)
			return "Invalid port";

		byte data[] = m.getBytes(UTF_8);
		DatagramPacket p = new DatagramPacket(data, 0, data.length, InetAddress.getLoopbackAddress(), port);
		DatagramSocket dsocket = new DatagramSocket();
		dsocket.setReceiveBufferSize(5000);
		dsocket.setSoTimeout(5000);
		try {
			dsocket.send(p);
			byte[] buffer = new byte[BUFFER_SIZE];
			DatagramPacket dp = new DatagramPacket(buffer, BUFFER_SIZE);
			dsocket.receive(dp);
			return new String(dp.getData(), dp.getOffset(), dp.getLength(), UTF_8);
		} catch (SocketTimeoutException stoe) {
			return "Timed out";
		} finally {
			dsocket.close();
		}
	}

	int getPort() {
		try {
			String l = collect(data.lock);
			String parts[] = l.split(":");
			return Integer.parseInt(parts[0]);
		} catch (Exception e) {
			// e.printStackTrace();
			return -1;
		}
	}

	int getPid() throws IOException {
		try {
			String l = collect(data.lock);
			String parts[] = l.split(":");
			return Integer.parseInt(parts[1]);
		} catch (Exception e) {
			return -1;
		}
	}

	public boolean isRunning() {
		return lock.exists();
	}

	@Override
	public String toString() {
		return data.name;
	}

	public ServiceData getServiceData() {
		return data;
	}

	public String update(ServiceData data) throws Exception {
		return jpm.createService(data, true);
	}

	public String trace(boolean b) throws Exception {
		if (!isRunning())
			return "Not running";

		if (b)
			send(getPort(), "TRACE-ON");
		else
			send(getPort(), "TRACE-OFF");
		return null;
	}

	public void remove() throws Exception {
		try {
			stop();
		} catch (Exception e) {}

		IO.deleteWithException(new File(data.sdir));
		jpm.platform.deleteService(data);
	}

	public void clear() {
		IO.delete(new File(data.log));
		File work = new File(data.work);
		IO.delete(work);
		try {
			IO.mkdirs(work);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
