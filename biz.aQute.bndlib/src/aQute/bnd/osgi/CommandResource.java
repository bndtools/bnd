package aQute.bnd.osgi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.libg.command.Command;

public class CommandResource extends WriteResource {
	private final static Logger	logger	= LoggerFactory.getLogger(CommandResource.class);
	final long		lastModified;
	final Builder	domain;
	final String	command;
	final File		wd;

	public CommandResource(String command, Builder domain, long lastModified, File wd) {
		this.lastModified = lastModified;
		this.domain = domain;
		this.command = command;
		this.wd = wd;
	}

	@Override
	public void write(OutputStream out) throws IOException, Exception {
		StringBuilder errors = new StringBuilder();
		StringBuilder stdout = new StringBuilder();
		logger.debug("executing command {}", command);
		Command cmd = new Command("sh");
		cmd.setCwd(wd);
		cmd.inherit();
		String oldpath = cmd.var("PATH");

		String path = domain.getProperty("-PATH");
		if (path != null) {
			path = path.replaceAll("\\s*,\\s*", File.pathSeparator);
			path = path.replaceAll("\\$\\{@\\}", oldpath);
			cmd.var("PATH", path);
			logger.debug("PATH: {}", path);
		}
		OutputStreamWriter osw = new OutputStreamWriter(out, "UTF-8");
		int result = cmd.execute(command, stdout, errors);
		osw.append(stdout);
		osw.flush();
		if (result != 0) {
			domain.error("Cmd '%s' failed in %s. %n  %s%n  %s", command, wd, errors, stdout);
		}
	}

	@Override
	public long lastModified() {
		return lastModified;
	}

}
