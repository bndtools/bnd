package aQute.bnd.print;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz.Def;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.stream.MapStream;
import aQute.lib.collections.MultiMap;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.bnd.unmodifiable.Sets;
import aQute.libg.generics.Create;

public class JarPrinter extends Processor {
	final Formatter						out				= new Formatter();

	final public static int				VERIFY			= 1;

	final public static int				MANIFEST		= 2;

	final public static int				LIST			= 4;

	final public static int				IMPEXP			= 16;
	final public static int				USES			= 32;
	final public static int				USEDBY			= 64;
	final public static int				COMPONENT		= 128;
	final public static int				METATYPE		= 256;
	final public static int				API				= 512;
	final public static int				CAPABILITIES	= 1024;
	static public final int				HEX				= 0;
	private static final Set<String>	syntax_headers	= Sets.of(OSGI_SYNTAX_HEADERS);

	public JarPrinter(Processor bnd) {
		super(bnd);
	}

	public JarPrinter() {}

	public JarPrinter doPrint(Jar jar, int options, boolean java, boolean xport)
		throws ZipException, IOException, Exception {
		if (jar.getManifest() != null) {
			if ((options & VERIFY) != 0) {
				doVerify(jar);
			}
			if ((options & MANIFEST) != 0) {
				header("MANIFEST");
				doManifest(jar);
			}
			if ((options & IMPEXP) != 0) {
				header("IMPEXP");
				doImpExp(jar);
				println();
			}
			if ((options & CAPABILITIES) != 0) {
				header("CAPABILITIES");
				doCapabilities(jar);
				println();
			}
			if ((options & COMPONENT) != 0) {
				header("COMPONENTS");
				doComponents(jar);
				println();
			}
			if ((options & METATYPE) != 0) {
				header("METATYPE");
				doMetatype(jar);
				println();
			}
			if ((options & (USES | USEDBY | API)) != 0) {
				doXref(jar, options, java, xport);
				println();
			}
		}
		if ((options & LIST) != 0) {
			header("LIST");
			doList(jar);
			println();
		}

		return this;
	}

	private void header(String string) {
		out.format("[%s]%n%n", string);
	}

	public void doXref(Jar jar, int options, boolean java, boolean xport) throws Exception, IOException {
		println();
		try (Analyzer analyzer = new Analyzer()) {
			analyzer.setPedantic(isPedantic());
			analyzer.setJar(jar);
			analyzer.removeClose(jar);

			Manifest m = jar.getManifest();
			if (m != null) {
				String s = m.getMainAttributes()
					.getValue(Constants.EXPORT_PACKAGE);
				if (s != null)
					analyzer.setExportPackage(s);
			}
			analyzer.analyze();

			Packages exports = analyzer.getExports();

			if ((options & API) != 0) {
				Map<PackageRef, List<PackageRef>> apiUses = analyzer.cleanupUses(analyzer.getAPIUses(), !java);
				if (!xport) {
					if (exports.isEmpty())
						warning("Not filtering on exported only since exports are empty");
					else
						apiUses.keySet()
							.retainAll(analyzer.getExports()
								.keySet());
				}
				header("API USES");
				MultiMap.format(apiUses);

				Set<PackageRef> privates = analyzer.getPrivates();
				for (PackageRef export : exports.keySet()) {
					Map<Def, List<TypeRef>> xRef = analyzer.getXRef(export, privates,
						Modifier.PROTECTED + Modifier.PUBLIC);
					if (!xRef.isEmpty()) {
						println();
						out.format("%s refers to private Packages (not good)\n\n", export);
						for (Entry<Def, List<TypeRef>> e : xRef.entrySet()) {
							TreeSet<PackageRef> refs = new TreeSet<>();
							for (TypeRef ref : e.getValue())
								refs.add(ref.getPackageRef());

							refs.retainAll(privates);
							out.format("%60s %-40s %s\n", e.getKey()
								.getOwnerType()
								.getFQN() //
								, e.getKey()
									.getName(),
								refs);
						}
						println();
					}
				}
				println();
			}

			Map<PackageRef, List<PackageRef>> uses = analyzer.cleanupUses(analyzer.getUses(), !java);
			if ((options & USES) != 0) {
				header("USES");
				println(MultiMap.format(uses));
				println();
			}
			if ((options & USEDBY) != 0) {
				header("USEDBY");
				MultiMap<PackageRef, PackageRef> usedBy = new MultiMap<>(uses).transpose();
				println(MultiMap.format(usedBy));
			}
		}
	}

	public void doList(Jar jar) {
		for (Map.Entry<String, Map<String, Resource>> entry : jar.getDirectories()
			.entrySet()) {
			String name = entry.getKey();
			Map<String, Resource> contents = entry.getValue();
			println(name);
			if (contents != null) {
				for (String element : contents.keySet()) {
					int n = element.lastIndexOf('/');
					if (n > 0)
						element = element.substring(n + 1);
					out.format("  %s", element);
					String path = element;
					if (name.length() != 0)
						path = name + "/" + element;
					Resource r = contents.get(path);
					if (r != null) {
						String extra = r.getExtra();
						if (extra != null) {
							out.format(" extra='%s'", Hex.toHexString(Resource.decodeExtra(extra)));
						}
					}
					println();
				}
			} else {
				println(name + " <no contents>");
			}
		}
		println();
	}

	public void doCapabilities(Jar jar) throws Exception {
		Manifest m = jar.getManifest();
		Domain domain = Domain.domain(m);

		if (m != null) {
			Parameters provide = domain.getProvideCapability();
			Parameters require = domain.getRequireCapability();
			print(Constants.PROVIDE_CAPABILITY, new TreeMap<>(provide));
			print(Constants.REQUIRE_CAPABILITY, new TreeMap<>(require));
		} else
			warning("File has no manifest");
	}

	public void doImpExp(Jar jar) throws Exception {
		Manifest m = jar.getManifest();

		if (m != null) {
			Domain domain = Domain.domain(m);
			Parameters imports = domain.getImportPackage();
			Parameters exports = domain.getExportPackage();
			for (String p : exports.keySet()) {
				if (imports.containsKey(p)) {
					Attrs attrs = imports.get(p);
					if (attrs.containsKey(VERSION_ATTRIBUTE)) {
						exports.get(p)
							.put("imported-as", attrs.get(VERSION_ATTRIBUTE));
					}
				}
			}
			print(Constants.IMPORT_PACKAGE, new TreeMap<>(imports));
			println();
			print(Constants.EXPORT_PACKAGE, new TreeMap<>(exports));
		} else
			warning("File has no manifest");
	}

	public void doVerify(Jar jar) throws Exception, IOException {
		try (Verifier verifier = new Verifier(jar)) {
			verifier.setPedantic(isPedantic());
			verifier.verify();
			getInfo(verifier);
		}
	}

	public void doManifest(Jar jar) throws Exception {
		Manifest manifest = jar.getManifest();
		if (manifest == null)
			warning("JAR has no manifest %s", jar);
		else {
			doManifest(manifest);
		}
		println();
	}

	private JarPrinter println(String string) {
		out.format("%s\n", string);
		return this;
	}

	public JarPrinter println() {
		out.format("%n");
		return this;
	}

	/**
	 * @param manifest
	 */
	public JarPrinter doManifest(Manifest manifest) {
		MultiMap<String, String> table = new MultiMap<>();
		MapStream.of(manifest.getMainAttributes())
			.forEach((k, v) -> {
				String key = k.toString();
				if (syntax_headers.contains(key)) {
					table.put(key, Strings.splitQuoted(v.toString()));
				} else {
					table.add(key, v.toString());
				}
			});
		println(MultiMap.format(table));
		return this;
	}

	/**
	 * Print the components in this JAR.
	 *
	 * @param jar
	 */
	private void doComponents(Jar jar) throws Exception {
		Manifest manifest = jar.getManifest();
		if (manifest == null) {
			println("No manifest");
			return;
		}

		String componentHeader = manifest.getMainAttributes()
			.getValue(Constants.SERVICE_COMPONENT);
		Parameters clauses = new Parameters(componentHeader, this);
		for (String path : clauses.keySet()) {
			println(path);

			Resource r = jar.getResource(path);
			if (r != null) {
				try (InputStreamReader ir = new InputStreamReader(r.openInputStream(), Constants.DEFAULT_CHARSET)) {
					IO.copy(ir, getWriter());
				}
			} else {
				println("  - no resource");
				warning("No Resource found for service component: %s", path);
			}
		}
		println();
	}

	public Writer getWriter() {
		return IO.appendableToWriter(out.out());
	}

	/**
	 * Print the metatypes in this JAR.
	 *
	 * @param jar
	 */
	public void doMetatype(Jar jar) throws Exception {
		Manifest manifest = jar.getManifest();
		if (manifest == null) {
			println("No manifest");
			return;
		}

		Map<String, Resource> map = jar.getDirectories()
			.get("OSGI-INF/metatype");
		if (map != null) {
			for (Map.Entry<String, Resource> entry : map.entrySet()) {
				println(entry.getKey());
				IO.copy(entry.getValue()
					.openInputStream(), getWriter());
				println();
			}
			println();
		}
	}

	/**
	 * @param msg
	 * @param ports
	 */

	public void print(String msg, Map<?, ? extends Map<?, ?>> ports) {
		if (ports.isEmpty())
			return;
		out.format("%s%n", msg);
		for (Entry<?, ? extends Map<?, ?>> entry : ports.entrySet()) {
			Object key = entry.getKey();
			Map<?, ?> clause = Create.copy((Map<?, ?>) entry.getValue());
			clause.remove("uses:");
			out.format("  %-38s %s\n", Processor.removeDuplicateMarker(key.toString()
				.trim()), clause.isEmpty() ? "" : clause.toString());
		}
	}

	@Override
	public String toString() {
		return out.toString();
	}
}
