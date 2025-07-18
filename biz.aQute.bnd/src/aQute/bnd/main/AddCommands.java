package aQute.bnd.main;

import static aQute.bnd.wstemplates.FragmentTemplateEngine.DEFAULT_INDEX;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import aQute.bnd.build.Workspace;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.wstemplates.FragmentTemplateEngine;
import aQute.bnd.wstemplates.FragmentTemplateEngine.TemplateInfo;
import aQute.lib.collections.ExtList;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;

public class AddCommands {

	private bnd			bnd;

	@Description("Create a project in the current workspace")
	@Arguments(arg = {
		"name", "..."
	})
	interface AddProjectOptions extends Options {}

	public AddCommands(bnd bnd) {
		this.bnd = bnd;
	}

	@Description("Create a project in the current workspace")
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

	@Arguments(arg = {
		"[name]..."
	})
	@Description("Add template fragment(s) to the current workspace. Leave the name empty to see a list of available templates at the index."
		+ "\\n\\nExample:\\n bnd add fragment osgi gradle ")
	interface AddTemplateOptions extends Options {
		@Description("Optional: URL of an alternative template fragment index, for testing purposes. Default is: "
			+ DEFAULT_INDEX)
		String index();
	}

	@Description("Add template fragment(s) to the current workspace.")
	public void _fragment(AddTemplateOptions options) throws Exception {
		List<String> args = options._arguments();

		Workspace ws = bnd.getWorkspace(bnd.getBase());
		if (ws == null) {
			bnd.error("No workspace found from %s", bnd.getBase());
			return;
		}

		List<String> selectedFragmentNames = args;
		String indexUrl = options.index();
		installTemplateFragment(ws, selectedFragmentNames, indexUrl);

	}

	@Arguments(arg = {
		"name", "..."
	})
	@Description("Create a bnd workspace in the current folder. The workspace can also be inialized with a set of template fragments.\n\n"
		+ "Example:\n bnd add workspace -f osgi -f gradle 'myworkspace'"
		+ "\n\nSee https://bnd.bndtools.org/chapters/620-template-fragments.html for more information.")
	interface AddWorkspaceOptions extends Options {

		@Description("Specify template fragment(s) by name to install together with the created workspace. Fragments are identified by the 'name' attribute in the index. Specify multiple fragments by repeating the -f option. "
			+ "To see a list of available templates use 'bnd add fragment' without arguments.")
		List<String> fragment();

		@Description("Optional: URL of an alternative template fragment index, for testing purposes. Default is: "
			+ DEFAULT_INDEX)
		String index();
	}

	@Description("Create a bnd workspace in the current folder. The workspace can also be inialized with a set of template fragments.")
	public void _workspace(AddWorkspaceOptions options) throws Exception {


		List<String> args = options._arguments();
		Workspace ws = null;
		for (String pname : args) {
			File wsdir = bnd.getFile(pname);
			ws = Workspace.createWorkspace(wsdir);
			if (ws == null) {
				bnd.error("Could not create workspace: %s (Check if the folder exists already)", pname);
				return;
			}

			bnd.out.format("%nCreated workspace: %s%n", pname);
		}

		List<String> fragments = options.fragment();
		if (fragments != null && !fragments
			.isEmpty()) {
			String indexUrl = options.index();
			installTemplateFragment(ws, fragments, indexUrl);
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

	private void installTemplateFragment(Workspace ws, List<String> selectedFragmentNames, String index)
		throws MalformedURLException {
		FragmentTemplateEngine engine = initFragmentTemplateEngine(ws, index);
		List<TemplateInfo> availableTemplates = engine.getAvailableTemplates();

		if (selectedFragmentNames.isEmpty()) {
			// show templates
			showAvailableTemplates(availableTemplates);
			return;
		}

		List<TemplateInfo> selectedTemplates = availableTemplates.stream()
			.filter(ti -> {
				return selectedFragmentNames.contains(ti.name());
			})
			.toList();
		engine.updater(ws.getBase(), selectedTemplates)
			.commit();

		bnd.out.format("Added template fragment(s): %n");
		selectedTemplates.forEach(ti -> bnd.out.format("%s - %s (Organisation: %s Repo: %s) %n", ti.name(),
			ti.description(), ti.id()
				.organisation(),
			ti.id()
				.repoUrl()));
	}

	private void showAvailableTemplates(List<TemplateInfo> availableTemplates) {
		bnd.out.format("Available template fragments:%n");
		availableTemplates.forEach(ti -> bnd.out.format("%s - %s (Organisation: %s Repo: %s) %n", ti.name(),
			ti.description(), ti.id()
				.organisation(),
			ti.id()
				.repoUrl()));
	}

	private FragmentTemplateEngine initFragmentTemplateEngine(Workspace ws, String index) throws MalformedURLException {
		FragmentTemplateEngine engine = new FragmentTemplateEngine(ws);
		String indexUrl = Objects.toString(index, DEFAULT_INDEX);

		engine.read(new URL(indexUrl))
			.unwrap()
			.forEach(engine::add);
		Parameters p = ws.getMergedParameters(Constants.WORKSPACE_TEMPLATES);
		engine.read(p)
			.forEach(engine::add);
		return engine;
	}

}
