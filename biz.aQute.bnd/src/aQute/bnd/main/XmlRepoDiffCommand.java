package aQute.bnd.main;

import static aQute.bnd.service.diff.Delta.IGNORED;
import static aQute.bnd.service.diff.Delta.UNCHANGED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import aQute.bnd.differ.XmlRepoDiffer;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Diff.Ignore;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.diff.Type;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.libg.glob.Glob;

public class XmlRepoDiffCommand extends Processor {

	private bnd bnd;

	@Arguments(arg = {
		"newer XML resource repository", "older XML resource repository"
	})
	@Description("Compares two XML resource repositories")
	public interface XmlRepoDiffOptions extends Options {
		@Description("Display all (changed and unchanged both)")
		boolean showall();

		@Description("Ignore elements from the comparison result (Format: type=name,..) e.g. "
			+ "RESOURCE_ID=org.apache.felix.scr#com.company.runtime.*,"
			+ "CAPABILITY=bnd.workspace.project#osgi.wiring.package:javax.xml.*,"
			+ "ATTRIBUTE=bundle-symbolic-name:system.bundle,REQUIREMENT=osgi.wiring.package:org.xml.*")
		String ignore();

		@Description("Expand 'filter' directives")
		boolean expandfilter();
	}

	public XmlRepoDiffCommand(bnd bnd) {
		this.bnd = bnd;
	}

	public void diff(XmlRepoDiffOptions options) throws Exception {
		List<String> arguments = options._arguments();

		if (arguments.size() != 2) {
			bnd.messages.Failed__(new IllegalArgumentException(), "Two file arguments are required");
			return;
		}

		File newer = bnd.getFile(arguments.remove(0));
		File older = bnd.getFile(arguments.remove(0));

		if (!newer.exists() || !newer.isFile()) {
			bnd.messages.NoSuchFile_(newer);
			return;
		}
		if (!older.exists() || !older.isFile()) {
			bnd.messages.NoSuchFile_(older);
			return;
		}

		Tree treeNewer = XmlRepoDiffer.resource(newer, options.expandfilter());
		Tree treeOlder = XmlRepoDiffer.resource(older, options.expandfilter());

		Diff diff = treeNewer.diff(treeOlder);

		try (PrintWriter writer = IO.writer(bnd.out, UTF_8)) {
			show(writer, diff, 0, !options.showall(), parseIgnore(options.ignore()));
		}
	}

	private static Ignore parseIgnore(String ignore) {
		if (ignore != null) {
			Map<Type, List<Glob>> split = Stream.of(ignore.split(","))
				.map(String::trim)
				.map(s -> s.split("="))
				.collect(toMap(a -> Type.valueOf(a[0]), a -> parseIgnoreValue(a[1])));

			return diff -> {
				for (Entry<Type, List<Glob>> entry : split.entrySet()) {
					Type type = entry.getKey();
					List<Glob> names = entry.getValue();
					Optional<Glob> matched = names.stream()
						.filter(n -> n.matches(diff.getName()))
						.filter(n -> diff.getType() == type)
						.findAny();
					if (matched.isPresent()) {
						return true;
					}
				}
				return false;
			};
		} else {
			return diff -> false;
		}
	}

	private static List<Glob> parseIgnoreValue(String value) {
		return Stream.of(value.split("#"))
			.map(String::trim)
			.map(Glob::new)
			.collect(toList());
	}

	private static void show(PrintWriter out, Diff diff, int indent, boolean limited, Ignore ignore) {
		if (limited && (diff.getDelta(ignore) == UNCHANGED || diff.getDelta(ignore) == IGNORED))
			return;

		IntStream.range(0, indent)
			.forEach(e -> out.print("  "));

		out.println(diff);

		diff.getChildren()
			.forEach(c -> show(out, c, indent + 1, limited, ignore));
	}
}
