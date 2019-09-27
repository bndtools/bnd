package aQute.bnd.main;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.version.Version;
import aQute.lib.collections.MultiMap;
import aQute.lib.collections.SortedList;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.tag.Tag;

/**
 * Implements commands to maintain the Package versions db.
 */
public class BaselineCommands {
	private final static Logger	logger				= LoggerFactory.getLogger(BaselineCommands.class);
	static TransformerFactory	transformerFactory	= TransformerFactory.newInstance();
	final bnd					bnd;
	final Baseline				baseline;
	final DiffPluginImpl		differ				= new DiffPluginImpl();
	final Collection<String>	SKIP_HEADERS		= Arrays.asList(Constants.CREATED_BY, Constants.BND_LASTMODIFIED,
		Constants.BUNDLE_MANIFESTVERSION, "Manifest-Version", Constants.TOOL);

	BaselineCommands(bnd bnd) throws IOException {
		this.bnd = bnd;
		this.baseline = new Baseline(bnd, differ);
	}

	@Description("Compare a newer bundle to a baselined bundle and provide versioning advice")
	@Arguments(arg = {
		"[newer jar]", "[older jar]"
	})
	interface baseLineOptions extends Options {
		@Description("Output file with fixup info")
		String fixup();

		@Description("Show any differences")
		boolean diff();

		@Description("Be quiet, only report errors")
		boolean quiet();

		@Description("Show all, also unchanged")
		boolean all();

		@Description("On changed, list API changes")
		boolean verbose();

		@Description("Packages to baseline (comma delimited)")
		String packages();
	}

	/**
	 * Compare
	 */

	@Description("Compare a newer bundle to a baselined bundle and provide versioning advice. If no parameters are given, and there "
		+ "is a local project, then we use the projects current build and the baseline jar in the release repo.")
	public void _baseline(baseLineOptions opts) throws Exception {

		List<String> args = opts._arguments();
		if (args.isEmpty()) {
			Project project = bnd.getProject();
			if (project != null) {
				try (ProjectBuilder parentBuilder = project.getBuilder(null)) {
					for (Builder b : parentBuilder.getSubBuilders()) {
						ProjectBuilder pb = (ProjectBuilder) b;
						try (Jar older = pb.getBaselineJar()) {
							if (older == null) {
								bnd.error("No baseline JAR available. Did you set " + Constants.BASELINE);
								return;
							}
							pb.setProperty(Constants.BASELINE, ""); // do not do
																	// baselining
																	// in
																	// build
							// make sure disabling is after getting the baseline
							// jar

							try (Jar newer = pb.build()) {
								differ.setIgnore(pb.getProperty(Constants.DIFFIGNORE));
								baseline(opts, newer, older,
									opts.packages() != null ? new Instructions(opts.packages()) : null);
								bnd.getInfo(b);
							}
						}
					}
				}
				bnd.getInfo(project);
				return;
			}
		}

		if (args.size() != 2) {
			throw new IllegalArgumentException("Accepts only two argument (<jar>)");
		}
		File newer = bnd.getFile(args.remove(0));
		if (!newer.isFile())
			throw new IllegalArgumentException("Not a valid newer input file: " + newer);

		File older = bnd.getFile(args.remove(0));
		if (!older.isFile())
			throw new IllegalArgumentException("Not a valid older input file: " + older);

		Jar nj = new Jar(newer);
		Jar oj = new Jar(older);
		baseline(opts, nj, oj, opts.packages() != null ? new Instructions(opts.packages()) : null);
	}

	private void baseline(baseLineOptions opts, Jar newer, Jar older, Instructions packages)
		throws FileNotFoundException, UnsupportedEncodingException, IOException, Exception {
		PrintStream out = null;

		if (opts.fixup() != null) {
			out = new PrintStream(bnd.getFile(opts.fixup()), "UTF-8");
		}

		Set<Info> infos = baseline.baseline(newer, older, packages);
		BundleInfo bundleInfo = baseline.getBundleInfo();

		Info[] sorted = infos.toArray(new Info[0]);
		Arrays.sort(sorted, (o1, o2) -> o1.packageName.compareTo(o2.packageName));

		if (!opts.quiet()) {
			bnd.out.printf("===============================================================%n%s %s %s-%s",
				bundleInfo.mismatch ? '*' : ' ', bundleInfo.bsn, newer.getVersion(), older.getVersion());
			if (bundleInfo.mismatch && bundleInfo.suggestedVersion != null)
				bnd.out.printf(" suggests %s", bundleInfo.suggestedVersion);

			bnd.out.printf("%n===============================================================%n");

			boolean hadHeader = false;
			for (Info info : sorted) {
				if (info.packageDiff.getDelta() != Delta.UNCHANGED || opts.all()) {
					if (!hadHeader) {
						bnd.out.printf("  %-50s %-10s %-10s %-10s %-10s %-10s%n", "Package", "Delta", "New", "Old",
							"Suggest", "If Prov.");
						hadHeader = true;
					}
					bnd.out.printf("%s %-50s %-10s %-10s %-10s %-10s %-10s%n", info.mismatch ? '*' : ' ',
						info.packageName, //
						info.packageDiff.getDelta(), //
						info.newerVersion, //
						info.olderVersion != null && info.olderVersion.equals(Version.LOWEST) ? "-" : info.olderVersion, //
						info.suggestedVersion != null && info.suggestedVersion.compareTo(info.newerVersion) <= 0 ? "ok"
							: info.suggestedVersion, //
						info.suggestedIfProviders == null ? "-" : info.suggestedIfProviders);

					if (info.packageDiff.getDelta() != Delta.UNCHANGED && opts.verbose()) {
						doPackageDiff(info.packageDiff);
					}
				}
			}
		}

		if (out != null) {
			// Create a fixup file

			Manifest manifest = newer.getManifest();
			if (manifest == null)
				manifest = new Manifest();

			for (Map.Entry<Object, Object> e : manifest.getMainAttributes()
				.entrySet()) {
				String key = e.getKey()
					.toString();

				if (!SKIP_HEADERS.contains(key)) {
					if (!Constants.EXPORT_PACKAGE.equals(key)) {
						out.printf("%-40s = ", key);
						String value = (String) e.getValue();
						out.append(value);
					}
					out.println();
				}
			}
			doExportPackage(sorted, out);
			out.close();
		}
	}

	protected void doPackageDiff(Diff diff) {
		StringBuilder sb = new StringBuilder();

		sb.append("\t");

		for (Diff curDiff : diff.getChildren()) {
			if (curDiff.getDelta() == Delta.UNCHANGED) {
				continue;
			}

			doDiff(curDiff, sb);
		}
	}

	protected void doDiff(Diff diff, StringBuilder sb) {
		String type = String.valueOf(diff.getType());

		String output = String.format("%s%-5s %-10s %s", sb, getShortDelta(diff.getDelta()), type.toLowerCase(),
			diff.getName());

		bnd.out.println(output);

		sb.append("\t");

		for (Diff curDiff : diff.getChildren()) {
			if (curDiff.getDelta() == Delta.UNCHANGED) {
				continue;
			}

			doDiff(curDiff, sb);
		}

		sb.deleteCharAt(sb.length() - 1);
	}

	protected String getShortDelta(Delta delta) {
		if (delta == Delta.ADDED) {
			return "+";
		} else if (delta == Delta.CHANGED) {
			return "~";
		} else if (delta == Delta.MAJOR) {
			return "MAJ";
		} else if (delta == Delta.MICRO) {
			return "MIC";
		} else if (delta == Delta.MINOR) {
			return "MIN";
		} else if (delta == Delta.REMOVED) {
			return "-";
		}

		String deltaString = delta.toString();

		return String.valueOf(deltaString.charAt(0));
	}

	/**
	 * Print out the packages from spec jars and check in which ees they appear.
	 * Example
	 *
	 * <pre>
	 *  package overview -ee j2se-1.6.0 -ee j2se-1.5.0 -ee
	 * j2ee-1.4.0 javax.activation-1.1.jar
	 * </pre>
	 */
	@Description("Print out the packages from spec jars and check in which ees they appear. Very specific. For example, schema ee.j2se-1.6.0 ee.j2se-1.5.0 ee.j2ee-1.4.0")
	interface schemaOptions extends Options {
		@Description("Output file")
		String output(String deflt);

		@Description("Specify an XSL file for pretty printing")
		String xsl();
	}

	class PSpec implements Comparable<PSpec> {
		String			packageName;
		Version			version;
		int				id;
		public Attrs	attrs;
		public Tree		tree;
		public Attrs	uses	= new Attrs();

		@Override
		public int compareTo(PSpec o) {
			return version.compareTo(o.version);
		}

	}

	/**
	 * Create a schema of a set of jars outling the packages and their versions.
	 * This will create a list of packages with multiple versions, link to their
	 * specifications, and the deltas between versions.
	 *
	 * <pre>
	 *  bnd package schema
	 * <file.jar>*
	 * </pre>
	 *
	 * @param opts
	 * @throws Exception
	 */
	public void _schema(schemaOptions opts) throws Exception {
		MultiMap<String, PSpec> map = new MultiMap<>();

		Tag top = new Tag("jschema");
		int n = 1000;
		for (String spec : opts._arguments()) {
			File f = bnd.getFile(spec);
			if (!f.isFile()) {
				bnd.messages.NoSuchFile_(f);
			} else {

				// For each specification jar we found

				logger.debug("spec {}", f);
				Jar jar = new Jar(f); // spec
				Manifest m = jar.getManifest();
				Attributes main = m.getMainAttributes();
				Tag specTag = new Tag(top, "specification");
				specTag.addAttribute("jar", spec);
				specTag.addAttribute("name", main.getValue("Specification-Name"));
				specTag.addAttribute("title", main.getValue("Specification-Title"));
				specTag.addAttribute("jsr", main.getValue("Specification-JSR"));
				specTag.addAttribute("url", main.getValue("Specification-URL"));
				specTag.addAttribute("version", main.getValue("Specification-Version"));
				specTag.addAttribute("vendor", main.getValue("Specification-Vendor"));
				specTag.addAttribute("id", n);
				specTag.addContent(main.getValue(Constants.BUNDLE_DESCRIPTION));

				Parameters exports = OSGiHeader.parseHeader(m.getMainAttributes()
					.getValue(Constants.EXPORT_PACKAGE));

				// Create a map with versions. Ensure import ranges overwrite
				// the
				// exported versions
				Parameters versions = new Parameters();
				versions.putAll(exports);
				versions.putAll(OSGiHeader.parseHeader(m.getMainAttributes()
					.getValue(Constants.IMPORT_PACKAGE)));

				Analyzer analyzer = new Analyzer();
				analyzer.setJar(jar);
				analyzer.analyze();

				Tree tree = differ.tree(analyzer);

				for (Entry<String, Attrs> entry : exports.entrySet()) {

					// For each exported package in the specification JAR

					Attrs attrs = entry.getValue();
					String packageName = entry.getKey();
					PackageRef packageRef = analyzer.getPackageRef(packageName);

					String version = attrs.get(Constants.VERSION_ATTRIBUTE);

					PSpec pspec = new PSpec();
					pspec.packageName = packageName;
					pspec.version = new Version(version);
					pspec.id = n;
					pspec.attrs = attrs;
					pspec.tree = tree;

					Collection<PackageRef> uses = analyzer.getUses()
						.get(packageRef);
					if (uses != null) {
						for (PackageRef x : uses) {
							if (x.isJava())
								continue;

							String imp = x.getFQN();

							if (imp.equals(packageName))
								continue;
							String v = null;
							if (versions.containsKey(imp))
								v = versions.get(imp)
									.get(Constants.VERSION_ATTRIBUTE);
							pspec.uses.put(imp, v);
						}
					}
					map.add(packageName, pspec);
				}
				jar.close();
				n++;
			}
		}

		// We now gather all the information about all packages in the map.
		// Next phase is generating the XML. Sorting the packages is
		// important because XSLT is brain dead.

		SortedList<String> names = new SortedList<>(map.keySet());

		Tag packagesTag = new Tag(top, "packages");
		Tag baselineTag = new Tag(top, "baseline");

		for (String pname : names) {

			// For each distinct package name

			SortedList<PSpec> specs = new SortedList<>(map.get(pname));

			PSpec older = null;
			Parameters olderExport = null;

			for (PSpec newer : specs) {

				// For each package in the total set

				Tag pack = new Tag(packagesTag, "package");
				pack.addAttribute("name", newer.packageName);
				pack.addAttribute("version", newer.version);
				pack.addAttribute("spec", newer.id);

				Parameters newerExport = new Parameters();
				newerExport.put(pname, newer.attrs);

				if (older != null) {
					String compareId = newer.packageName + "-" + newer.id + "-" + older.id;
					pack.addAttribute("delta", compareId);

					logger.debug(" newer={} older={}", newerExport, olderExport);

					Set<Info> infos = baseline.baseline(newer.tree, newerExport, older.tree, olderExport,
						new Instructions(pname));

					for (Info info : infos) {
						Tag tag = getTag(info);
						tag.addAttribute("id", compareId);
						tag.addAttribute("newerSpec", newer.id);
						tag.addAttribute("olderSpec", older.id);
						baselineTag.addContent(tag);
					}
					older.tree = null;
					older.attrs = null;
					older = newer;
				}

				//
				// XRef, show the used packages for this package
				//

				for (Entry<String, String> uses : newer.uses.entrySet()) {
					Tag reference = new Tag(pack, "import");
					reference.addAttribute("name", uses.getKey());
					reference.addAttribute("version", uses.getValue());
				}

				older = newer;
				olderExport = newerExport;
			}
		}

		String o = opts.output("schema.xml");
		File of = bnd.getFile(o);
		File pof = of.getParentFile();
		IO.mkdirs(pof);
		try (PrintWriter pw = IO.writer(of, UTF_8)) {
			pw.print("<?xml version='1.0' encoding='UTF-8'?>\n");
			top.print(0, pw);
		}

		if (opts.xsl() != null) {
			URL home = bnd.getBase()
				.toURI()
				.toURL();
			URL xslt = new URL(home, opts.xsl());
			String path = of.getAbsolutePath();
			if (path.endsWith(".xml"))
				path = path.substring(0, path.length() - 4);

			path = path + ".html";
			File html = new File(path);
			logger.debug("xslt {} {} {} {}", xslt, of, html, html.exists());
			try (OutputStream out = IO.outputStream(html); InputStream in = xslt.openStream()) {
				Transformer transformer = transformerFactory.newTransformer(new StreamSource(in));
				transformer.transform(new StreamSource(of), new StreamResult(out));
			}
		}
	}

	private Tag getTag(Info info) {
		Tag tag = new Tag("info");
		tag.addAttribute("name", info.packageName);
		tag.addAttribute("newerVersion", info.newerVersion);
		tag.addAttribute("olderVersion", info.olderVersion);
		tag.addAttribute("suggestedVersion", info.suggestedVersion);
		tag.addAttribute("suggestedIfProviders", info.suggestedIfProviders);
		tag.addAttribute("mismatch", info.mismatch);
		tag.addAttribute("warning", info.warning);
		StringBuilder sb = new StringBuilder();
		if (info.packageDiff.getDelta() == Delta.UNCHANGED)
			tag.addAttribute("equals", "true");
		else {
			traverseTag(sb, info.packageDiff, "");
			String s = sb.toString()
				.trim();
			if (s.length() != 0) {
				Tag d = new Tag(tag, "diff", s);
				d.setCDATA();
			}
		}
		if (info.providers != null)
			for (String provider : info.providers) {
				Tag p = new Tag(tag, "provider");
				p.addAttribute("provider", provider);
			}
		return tag;
	}

	private void traverseTag(StringBuilder sb, Diff diff, String indent) {
		sb.append(indent);
		sb.append(diff.toString()
			.trim()
			.replace('\n', ' '));
		sb.append("\n");
		if (diff.getDelta() == Delta.ADDED || diff.getDelta() == Delta.REMOVED)
			return;

		for (Diff child : diff.getChildren()) {
			if (child.getDelta() != Delta.UNCHANGED && child.getDelta() != Delta.IGNORED)
				traverseTag(sb, child, indent + " ");
		}
	}

	/**
	 * @param infos
	 * @param out
	 * @throws IOException
	 */
	public void doExportPackage(Info[] infos, PrintStream out) throws IOException {

		out.printf("# Suggested versions\n%-40s = ", Constants.EXPORT_PACKAGE);
		String del = "";
		for (Info info : infos) {
			out.append(del);
			out.printf("\\\n  ");
			out.append(info.packageName);
			info.attributes.put(Constants.VERSION_ATTRIBUTE, info.suggestedVersion.toString());
			for (Map.Entry<String, String> clause : info.attributes.entrySet()) {
				if (clause.getKey()
					.equals(Constants.USES_DIRECTIVE))
					continue;

				out.append(";\\\n    ");
				out.append(clause.getKey());
				out.append("=");
				Processor.quote(out, clause.getValue());
			}

			if (info.providers != null && !info.providers.isEmpty()) {
				out.append(";\\\n    " + Constants.PROVIDER_TYPE_DIRECTIVE + "=\"");
				String del2 = "";
				for (String part : info.providers) {
					out.append(del2);
					out.append("\\\n        ");
					out.append(part);
					del2 = ",";
				}
				out.append("\"");
			}
			del = ",";
		}
	}

}
