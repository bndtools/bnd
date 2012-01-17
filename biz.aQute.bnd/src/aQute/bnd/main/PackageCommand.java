package aQute.bnd.main;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import aQute.bnd.differ.*;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.service.diff.*;
import aQute.lib.collections.*;
import aQute.lib.getopt.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Descriptors.PackageRef;
import aQute.lib.tag.*;
import aQute.libg.header.*;
import aQute.libg.version.*;

/**
 * Implements commands to maintain the Package versions db.
 */
public class PackageCommand {
	static TransformerFactory	transformerFactory	= TransformerFactory.newInstance();
	final bnd					bnd;
	final Baseline				baseline;
	final DiffPluginImpl		differ				= new DiffPluginImpl();
	final Collection<String>	SKIP_HEADERS		= Arrays.asList("Created-By",
															Constants.BND_LASTMODIFIED,
															Constants.BUNDLE_MANIFESTVERSION,
															"Manifest-Version", "Tool");

	PackageCommand(bnd bnd) throws IOException {
		this.bnd = bnd;
		this.baseline = new Baseline(bnd, differ);
	}

	interface BaseLineOptions extends IGetOpt {
		String fixup();
	}

	/**
	 * Compare
	 */

	public void _baseline(BaseLineOptions options) throws Exception {

		List<String> args = options._();
		if (args.size() != 2) {
			throw new IllegalArgumentException("Accepts only two argument (<jar>)");
		}
		PrintStream out = null;

		if (options.fixup() != null) {
			FileOutputStream fout;
			File o = bnd.getFile(options.fixup());
			fout = new FileOutputStream(o);
			out = new PrintStream(fout);
		}

		File newer = bnd.getFile(args.get(0));
		if (!newer.isFile())
			throw new IllegalArgumentException("Not a valid newer input file: " + newer);

		File older = bnd.getFile(args.get(1));
		if (!older.isFile())
			throw new IllegalArgumentException("Not a valid older input file: " + older);

		Jar nj = new Jar(newer);
		Jar oj = new Jar(older);
		Set<Info> infos = baseline.baseline(nj, oj, null);
		Info[] sorted = infos.toArray(new Info[infos.size()]);
		Arrays.sort(sorted, new Comparator<Info>() {
			public int compare(Info o1, Info o2) {
				return o1.packageName.compareTo(o2.packageName);
			}
		});

		bnd.out.printf("  %-50s %-10s %-10s %-10s %-10s %-10s\n", "Package", "Delta", "New", "Old",
				"Suggest", "If Prov.");
		for (Info info : sorted) {
			bnd.out.printf("%s %-50s %-10s %-10s %-10s %-10s %-10s\n", info.mismatch ? '*' : ' ',
					info.packageName, info.packageDiff.getDelta(), info.newerVersion,
					info.olderVersion, info.suggestedVersion,
					info.suggestedIfProviders == null ? "-" : info.suggestedIfProviders);
		}

		if (out != null) {
			// Create a fixup file

			Manifest manifest = nj.getManifest();
			if (manifest == null)
				manifest = new Manifest();

			for (Map.Entry<Object, Object> e : manifest.getMainAttributes().entrySet()) {
				String key = e.getKey().toString();

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

	/**
	 * Print out the packages from spec jars and check in which ees they appear.
	 * 
	 * Example
	 * 
	 * <pre>
	 * package overview -ee j2se-1.6.0 -ee j2se-1.5.0 -ee j2ee-1.4.0 javax.activation-1.1.jar
	 * </pre>
	 * 
	 */
	interface overview extends IGetOpt {
		Collection<Instruction> filter();

		String output(String deflt);

		String xsl();
	}

	class PSpec implements Comparable<PSpec> {
		String			packageName;
		Version			version;
		int				id;
		public Attrs	attrs;
		public Tree		tree;
		public Attrs	uses	= new Attrs();

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
	 *    bnd package schema <file.jar>*
	 * </pre>
	 * 
	 * @param ov
	 * @throws Exception
	 */
	public void _schema(overview ov) throws Exception {
		MultiMap<String, PSpec> map = new MultiMap<String, PSpec>();

		Tag top = new Tag("jschema");
		int n = 1000;
		for (String spec : ov._()) {
			File f = bnd.getFile(spec);
			if (!f.isFile()) {
				bnd.error("No such file: %s", f);
			} else {

				// For each specification jar we found

				bnd.trace("spec %s", f);
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

				Parameters exports = OSGiHeader.parseHeader(m.getMainAttributes().getValue(
						Constants.EXPORT_PACKAGE));

				// Create a map with versions. Ensure import ranges overwrite
				// the
				// exported versions
				Parameters versions = new Parameters();
				versions.putAll(exports);
				versions.putAll(OSGiHeader.parseHeader(m.getMainAttributes().getValue(
						Constants.IMPORT_PACKAGE)));

				Analyzer analyzer = new Analyzer();
				analyzer.setJar(jar);
				analyzer.analyze();

				Tree tree = differ.tree(analyzer);

				for (Entry<String, Attrs> entry : exports.entrySet()) {

					// For each exported package in the specification JAR

					Attrs attrs = entry.getValue();
					String packageName = entry.getKey();
					String version = attrs.get(Constants.VERSION_ATTRIBUTE);

					PSpec pspec = new PSpec();
					pspec.packageName = packageName;
					pspec.version = new Version(version);
					pspec.id = n;
					pspec.attrs = attrs;
					pspec.tree = tree;

					Collection<PackageRef> uses = analyzer.getUses().get(packageName);
					if (uses != null) {
						for (PackageRef x : uses) {
							if (x.isJava())
								continue;

							String imp = x.getFQN();

							if (imp.equals(packageName))
								continue;
							String v = null;
							if (versions.containsKey(imp))
								v = versions.get(imp).get(Constants.VERSION_ATTRIBUTE);
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

		SortedList<String> names = new SortedList<String>(map.keySet());

		Tag packagesTag = new Tag(top, "packages");
		Tag baselineTag = new Tag(top, "baseline");

		for (String pname : names) {

			// For each distinct package name

			SortedList<PSpec> specs = new SortedList<PSpec>(map.get(pname));

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

					bnd.trace(" newer=%s older=%s", newerExport, olderExport);

					Set<Info> infos = baseline.baseline(newer.tree, newerExport, older.tree,
							olderExport, new Instructions(pname));

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

		String o = ov.output("schema.xml");
		File of = bnd.getFile(o);
		of.getParentFile().mkdirs();
		FileWriter fw = new FileWriter(of);
		try {
			PrintWriter pw = new PrintWriter(fw);
			try {
				pw.println("<?xml version='1.0'?>");
				top.print(0, pw);
			} finally {
				pw.close();
			}
		} finally {
			fw.close();
		}

		if (ov.xsl() != null) {
			URL home = bnd.getBase().toURI().toURL();
			URL xslt = new URL(home, ov.xsl());
			String path = of.getAbsolutePath();
			if (path.endsWith(".xml"))
				path = path.substring(0, path.length() - 4);

			path = path + ".html";
			File html = new File(path);
			bnd.trace("xslt %s %s %s %s", xslt, of, html, html.exists());
			FileOutputStream out = new FileOutputStream(html);
			try {
				Transformer transformer = transformerFactory.newTransformer(new StreamSource(xslt
						.openStream()));
				transformer.transform(new StreamSource(of), new StreamResult(out));
			} finally {
				out.close();
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
			String s = sb.toString().trim();
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
		sb.append(diff.toString().trim().replace('\n', ' '));
		sb.append("\n");
		if (diff.getDelta() == Delta.ADDED || diff.getDelta() == Delta.REMOVED)
			return;

		for (Diff child : diff.getChildren()) {
			if (child.getDelta() != Delta.UNCHANGED && child.getDelta() != Delta.IGNORED)
				traverseTag(sb, child, indent + " ");
		}
	}

	/**
	 * @param exports
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
				if (clause.getKey().equals(Constants.USES_DIRECTIVE))
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
