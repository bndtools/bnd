package aQute.bnd.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;
import aQute.lib.fileset.FileSet;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.command.Command;

/**
 * Implements a mini make builder that is run just before we build the bundle.
 */
class Makefile extends Processor {
	private final static Logger	logger		= LoggerFactory.getLogger(Makefile.class);

	private Parameters			parameters;
	private List<Cmd>			commands	= new ArrayList<>();
	private String				path;

	class Cmd {

		Pattern				report;
		File				target;
		FileSet				source;
		String				command;
		String				name;
		String				make;
		Map<String, String>	env	= new HashMap<>();

		void execute() {

			if (!isStale())
				return;

			StringBuilder errors = new StringBuilder();
			StringBuilder stdout = new StringBuilder();
			logger.debug("executing command {}", name);
			Command cmd = new Command("sh");
			cmd.setTimeout(1, TimeUnit.MINUTES);
			cmd.setCwd(getBase());
			cmd.inherit();

			if (path != null)
				cmd.var("PATH", path);

			for (Map.Entry<String, String> e : env.entrySet()) {
				String key = e.getKey();
				if (key.endsWith(":"))
					continue;

				cmd.var(key, e.getValue());
			}

			try {
				String command = this.command;

				command = command.replaceAll("\\$@", target.toString());
				if (source != null)
					command = command.replaceAll("\\$\\^", Strings.join(" ", source.getFiles()));

				logger.debug("cmd {}", command);

				int result = cmd.execute(command, stdout, errors);

				if (stdout.length() > 0)
					logger.debug("{} stdout: {}", name, stdout);
				if (errors.length() > 0)
					logger.debug("{} stderr: {}", name, errors);

				if (result != 0) {
					IO.delete(target);
					boolean found = false;
					if (report != null) {
						found |= parseErrors(report, errors);
						found |= parseErrors(report, stdout);
					}
					if (!found) {
						SetLocation location = error("%s: -prepare exit status = %s: %s", name, result,
							stdout + "\n" + errors);
						FileLine fl = getParent().getHeader("-prepare", make);
						if (fl != null) {
							fl.set(location);
						}
					}
				}
			} catch (Exception e) {
				exception(e, "%s: -prepare", name);
				IO.delete(target);
			}
		}

		boolean isStale() {
			if (source != null) {

				if (!target.isFile())
					return true;

				long lastModified = target.lastModified();

				for (File s : source.getFiles()) {
					if (lastModified < s.lastModified())
						return true;
				}
				return false;
			} else
				return true;
		}

		private boolean parseErrors(Pattern report, StringBuilder errors) {
			Matcher m = report.matcher(errors);
			boolean found = false;
			while (m.find()) {
				found = true;
				logger.debug("found errors {}", m.group());
				String type = getGroup(m, "type");

				SetLocation location = "warning".equals(type) ? warning("%s: %s", name, m.group("message"))
					: error("%s: %s", name, m.group("message"));

				String fileName = getGroup(m, "file");
				if (fileName != null) {
					File file = IO.getFile(getBase(), fileName);
					if (file.isFile()) {
						location.file(file.getAbsolutePath());
					}

					String ls = getGroup(m, "line");
					if (ls != null && ls.matches("\\d+"))
						location.line(Integer.parseInt(ls));
					logger.debug("file {}#{}", file, ls);
				}
			}
			return found;
		}

		private String getGroup(Matcher m, String group) {
			try {
				return m.group(group);
			} catch (Exception e) {}
			return null;
		}
	}

	Makefile(Processor project) {
		super(project);
		setBase(project.getBase());
		getSettings(project);

		this.parameters = new Parameters(mergeProperties("-prepare"), this);

		for (Map.Entry<String, Attrs> e : parameters.entrySet())
			try {
				Cmd cmd = new Cmd();

				cmd.make = removeDuplicateMarker(e.getKey());

				Attrs attrs = e.getValue();

				cmd.name = cmd.make;
				if (attrs.containsKey("name:"))
					cmd.name = attrs.get("name:");

				String parts[] = cmd.make.trim()
					.split("\\s*<=\\s*");
				if (parts.length > 2) {
					error("Command with dep spec %s has too many <= separated parts", cmd.name);
					continue;
				}

				cmd.target = IO.getFile(getBase(), parts[0]);
				cmd.source = parts.length > 1 ? new FileSet(getBase(), parts[1]) : null;

				if (attrs.containsKey("report:"))
					cmd.report = Pattern.compile(attrs.get("report:"));

				if (attrs.containsKey("command:"))
					cmd.command = attrs.remove("command:");
				else {
					error("-prepare: No command specified (command:) for %s", cmd.name);
					continue;
				}

				commands.add(cmd);
			} catch (Exception ee) {
				exception(ee, "-prepare: Could not parse command %s : %s", e.getKey(), e.getValue());
			}

		if (this.parameters.size() > 0) {
			path = getProperty("-PATH");
			if (path != null) {
				String oldpath = System.getenv("PATH");
				path = path.replaceAll("\\s*,\\s*", File.pathSeparator);
				if (oldpath != null && !oldpath.isEmpty()) {
					if (path.contains("${@}"))
						path = path.replaceAll("\\$\\{@\\}", oldpath);
					else
						path = path + File.pathSeparator + oldpath;
				}
				logger.debug("PATH: {}", path);
			}

		}
	}

	void make() {
		for (Cmd cmd : commands) {
			cmd.execute();
		}
		getParent().getInfo(this);
	}
}
