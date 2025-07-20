package aQute.bnd.main;

import static aQute.bnd.wstemplates.FragmentTemplateEngine.DEFAULT_INDEX;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

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
		String indexUrl = DEFAULT_INDEX;
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
			String indexUrl = DEFAULT_INDEX;
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

	private void showTemplatesFragments(List<TemplateInfo> availableTemplates) {
		availableTemplates.forEach(ti -> bnd.out.format("%s - %s%n  - Organisation: %s%n  - Repo: %s %n", ti.name(),
			ti.description(), ti.id()
				.organisation(),
			ti.id()
				.repoUrl()));
	}

	private void installTemplateFragment(Workspace ws, List<String> selectedFragmentNames, String index)
		throws MalformedURLException {
		FragmentTemplateEngine engine = initFragmentTemplateEngine(ws, index);
		List<TemplateInfo> availableTemplates = engine.getAvailableTemplates();

		if (selectedFragmentNames == null || selectedFragmentNames.isEmpty()) {
			bnd.out.format("Available template fragments:%n");
			showTemplatesFragments(availableTemplates);
			return;
		}
		List<TemplateInfo> selectedTemplates = availableTemplates.stream()
			.filter(ti -> {
				return selectedFragmentNames.contains(ti.name());
			})
			.toList();

		if (selectedTemplates.isEmpty()) {
			// show templates
			return;
		}

		List<TemplateInfo> thirdParty = selectedTemplates.stream()
			.filter(t -> !t.isOfficial())
			.toList();

		if (!thirdParty.isEmpty()) {
			bnd.out.format("You have selected " + thirdParty.size() + " fragments from 3rd-party authors: %n");
			showTemplatesFragments(thirdParty);

			boolean confirmed = showConfirmation(
				"Are you sure you trust the authors and want to continue fetching the content?");

			if (!confirmed) {
				// not confirmed. cancel selected
				bnd.out.format(
					"%nNo template fragments installed with workspace, because user did not confirm the installation.");
				return;
			}

		}


		engine.updater(ws.getBase(), selectedTemplates)
			.commit();

		bnd.out.format("Added %s template fragment(s): %n", selectedTemplates.size());
		showTemplatesFragments(selectedTemplates);
	}

	private boolean showConfirmation(String question) {
		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				bnd.out.format("%n%s (yes/no): ", question);
				String input = scanner.nextLine()
					.trim()
					.toLowerCase();

				switch (input) {
					case "yes" :
					case "y" :
						return true;
					case "no" :
					case "n" :
						return false;
					default :
						System.out.println("Please enter '(y)es' or '(n)o'.");
				}
			}
		}
	}
}
