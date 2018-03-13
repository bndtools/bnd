package aQute.remote.util;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import aQute.bnd.util.dto.DTO;
import aQute.lib.collections.MultiMap;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA1;

/**
 * This is a base class that provides the basic functionality of a supervisor.
 * In general an actual supervisor extends this class to provide the
 * functionality to use on the client side.
 *
 * @param <Supervisor> The supervisor type
 * @param <Agent> The agent type
 */
public class AgentSupervisor<Supervisor, Agent> {
	private static final Map<File, Info>			fileInfo	= new ConcurrentHashMap<>();
	private static final MultiMap<String, String>	shaInfo		= new MultiMap<>();
	private static final int						connectWait	= 200;
	private static byte[]							EMPTY		= new byte[0];
	private Agent									agent;
	private CountDownLatch							latch		= new CountDownLatch(1);
	protected volatile int							exitCode;
	private Link<Supervisor, Agent>					link;
	private AtomicBoolean							quit		= new AtomicBoolean(false);

	static class Info extends DTO {
		public String	sha;
		public long		lastModified;
	}

	protected void connect(Class<Agent> agent, Supervisor supervisor, String host, int port) throws Exception {
		connect(agent, supervisor, host, port, -1);
	}

	protected void connect(Class<Agent> agent, Supervisor supervisor, String host, int port, final int timeout)
		throws Exception {
		if (timeout < -1) {
			throw new IllegalArgumentException("timeout can not be less than -1");
		}

		int retryTimeout = timeout;

		while (true) {
			try {
				Socket socket = new Socket();
				socket.connect(new InetSocketAddress(host, port), Math.max(timeout, 0));
				link = new Link<>(agent, supervisor, socket);
				this.setAgent(link);
				link.open();
				return;
			} catch (ConnectException e) {
				if (retryTimeout == 0) {
					throw e;
				}
				if (retryTimeout > 0) {
					retryTimeout = Math.max(retryTimeout - connectWait, 0);
				}
				Thread.sleep(connectWait);
			}
		}
	}

	public byte[] getFile(String sha) throws Exception {
		List<String> copy;
		synchronized (shaInfo) {
			List<String> list = shaInfo.get(sha);
			if (list == null)
				return EMPTY;

			copy = new ArrayList<>(list);
		}
		for (String path : copy) {
			File f = new File(path);
			if (f.isFile()) {
				byte[] data = IO.read(f);
				return data;
			}
		}
		return EMPTY;
	}

	public void setAgent(Link<Supervisor, Agent> link) {
		this.agent = link.getRemote();
		this.link = link;
	}

	public void close() throws IOException {
		if (quit.getAndSet(true))
			return;

		if (link.isOpen())
			link.close();

		latch.countDown();
	}

	public int join() throws InterruptedException {
		latch.await();
		return exitCode;
	}

	protected void exit(int exitCode) {
		this.exitCode = exitCode;
		try {
			close();
		} catch (Exception e) {
			// ignore
		}
	}

	public Agent getAgent() {
		return agent;
	}

	public String addFile(File file) throws NoSuchAlgorithmException, Exception {
		file = file.getAbsoluteFile();
		Info info = fileInfo.get(file);
		if (info == null) {
			info = new Info();
			fileInfo.put(file, info);
			info.lastModified = -1;
		}

		synchronized (shaInfo) {
			if (info.lastModified != file.lastModified()) {
				String sha = SHA1.digest(file)
					.asHex();
				if (info.sha != null && !sha.equals(info.sha))
					shaInfo.removeValue(info.sha, file.getAbsolutePath());
				info.sha = sha;
				info.lastModified = file.lastModified();
				shaInfo.add(sha, file.getAbsolutePath());
			}
			return info.sha;
		}
	}

	public boolean isOpen() {
		return link.isOpen();
	}

}
