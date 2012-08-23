package aQute.jpm.lib;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.net.*;

import aQute.lib.io.*;

public class Service {
	final ServiceData				data;
	final JustAnotherPackageManager	jpm;
	final File lock;
	Service(JustAnotherPackageManager jpm, ServiceData data) throws Exception {
		this.jpm = jpm;
		this.data = data;
		this.lock = new File(data.lock);
	}

	public String start() throws Exception {
		if (lock.createNewFile()) {
			try {
				int result = jpm.platform.launchService(data);
				if (result == 0)
					return null;

				return "Could not launch service " + data.name + " return value " + result;
			}
			catch (Throwable t) {
				IO.delete(lock);
				return String.format("Failed to start %s for %s", data.name, t.getMessage());
			}
		}
		return "Already running";
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
			}
			finally {
				lock.delete();
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

		byte data[] = m.getBytes("UTF-8");
		DatagramPacket p = new DatagramPacket(data, 0, data.length, InetAddress.getByAddress(new byte[] {
				127, 0, 0, 1
		}), port);
		DatagramSocket dsocket = new DatagramSocket();
		dsocket.setReceiveBufferSize(5000);
		dsocket.setSoTimeout(5000);
		try {
			dsocket.send(p);
			byte[] buffer = new byte[1000];
			DatagramPacket dp = new DatagramPacket(buffer, 1000);
			dsocket.receive(dp);
			return new String(dp.getData(), dp.getOffset(), dp.getLength(), "UTF-8");
		}
		catch (SocketTimeoutException stoe) {
			return "Timed out";
		}
		finally {
			dsocket.close();
		}
	}

	int getPort() {
		try {
			String l = collect(data.lock);
			String parts[] = l.split(":");
			return Integer.parseInt(parts[0]);
		}
		catch (Exception e) {
			return -1;
		}
	}

	int getPid() throws IOException {
		try {
			String l = collect(data.lock);
			String parts[] = l.split(":");
			return Integer.parseInt(parts[1]);
		}
		catch (Exception e) {
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
		return jpm.createService(data);
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
}
