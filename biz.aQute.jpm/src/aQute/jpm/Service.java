package aQute.jpm;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.net.*;

import aQute.jpm.service.*;
import aQute.libg.version.*;

public class Service {

	File				base;
	File				lock;
	JustAnotherPackageManager	jpm;

	Service(JustAnotherPackageManager jpm, File base, String name) throws Exception {
		this.jpm = jpm;
		this.base = base;
		this.lock = new File(base, "lock");
	}

	
	public boolean start() throws Exception {
		System.out.println("Creating lock " + lock + " " + lock.exists());
		if (lock.createNewFile()) {
			File start = new File(base,"start");
			File wdir = new File(base, "work");
			wdir.mkdirs();
			
			Process p = Runtime.getRuntime().exec(start.getAbsolutePath(), new String[]{}, wdir);
			int n = p.waitFor();
			if ( n != 0 ) {
				System.out.println("Failed to start " + n);
				lock.delete();
			}
			return true;
		} else
			System.out.println("Lock not created, already existed");
		return false;
	}

	public boolean stop() throws Exception {
		if (lock.exists()) {
			if (!lock.canWrite())
				throw new SecurityException("Cannot write lock");

			try {
				send(getPort(), "STOP");
			} finally {
				// TODO wait for lock to disappear
				lock.delete();
			}
			return true;
		}
		return false;
	}

	public String status() throws Exception {
		if (lock.canWrite() && lock.exists()) {
			return send(getPort(), "STATUS");
		}
		return null;
	}

	private String send(int port, String m) throws Exception {
		byte data[] = m.getBytes();
		DatagramPacket p = new DatagramPacket(data, 0, data.length, InetAddress.getLocalHost(),
				port);
		DatagramSocket dsocket = new DatagramSocket();
		try {
			dsocket.send(p);
			byte[] buffer = new byte[1000];
			DatagramPacket dp = new DatagramPacket(buffer, 1000);
			dsocket.setSoTimeout(5000);
			dsocket.receive(dp);
			return new String(dp.getData(), dp.getOffset(), dp.getLength());
		} finally {
			dsocket.close();
		}
	}

	int getPort() throws IOException {
		String l = collect(lock);
		String parts[] = l.split(":");		
		return Integer.parseInt(parts[0]);
	}

	int getPid() throws IOException {
		String l = collect(lock);
		String parts[] = l.split(":");		
		return Integer.parseInt(parts[1]);
	}

	public void createService(File target, String main) throws Exception {
		base.mkdirs();
		jpm.platform.createService(base, new File[]{target,jpm.repo.get("biz.aQute.jpm.service", new VersionRange("0"), 1)}, ServiceMain.class.getName(), lock.getAbsolutePath(), main);
	}

	public boolean isRunning() {
		return lock.exists();
	}
}
