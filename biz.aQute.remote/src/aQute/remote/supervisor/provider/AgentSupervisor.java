package aQute.remote.supervisor.provider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import aQute.lib.io.IO;
import aQute.remote.api.Agent;

public class AgentSupervisor {
	SupervisorClient<Agent> sub;
	private Thread stdinReader;
	private InputStream in;

	public AgentSupervisor(SupervisorClient<Agent> sub) {
		this.sub=sub;
	}
	
	public static AgentSupervisor create(String host, int port) throws Exception {
		SupervisorClient<Agent> sub = SupervisorClient.link(Agent.class, 	host, port);
		return new AgentSupervisor(sub);
	}
	public void setStreams(final InputStream in, Appendable out, Appendable err) {
		sub.setStdout(out);
		sub.setStderr(err);
		this.in = in;
		stdinReader = new Thread("stdin reader") {
			public void run() {
				try {
					BufferedReader reader = IO.reader(in);
					while (!isInterrupted()) {
						String line = reader.readLine();
						if (line == null)
							return;

						sub.getAgent().stdin(line);
					}
				} catch (Exception e) {
					return;
				}
			}
		};
		stdinReader.start();
	}

	public void close() throws IOException {
		sub.close();
		in.close();
	}

	public String addFile(File f) throws NoSuchAlgorithmException, Exception {
		return sub.addFile(f);
	}

	public Agent getAgent() {
		return sub.getAgent();
	}

	public int join() throws InterruptedException {
		return sub.join();
	}
}
