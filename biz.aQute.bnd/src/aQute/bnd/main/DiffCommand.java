package aQute.bnd.main;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Differ;
import aQute.bnd.service.diff.Tree;
import aQute.configurable.Config;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.tag.Tag;

public class DiffCommand {
	private final static Logger	logger	= LoggerFactory.getLogger(DiffCommand.class);
	bnd							bnd;

	DiffCommand(bnd bnd) {
		this.bnd = bnd;
	}

	@Description("Compares two jars. Without specifying the JARs (and when there is a "
		+ "current project) the jars of this project are diffed against their "
		+ "baseline in the baseline repository, using the sub-builder's options (these can be overridden). "
		+ "If one JAR is given, the tree is shown. Otherwise 2 JARs must be specified and they are "
		+ "then compared to eachother.")
	@Arguments(arg = {
		"[newer file]", "[older file]"
	})
	interface diffOptions extends Options {
		@Config(description = "Print the API")
		boolean api();

		@Config(description = "Print the Resources")
		boolean resources();

		@Config(description = "Print the Manifest")
		boolean manifest();

		@Config(description = "Show full tree")
		boolean full();

		@Config(description = "Print difference as valid XML")
		boolean xml();

		@Config(description = "Where to send the output")
		File output();

		@Config(description = "Limit to these packages (can have wildcards)")
		Collection<String> pack();

		@Config(description = "Ignore headers")
		Collection<String> ignore();
	}

	public void diff(diffOptions options) throws Exception {
		DiffPluginImpl di = new DiffPluginImpl();

		List<String> args = options._arguments();
		if (args.isEmpty()) {
			Project project = bnd.getProject();
			if (project != null) {
				try (ProjectBuilder projectBuilder = project.getBuilder(null)) {
					for (Builder b : projectBuilder.getSubBuilders()) {
						ProjectBuilder pb = (ProjectBuilder) b;
						Jar older = pb.getBaselineJar(); // make sure remains
															// before
						// disabling baselining
						pb.setProperty(Constants.BASELINE, ""); // do not do
						// baselining in
						// build
						Jar newer = pb.build();
						di.setIgnore(pb.getProperty(Constants.DIFFIGNORE));
						diff(options, di, newer, older);
						bnd.getInfo(b);
					}
				}
				bnd.getInfo(project);
				return;
			}

		} else if (options._arguments()
			.size() == 1) {
			logger.debug("Show tree");
			showTree(bnd, options);
			return;
		}

		if (options._arguments()
			.size() != 2) {
			throw new IllegalArgumentException("Requires 2 jar files input");
		}

		Jar newer = bnd.getJar(args.get(0));
		Jar older = bnd.getJar(args.get(1));
		diff(options, di, newer, older);
	}

	private void diff(diffOptions options, DiffPluginImpl di, Jar newer, Jar older) throws Exception {
		if (newer == null) {
			bnd.error("No newer file specified");
			return;
		}

		if (older == null) {
			bnd.error("No older file specified");
			return;
		}

		PrintWriter pw = null;
		try {
			File fout = options.output();

			if (fout == null)
				pw = IO.writer(bnd.out);
			else
				pw = IO.writer(fout, UTF_8);

			Instructions packageFilters = new Instructions(options.pack());

			if (options.ignore() != null)
				di.setIgnore(Processor.join(options.ignore()));

			Tree n = di.tree(newer);
			Tree o = di.tree(older);
			Diff diff = n.diff(o);

			boolean all = options.api() == false && options.resources() == false && options.manifest() == false;
			if (!options.xml()) {
				if (all || options.api())
					for (Diff packageDiff : diff.get("<api>")
						.getChildren()) {
						if (packageFilters.matches(packageDiff.getName()))
							show(pw, packageDiff, 0, !options.full());
					}
				if (all || options.manifest())
					show(pw, diff.get("<manifest>"), 0, !options.full());
				if (all || options.resources())
					show(pw, diff.get("<resources>"), 0, !options.full());
			} else {
				Tag tag = new Tag("diff");
				tag.addAttribute("date", new Date());
				tag.addContent(getTagFrom("newer", newer));
				tag.addContent(getTagFrom("older", older));
				if (all || options.api())
					tag.addContent(getTagFrom(diff.get("<api>"), !options.full()));
				if (all || options.manifest())
					tag.addContent(getTagFrom(diff.get("<manifest>"), !options.full()));
				if (all || options.resources())
					tag.addContent(getTagFrom(diff.get("<resources>"), !options.full()));

				pw.print("<?xml version='1.0' encoding='UTF-8'?>\n");
				tag.print(0, pw);
			}
		} finally {
			if (older != null) {
				older.close();
			}
			if (newer != null) {
				newer.close();
			}
			if (pw != null) {
				pw.close();
			}
		}
	}

	/**
	 * Just show the single tree
	 *
	 * @param bnd
	 * @param options
	 * @throws Exception
	 */

	private static void showTree(bnd bnd, diffOptions options) throws Exception {
		File fout = options.output();
		PrintWriter pw;
		if (fout == null)
			pw = IO.writer(bnd.out);
		else
			pw = IO.writer(fout, UTF_8);

		Instructions packageFilters = new Instructions(options.pack());

		try (Jar newer = new Jar(bnd.getFile(options._arguments()
			.get(0)))) {
			Differ di = new DiffPluginImpl();
			Tree n = di.tree(newer);

			boolean all = options.api() == false && options.resources() == false && options.manifest() == false;

			if (all || options.api())
				for (Tree packageDiff : n.get("<api>")
					.getChildren()) {
					if (packageFilters.matches(packageDiff.getName()))
						show(pw, packageDiff, 0);
				}
			if (all || options.manifest())
				show(pw, n.get("<manifest>"), 0);
			if (all || options.resources())
				show(pw, n.get("<resources>"), 0);
		} finally {
			pw.close();
		}

	}

	private static Tag getTagFrom(Diff diff, boolean limited) {
		if (limited && (diff.getDelta() == Delta.UNCHANGED || diff.getDelta() == Delta.IGNORED))
			return null;

		Tag tag = new Tag("diff");
		tag.addAttribute("name", diff.getName());
		tag.addAttribute("delta", diff.getDelta());
		tag.addAttribute("type", diff.getType());

		if (limited && (diff.getDelta() == Delta.ADDED || diff.getDelta() == Delta.REMOVED))
			return tag;

		for (Diff c : diff.getChildren()) {
			Tag child = getTagFrom(c, limited);
			if (child != null)
				tag.addContent(child);
		}
		return tag;
	}

	private static Tag getTagFrom(String name, Jar jar) throws Exception {
		Tag tag = new Tag(name);
		tag.addAttribute("bsn", jar.getBsn());
		tag.addAttribute("name", jar.getName());
		tag.addAttribute("version", jar.getVersion());
		tag.addAttribute("lastmodified", jar.lastModified());
		return tag;
	}

	public static void show(PrintWriter out, Diff diff, int indent, boolean limited) {
		if (limited && (diff.getDelta() == Delta.UNCHANGED || diff.getDelta() == Delta.IGNORED))
			return;

		for (int i = 0; i < indent; i++)
			out.print(" ");

		out.println(diff.toString());

		if (limited && (diff.getDelta() == Delta.ADDED || diff.getDelta() == Delta.REMOVED))
			return;

		for (Diff c : diff.getChildren()) {
			show(out, c, indent + 1, limited);
		}
	}

	public static void show(PrintWriter out, Tree tree, int indent) {
		for (int i = 0; i < indent; i++)
			out.print(" ");

		out.println(tree.toString());

		for (Tree c : tree.getChildren()) {
			show(out, c, indent + 1);
		}
	}

}
