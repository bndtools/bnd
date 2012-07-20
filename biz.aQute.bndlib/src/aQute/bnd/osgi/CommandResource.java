package aQute.bnd.osgi;

import java.io.*;

import aQute.libg.command.*;

public class CommandResource extends WriteResource {
	final long		lastModified;
	final Builder	domain;
	final String	command;

	public CommandResource(String command, Builder domain, long lastModified) {
		this.lastModified = lastModified;
		this.domain = domain;
		this.command = command;
	}

	@Override
	public void write(OutputStream out) throws IOException, Exception {
		StringBuilder errors = new StringBuilder();
		StringBuilder stdout = new StringBuilder();
		try {
			domain.trace("executing command %s", command);
			Command cmd = new Command("sh");
			cmd.inherit();
			String oldpath = cmd.var("PATH");

			String path = domain.getProperty("-PATH");
			if (path != null) {
				path = path.replaceAll("\\s*,\\s*", File.pathSeparator);
				path = path.replaceAll("\\$\\{@\\}", oldpath);
				cmd.var("PATH", path);
				domain.trace("PATH: %s", path);
			}
			OutputStreamWriter osw = new OutputStreamWriter(out, "UTF-8");
			int result = cmd.execute(command, stdout, errors);
			osw.append(stdout);
			osw.flush();
			if (result != 0) {
				domain.error("executing command failed %s %s", command, stdout + "\n" + errors);
			}
		}
		catch (Exception e) {
			domain.error("executing command failed %s %s", command, e.getMessage());
		}
	}

	@Override
	public long lastModified() {
		return lastModified;
	}

}
