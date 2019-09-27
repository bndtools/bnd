package bndtools.jareditor.internal;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.Verifier;
import aQute.lib.collections.SortedList;
import aQute.lib.io.IO;
import aQute.libg.generics.Create;
import bndtools.jareditor.internal.utils.CollectionUtil;

public class Printer extends Processor {

	final static int			MANIFEST	= 2;
	final static int			LIST		= 4;

	final static int			IMPEXP		= 16;
	final static int			USES		= 32;
	final static int			USEDBY		= 64;
	final static int			COMPONENT	= 128;
	final static int			METATYPE	= 256;
	final static int			VERIFY		= 1;

	PrintStream					out			= System.out;
	OutputStreamWriter			or			= new OutputStreamWriter(out, Constants.DEFAULT_CHARSET);

	private static final String	EOL			= String.format("%n");

	public void setOut(PrintStream out) {
		this.out = out;
		this.or = new OutputStreamWriter(out, Constants.DEFAULT_CHARSET);
	}

	public void doPrint(String string, int options) throws Exception {
		int optionsi = options;
		File file = new File(string);
		if (!file.exists())
			error("File to print not found: " + string);
		else {
			if (optionsi == 0)
				optionsi = VERIFY | MANIFEST | IMPEXP | USES;
			doPrint(file, optionsi);
		}
	}

	private void doPrint(File file, int options) throws ZipException, IOException, Exception {
		try (Jar jar = new Jar(file.getName(), file)) {
			if ((options & VERIFY) != 0) {
				try (Verifier verifier = new Verifier(jar)) {
					verifier.setPedantic(isPedantic());
					verifier.verify();
					getInfo(verifier);
				}
			}
			if ((options & MANIFEST) != 0) {
				Manifest manifest = jar.getManifest();
				if (manifest == null)
					warning("JAR has no manifest " + file);
				else {
					out.println("[MANIFEST " + jar.getName() + "]");
					printManifest(manifest);
				}
				out.println();
			}
			if ((options & IMPEXP) != 0) {
				out.println("[IMPEXP]");
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
					print("Import-Package", new TreeMap<>(imports));
					print("Export-Package", new TreeMap<>(exports));
				} else
					warning("File has no manifest");
			}

			if ((options & (USES | USEDBY)) != 0) {
				out.println();
				try (Analyzer analyzer = new Analyzer()) {
					analyzer.setPedantic(isPedantic());
					analyzer.setJar(jar);
					analyzer.analyze();
					if ((options & USES) != 0) {
						out.println("[USES]");
						printMultiMap(analyzer.getUses());
						out.println();
					}
					if ((options & USEDBY) != 0) {
						out.println("[USEDBY]");
						Map<PackageRef, Set<PackageRef>> usedBy = CollectionUtil
							.invertMapOfCollection(analyzer.getUses());
						printMultiMap(usedBy);
					}
					analyzer.setJar((Jar) null);
				}
				out.println();
			}

			if ((options & COMPONENT) != 0) {
				printComponents(jar);
				out.println();
			}

			if ((options & METATYPE) != 0) {
				printMetatype(jar);
				out.println();
			}

			if ((options & LIST) != 0) {
				out.println("[LIST]");
				for (Map.Entry<String, Map<String, Resource>> entry : jar.getDirectories()
					.entrySet()) {
					String name = entry.getKey();
					Map<String, Resource> contents = entry.getValue();
					out.println(name);
					if (contents != null) {
						for (String element : contents.keySet()) {
							int n = element.lastIndexOf('/');
							if (n > 0)
								element = element.substring(n + 1);
							out.print("  ");
							out.print(element);
							String path = element;
							if (name.length() != 0)
								path = name + "/" + element;
							Resource r = contents.get(path);
							if (r != null) {
								String extra = r.getExtra();
								if (extra != null) {
									out.print(" extra='" + escapeUnicode(extra) + "'");
								}
							}
							out.println();
						}
					}
				}
				out.println();
			}
		}
	}

	/**
	 * @param manifest
	 */
	private void printManifest(Manifest manifest) {
		SortedSet<String> sorted = new TreeSet<>();
		for (Object element : manifest.getMainAttributes()
			.keySet()) {
			sorted.add(element.toString());
		}
		for (String key : sorted) {
			Object value = manifest.getMainAttributes()
				.getValue(key);
			format(out, "%-40s %-40s%s", new Object[] {
				key, value, EOL
			});
		}
	}

	private void print(String msg, Map<?, ? extends Map<?, ?>> ports) {
		if (ports.isEmpty())
			return;
		out.println(msg);
		for (Entry<?, ? extends Map<?, ?>> entry : ports.entrySet()) {
			Object key = entry.getKey();
			Map<?, ?> clause = Create.copy((Map<?, ?>) entry.getValue());
			clause.remove("uses:");
			format(out, "  %-38s %s%s", key.toString()
				.trim(), clause.isEmpty() ? "" : clause.toString(), EOL);
		}
	}

	private <T extends Comparable<? super T>> void printMultiMap(Map<T, ? extends Collection<T>> map) {
		SortedList<T> keys = new SortedList<>(map.keySet());
		for (Object key : keys) {
			String name = key.toString();

			SortedList<T> values = new SortedList<>(map.get(key));
			String list = vertical(41, values);
			format(out, "%-40s %s", name, list);
		}
	}

	private static String vertical(int padding, Collection<?> used) {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (Object s : used) {
			String name = s.toString();
			sb.append(del);
			sb.append(name);
			sb.append(EOL);
			del = pad(padding);
		}
		if (sb.length() == 0)
			sb.append(EOL);
		return sb.toString();
	}

	private static String pad(int i) {
		StringBuilder sb = new StringBuilder();
		int ii = i;
		while (ii-- > 0)
			sb.append(' ');
		return sb.toString();
	}

	private static void format(PrintStream out, String string, Object... objects) {
		if (objects == null || objects.length == 0)
			return;

		StringBuffer sb = new StringBuffer();
		int index = 0;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			switch (c) {
				case '%' :
					String s = objects[index++] + "";
					int width = 0;
					int justify = -1;

					i++;

					c = string.charAt(i++);
					switch (c) {
						case '-' :
							justify = -1;
							break;
						case '+' :
							justify = 1;
							break;
						case '|' :
							justify = 0;
							break;
						default :
							--i;
					}
					c = string.charAt(i++);
					while (c >= '0' && c <= '9') {
						width *= 10;
						width += c - '0';
						c = string.charAt(i++);
					}
					--i;
					if (c != 's') {
						throw new IllegalArgumentException("Invalid sprintf format:  " + string);
					}

					if (s.length() > width)
						sb.append(s);
					else {
						switch (justify) {
							case -1 :
							default :
								sb.append(s);
								for (int j = 0; j < width - s.length(); j++)
									sb.append(" ");
								break;

							case 1 :
								for (int j = 0; j < width - s.length(); j++)
									sb.append(" ");
								sb.append(s);
								break;

							case 0 :
								int spaces = (width - s.length()) / 2;
								for (int j = 0; j < spaces; j++)
									sb.append(" ");
								sb.append(s);
								for (int j = 0; j < width - s.length() - spaces; j++)
									sb.append(" ");
								break;
						}
					}
					break;

				default :
					sb.append(c);
			}
		}
		out.print(sb);
	}

	private static final String escapeUnicode(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= ' ' && c <= '~' && c != '\\')
				sb.append(c);
			else {
				sb.append("\\u");
				sb.append(nibble(c >> 12));
				sb.append(nibble(c >> 8));
				sb.append(nibble(c >> 4));
				sb.append(nibble(c));
			}
		}
		return sb.toString();
	}

	private static final char nibble(int i) {
		return "0123456789ABCDEF".charAt(i & 0xF);
	}

	/**
	 * Print the components in this JAR.
	 *
	 * @param jar
	 */
	private void printComponents(Jar jar) throws Exception {
		out.println("[COMPONENTS]");
		Manifest manifest = jar.getManifest();
		if (manifest == null) {
			out.println("No manifest");
			return;
		}

		String componentHeader = manifest.getMainAttributes()
			.getValue(Constants.SERVICE_COMPONENT);
		Parameters clauses = new Parameters(componentHeader);
		boolean printed = false;
		for (String path : clauses.keySet()) {
			printed = true;
			out.println(path);

			Resource r = jar.getResource(path);
			if (r != null) {
				IO.copy(IO.reader(r.openInputStream(), Constants.DEFAULT_CHARSET), or);
			} else {
				out.println("  - no resource");
				warning("No Resource found for service component: " + path);
			}
		}
		if (printed) {
			out.println();
		}
	}

	/**
	 * Print the metatypes in this JAR.
	 *
	 * @param jar
	 */
	private void printMetatype(Jar jar) throws Exception {
		out.println("[METATYPE]");
		Manifest manifest = jar.getManifest();
		if (manifest == null) {
			out.println("No manifest");
			return;
		}

		Map<String, Resource> map = jar.getDirectories()
			.get("OSGI-INF/metatype");
		if (map != null) {
			for (Map.Entry<String, Resource> entry : map.entrySet()) {
				out.println(entry.getKey());
				IO.copy(IO.reader(entry.getValue()
					.openInputStream(), Constants.DEFAULT_CHARSET), or);
				out.println();
			}
			out.println();
		}
	}

}
