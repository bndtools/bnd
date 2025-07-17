package aQute.bnd.main;

import java.io.File;
import java.util.List;

import aQute.bnd.build.Workspace;
import aQute.lib.collections.ExtList;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;

public class AddCommands {

	private bnd			bnd;

	@Description("Create a project")
	@Arguments(arg = {
		"name", "..."
	})
	interface AddProjectOptions extends Options {}

	public AddCommands(bnd bnd) {
		this.bnd = bnd;
	}

	@Description("Create a project")
	public void _project(AddProjectOptions options) throws Exception {
		List<String> args = options._arguments();

		Workspace ws = Workspace.findWorkspace(bnd.getBase());
		if (ws == null) {
			bnd.error("No workspace found from %s", bnd.getBase());
			return;
		}

		for (String pname : args) {
			ws.createProject(pname);
		}

		bnd.getInfo(ws);
		return;

	}

	@Description("Create a workspace")
	@Arguments(arg = {
		"name", "..."
	})
	interface AddWorkspaceOptions extends Options {}

	@Description("Create a workspace")
	public void _workspace(AddWorkspaceOptions options) throws Exception {
		List<String> args = options._arguments();
		Workspace ws = null;
		for (String pname : args) {
			File wsdir = bnd.getFile(pname);
			ws = Workspace.createWorkspace(wsdir);
			if (ws == null) {
				bnd.error("Could not create workspace");
			}
		}
		bnd.getInfo(ws);
		return;
	}

	@Arguments(arg = {
		"[name]", "..."
	})
	@Description("Add a plugin")
	interface PluginAddOptions extends Options {
		String alias();

		boolean force();

		// boolean interactive();

		// String jar();
	}

	@Description("Add a plugin")
	public void _plugin(PluginAddOptions options) throws Exception {
		List<String> args = options._arguments();

		Workspace ws = bnd.getWorkspace(bnd.getBase());
		if (ws == null) {
			bnd.error("No workspace found from %s", bnd.getBase());
			return;
		}

		CommandLine cl = new CommandLine(bnd);
		String help = cl.execute(new Plugins(bnd, ws), "add", new ExtList<>(args));
		if (help != null)
			bnd.out.println(help);
		bnd.getInfo(ws);
		return;

	}

}
