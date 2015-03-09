package aQute.remote.plugin;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.osgi.dto.DTO;

import aQute.lib.collections.MultiMap;
import aQute.lib.io.IO;
import aQute.libg.comlink.Link;
import aQute.libg.cryptography.SHA1;
import aQute.remote.api.Agent;
import aQute.remote.api.Event;
import aQute.remote.api.Supervisor;

public class AgentSupervisor implements Supervisor {
	private static final Map<File, Info> fileInfo = new ConcurrentHashMap<File, AgentSupervisor.Info>();
	private static final MultiMap<String, String> shaInfo = new MultiMap<String, String>();
	private static final byte[] EMPTY = new byte[0];
	
	private Appendable stdout;
	private Appendable stderr;
	private Agent agent;
	private CountDownLatch latch = new CountDownLatch(1);
	private int exitCode;
	private Link<Supervisor, Agent> link;

	static class Info extends DTO {
		public String sha;
		public long lastModified;
	}

	public static AgentSupervisor create(String host, int port)
			throws UnknownHostException, IOException, InterruptedException {
		while (true)
			try {
				Socket socket = new Socket(host, port);
				AgentSupervisor supervisor = new AgentSupervisor();
				Link<Supervisor, Agent> link = new Link<Supervisor, Agent>(
						Agent.class, supervisor, socket);
				supervisor.setAgent(link);
				link.open();
				return supervisor;
			} catch (ConnectException e) {
				Thread.sleep(200);
			}
	}

	@Override
	public void event(Event e) throws Exception {
		System.out.println(e);
		switch (e.type) {
		case exit:
			exitCode = e.code;
			latch.countDown();
			break;
		default:
			break;
		}

	}

	@Override
	public void stdout(String out) throws Exception {
		if (stdout != null)
			stdout.append(out);
	}

	@Override
	public void stderr(String out) throws Exception {
		if (stderr != null)
			stderr.append(out);
	}

	@Override
	public byte[] getFile(String sha) throws Exception {
		List<String> copy;
		synchronized (shaInfo) {
			List<String> list = shaInfo.get(sha);
			if (list == null)
				return EMPTY;

			copy = new ArrayList<String>(list);
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
		latch.countDown();
		link.close();
	}

	public int join() throws InterruptedException {
		latch.await();
		return exitCode;
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
				String sha = SHA1.digest(file).asHex();
				if (info.sha != null && !sha.equals(info.sha))
					shaInfo.remove(info.sha, file.getAbsolutePath());
				info.sha = sha;
				info.lastModified = file.lastModified();
				shaInfo.add(sha, file.getAbsolutePath());
			}
			return info.sha;
		}
	}

	public void setStdout(Appendable out) {
		this.stdout = out;
	}

	public void setStderr(Appendable err) {
		this.stderr = err;
	}

	public void setStreams(Appendable out, Appendable err) throws Exception {
		setStdout(out);
		setStderr(err);
		getAgent().redirect(true);
	}
}
