package aQute.bnd.main;

import java.util.List;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.collections.ExtList;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;

public class RemoveCommands {

	private bnd			bnd;

	@Description("Remove a project from workspace")
	@Arguments(arg = {
		"name", "..."
	})
	interface RemoveProjectOptions extends Options {}

	public RemoveCommands(bnd bnd) {
		this.bnd = bnd;
	}

	@Description("Remove a project from workspace")
	public void _project(RemoveProjectOptions options) throws Exception {

		Workspace ws = bnd.getWorkspace(bnd.getBase());
		if (ws == null) {
			bnd.error("No workspace found from %s", bnd.getBase());
			return;
		}

		List<String> args = options._arguments();

		for (String pname : args) {
			Project project = ws.getProject(pname);
			if (project == null) {
				bnd.error("No such project %s", pname);
			} else
				project.remove();
		}

		return;
	}

	@Description("Remove workspace")
	@Arguments(arg = {
		"name", "..."
	})
	interface RemoveWorkspaceOptions extends Options {}

	@Description("Remove workspace")
	public void _workspace(RemoveWorkspaceOptions options) throws Exception {
		bnd.error("To delete a workspace, delete the directory");
		return;
	}

	@Arguments(arg = "alias...")
	@Description("Remove a plugin from the workspace")
	interface PluginRemoveOptions extends Options {}


	@Description("Remove a plugin from the workspace")
	public void _plugin(PluginRemoveOptions options) throws Exception {
		List<String> args = options._arguments();

		Workspace ws = bnd.getWorkspace(bnd.getBase());
		if (ws == null) {
			bnd.error("No workspace found from %s", bnd.getBase());
			return;
		}

		CommandLine cl = new CommandLine(bnd);
		String help = cl.execute(new Plugins(bnd, ws), "remove", new ExtList<>(args));
		if (help != null)
			bnd.out.println(help);
		return;

	}

}
