package aQute.bnd.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.lib.collections.MultiMap;
import aQute.lib.collections.SortedList;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.strings.Strings;
import aQute.libg.generics.Create;

public class XRefCommand {
	private final bnd bnd;

	@Description("Show a cross references for all classes in a set of jars.")
	@Arguments(arg = {
		"<jar path>", "[...]"
	})
	interface xrefOptions extends Options {
		@Description("Show classes instead of packages")
		boolean classes();

		@Description("Show references to other classes/packages (>)")
		boolean to();

		@Description("Show references from other classes/packages (<)")
		boolean from();

		@Description("Match source types")
		List<String> source();

		@Description("Match destination types")
		List<String> destination();

		@Description("Filter for class names, a globbing expression")
		List<String> match();

		@Description("Output list of package/class names that have been referred to")
		String referrredTo();

	}

	static public class All {
		public Map<TypeRef, List<TypeRef>>			classes		= new HashMap<>();
		public Map<PackageRef, List<PackageRef>>	packages	= new HashMap<>();
	}

	XRefCommand(aQute.bnd.main.bnd bnd) {
		this.bnd = bnd;
	}

	void xref(xrefOptions options) throws FileNotFoundException {
		Analyzer analyzer = new Analyzer();
		final MultiMap<TypeRef, TypeRef> table = new MultiMap<>();
		final MultiMap<PackageRef, PackageRef> packages = new MultiMap<>();
		Set<TypeRef> set = Create.set();

		Instructions filter = new Instructions(options.match());
		Instructions source = new Instructions(options.source());
		Instructions destination = new Instructions(options.destination());
		bnd.getLogger()
			.info(" sources {}", source);

		for (String arg : options._arguments()) {
			try {
				File file = bnd.getFile(arg);
				try (Jar jar = new Jar(file.getName(), file)) {
					for (Map.Entry<String, Resource> entry : jar.getResources()
						.entrySet()) {
						String key = entry.getKey();
						Resource r = entry.getValue();
						if (key.endsWith(".class")) {
							TypeRef ref = analyzer.getTypeRefFromPath(key);
							String fqn = ref.getFQN();

							if (filter.matches(fqn) && source.matches(fqn)) {
								bnd.getLogger()
									.info("# include {}", fqn);
								set.add(ref);

								try (InputStream in = r.openInputStream()) {
									Clazz clazz = new Clazz(analyzer, key, r);

									// TODO use the proper bcp instead
									// of using the default layout

									Set<TypeRef> s = clazz.parseClassFile();
									for (Iterator<TypeRef> t = s.iterator(); t.hasNext();) {
										TypeRef tr = t.next();
										while (tr.isArray())
											tr = tr.getComponentTypeRef();

										if (!destination.matches(tr.getFQN()) || tr.isJava() || tr.isPrimitive())
											t.remove();
										else {
											packages.add(ref.getPackageRef(), tr.getPackageRef());
										}
									}
									if (!s.isEmpty()) {
										table.addAll(ref, s);
										set.addAll(s);
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		boolean to = options.to();
		boolean from = options.from();
		if (to == false && from == false && !"--".equals(options.referrredTo()))
			to = from = true;

		if (options.classes()) {
			if (options.referrredTo() != null) {
				printReferred(options.referrredTo(), Strings.join("\n", flatten(table)));
			}

			if (to)
				printxref(table, ">");
			if (from)
				printxref(table.transpose(), "<");
		} else {
			if (options.referrredTo() != null) {
				printReferred(options.referrredTo(), Strings.join("\n", flatten(packages)));
			}
			if (to)
				printxref(packages, ">");
			if (from)
				printxref(packages.transpose(), "<");
		}
	}

	private <T> Set<T> flatten(final MultiMap<T, T> packages) {
		return packages.values()
			.stream()
			.flatMap(List::stream)
			.collect(Collectors.toCollection(TreeSet::new));
	}

	private void printReferred(String referrredTo, String joined) throws FileNotFoundException {
		PrintStream out = bnd.out;
		boolean toConsole = referrredTo.equals("--");
		if (!toConsole) {
			File file = bnd.getFile(referrredTo);
			if (!file.getParentFile()
				.mkdirs()) {
				bnd.error("Cannot make parent directory for referred output file %s", referrredTo);
			}
			out = new PrintStream(file);
		}
		out.println(joined);
		if (!toConsole)
			out.close();
	}

	private void printxref(MultiMap<?, ?> map, String direction) {
		SortedList<?> labels = new SortedList<Object>(map.keySet(), null);
		for (Object element : labels) {
			List<?> e = map.get(element);
			if (e == null) {
				// ignore
			} else {
				Set<Object> set = new LinkedHashSet<>(e);
				set.remove(element);
				Iterator<?> row = set.iterator();
				String first = "";
				if (row.hasNext())
					first = row.next()
						.toString();
				bnd.out.printf("%50s %s %s\n", element, direction, first);
				while (row.hasNext()) {
					bnd.out.printf("%50s   %s\n", "", row.next());
				}
			}
		}
	}

}
