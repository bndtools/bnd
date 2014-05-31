package aQute.bnd.enroute.commands;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import aQute.bnd.build.*;
import aQute.bnd.main.*;
import aQute.bnd.osgi.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;

@Description("OSGi enRoute Commands")
public class EnrouteCommand {

	private bnd				bnd;
	private EnrouteOptions	opts;

	public EnrouteCommand(bnd bnd, EnrouteOptions opts) throws Exception {
		this.bnd = bnd;
		this.opts = opts;

		List<String> args = opts._();
		if (args.size() == 0) {
			// Default command
			printHelp();
		} else {
			// Other commands
			String cmd = args.remove(0);
			String help = opts._command().execute(this, cmd, args);
			if (help != null) {
				bnd.out.print(help);
			}
		}
	}

	private void printHelp() throws Exception {
		Formatter f = new Formatter();
		opts._command().help(f, this);
		bnd.out.println(f);
		f.close();
	}

	@Description("Create a workspace in the base directory (working directory or set with bnd -b <base>). The name of the workspace should be a "
			+ "Bundle Symbolic Name type (like com.example.whatever). If another type of name is "
			+ "necessary you can override it with --anyname. Two directories will be created. One for the bnd"
			+ "workspace and the other for the eclipse workspace. Having two directories makes life a lot"
			+ "easier when you use git, it allows you to clear the bnd workspace with 'git clean -fdx' without"
			+ "killing any personal Eclipse data. It also prevents you from accidentally storing this"
			+ "personal data in git. If you know better, use --single to let these workspaces overlap.\n"
			+ "The directory for the workspaces must be empty unless you specify --update or --force.%n"
			+ "This template will also install a gradle build system and a travis continuous integration"
			+ "control file.%n"
			+ "A workspace cannot be created at the root of a file system. The general layout of the"
			+ "file system is%n"
			+ "%n%n" //
			+ "    ../<base>/%n" //
			+ "          <ws>/%n"
			+ "              cnf/%n"
			+ "                  build.bnd%n"
			+ "                  ....%n"
			+ "          ECLIPSE/%n"
			+ "              <ws>/%n"
			+ "                  .metadata/%n" + "                      ....%n")
	@Arguments(arg = "workspace")
	public interface WorkspaceOptions extends Options {
		@Description("Create a single workspace for the Eclipse workspace and the bnd workspace. This is not recommended.")
		boolean single();

		@Description("In general a workspace should follow the rules for Bundle Symbolic%n"
				+ "Names. If this option is set any name (including one with file seperators) is fine.")
		boolean anyname();

		@Description("Existing files are updated when they are older than the ones in the template")
		boolean update();

		@Description("Update a workspace overwrite all existing files.")
		boolean force();
	}

	public void _workspace(WorkspaceOptions opts) throws Exception {
		File base = bnd.getBase();

		String name = opts._().get(0);

		File workspaceDir = Processor.getFile(base, name);
		name = workspaceDir.getName();
		base = workspaceDir.getParentFile();

		if (base == null) {
			bnd.error("You cannot create a workspace in the root (%s). The parent of a workspace %n"
					+ "must be a valid directory. Recommended is to dedicate a directory to %n"
					+ "all (or related) workspaces.", workspaceDir);
			return;
		}

		if (!opts.anyname() && !Verifier.isBsn(name)) {
			bnd.error("You specified a workspace name that does not follow the recommended pattern "
					+ "(it should be like a Bundle Symbolic name). It is a bit pedantic but it "
					+ "really helps hwne you get many workspaces. If you insist on this name, use the -a/--anyname option.");
			return;
		}

		Workspace ws = bnd.getWorkspace((File) null);

		if (ws != null && ws.isValid()) {
			bnd.error(
					"You are currently in a workspace already (%s) in %s. You can only create a new workspace outside an existing workspace",
					ws, base);
			return;
		}

		File eclipse = new File(base, "_ECLIPSE");
		if (!eclipse.isDirectory()) {
			if (opts.single())
				eclipse = base;
			else
				eclipse.mkdirs();
		}

		if (!base.isDirectory()) {
			bnd.error("Could not create directory for the bnd workspace");
		} else if (!eclipse.isDirectory()) {
			bnd.error("Could not create directory for the Eclipse workspace");
		}

		File eclipseDir = new File(eclipse, name);

		workspaceDir.mkdirs();
		eclipseDir.mkdirs();

		if (!workspaceDir.isDirectory()) {
			bnd.error("Could not create the workspace directory %s", workspaceDir);
			return;
		}

		if (!opts.update() && !opts.force() && workspaceDir.list().length > 0) {
			bnd.error(
					"The workspace directory %s is not empty, specify -u/--update to update or -f/--force to replace",
					workspaceDir);
		}

		if (!opts.single()) {
			if (!eclipseDir.isDirectory()) {
				bnd.error("Could not create the eclipse directory %s", eclipseDir);
				return;
			}
		}

		InputStream in = getClass().getResourceAsStream("/templates/enroute.zip");
		if (in == null) {
			bnd.error("Cannot find template in this jar %s", "/templates/enroute.zip");
			return;
		}

		Pattern glob = Pattern.compile("[^/]+|cnf/.*");

		copy(workspaceDir, in, glob, opts.force());

		File readme = new File(workspaceDir, "README.md");
		if (readme.isFile())
			IO.copy(readme, bnd.out);

			bnd.out.printf("%nWorkspace %s created.%n%n" //
					+ " Start Eclipse:%n" //
					+ "   1) Select the Eclipse workspace %s%n" //
					+ "   2) Package Explorer context menu: Import/General/Existing Projects from %s%n" + "%n" + "", //
					workspaceDir.getName(), eclipseDir, workspaceDir);

	}

	private void copy(File workspaceDir, InputStream in, Pattern glob, boolean overwrite) throws Exception {

		Jar jar = new Jar("dot", in);
		try {
			for (Entry<String,Resource> e : jar.getResources().entrySet()) {

				String path = e.getKey();

				if (glob != null && !glob.matcher(path).matches())
					continue;

				Resource r = e.getValue();
				File dest = Processor.getFile(workspaceDir, path);
				if (overwrite || !dest.isFile() || dest.lastModified() < r.lastModified() || r.lastModified() <= 0) {

					bnd.trace("copy %s to %s", path, dest);

					File dp = dest.getParentFile();
					if (!dp.exists() && !dp.mkdirs()) {
						throw new IOException("Could not create directory " + dp);
					}

					IO.copy(r.openInputStream(), dest);
				}
			}
		}
		finally {
			jar.close();
		}
	}

}
