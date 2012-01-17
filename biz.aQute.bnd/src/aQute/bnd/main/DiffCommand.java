package aQute.bnd.main;

import java.io.*;
import java.util.*;

import aQute.bnd.differ.*;
import aQute.bnd.service.diff.*;
import aQute.configurable.*;
import aQute.lib.getopt.*;
import aQute.lib.osgi.*;
import aQute.lib.tag.*;

public class DiffCommand {

	interface diff extends IGetOpt {
		@Config(description = "Print the API") boolean api();

		@Config(description = "Print the Resources") boolean resources();

		@Config(description = "Print the Manifest") boolean manifest();

		@Config(description = "Show full tree") boolean full();

		@Config(description = "Print difference as valid XML") boolean xml();

		@Config(description = "Where to send the output") File output();

		@Config(description = "Limit to these packages") Collection<String> pack();
	}

	public static void diff(bnd bnd, String[] args, int first, PrintStream out) throws Exception {

		diff options = GetOpt.getopt(args, first, diff.class);

		if (options.help() != null) {
			System.out.println(GetOpt.getHelp(diff.class));
			return;
		}

		if (options._().size() == 1) {
			bnd.trace("Show tree");
			showTree(bnd,options);
			return;
		}
		if (options._().size() != 2) {
			throw new IllegalArgumentException("Requires 2 jar files input");
		}
		File fout = options.output();
		PrintWriter pw;
		if (fout == null)
			pw = new PrintWriter(bnd.out);
		else
			pw = new PrintWriter(fout);

		Instructions packageFilters = new Instructions(options.pack());

		Iterator<String> it = options._().iterator();
		Jar newer = new Jar(bnd.getFile(it.next()));
		try {
			Jar older = new Jar(bnd.getFile(it.next()));
			try {
				Differ di = new DiffPluginImpl();
				Tree n = di.tree(newer);
				Tree o = di.tree(older);
				Diff diff = n.diff(o);

				boolean all = options.api() == false && options.resources() == false
						&& options.manifest() == false;
				if (!options.xml()) {
					if (all || options.api())
						for (Diff packageDiff : diff.get("<api>").getChildren()) {
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

					pw.println("<?xml version='1.0'?>");
					tag.print(0, pw);
				}
				pw.close();
			} finally {
				older.close();
			}
		} finally {
			newer.close();
		}
	}

	/**
	 * Just show the single tree
	 * 
	 * @param bnd
	 * @param options
	 * @throws Exception
	 */
	
	private static void showTree(bnd bnd, diff options) throws Exception {
		File fout = options.output();
		PrintWriter pw;
		if (fout == null)
			pw = new PrintWriter(bnd.out);
		else
			pw = new PrintWriter(fout);

		Instructions packageFilters = new Instructions(options.pack());

		Jar newer = new Jar(bnd.getFile(options._().get(0)));
		try {
			Differ di = new DiffPluginImpl();
			Tree n = di.tree(newer);

			boolean all = options.api() == false && options.resources() == false
					&& options.manifest() == false;

			if (all || options.api())
				for (Tree packageDiff : n.get("<api>").getChildren()) {
					if (packageFilters.matches(packageDiff.getName()))
						show(pw, packageDiff, 0, !options.full());
				}
			if (all || options.manifest())
				show(pw, n.get("<manifest>"), 0, !options.full());
			if (all || options.resources())
				show(pw, n.get("<resources>"), 0, !options.full());
			pw.close();
		} finally {
			newer.close();
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

	private static void show(PrintWriter out, Diff diff, int indent, boolean limited) {
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

	private static void show(PrintWriter out, Tree tree, int indent, boolean limited) {
		for (int i = 0; i < indent; i++)
			out.print(" ");

		out.println(tree.toString());

		for (Tree c : tree.getChildren()) {
			show(out, c, indent + 1, limited);
		}
	}

}
